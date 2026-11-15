package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.client.command.*;
import org.fogofwarlabs.sc4j.engine.client.game.GameSession;
import org.fogofwarlabs.sc4j.engine.client.input.KeyBindingTable;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.SkirmishScenario;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommandType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine default command card: a producer with queue entries shows the cancel button after its training buttons and it cancels the
 * last entry, a producer with an empty queue does not show it, and a building under construction shows only the cancel button, which cancels
 * its construction. A type whose requirement misses a unit shows refused with that unit named, and a type the player cannot pay for or whose
 * queue is full is refused with the reason; the translator runs no refused action and reports the refusal. A producer shows the rally button,
 * whose command sets the rally point the unit view reads; a worker shows none.
 */
final class StandardCommandCardSourceTest {
    private static final int ORDER_ID = 0;
    private static final int TRAIN_ID = 1;
    private static final int CONSTRUCT_ID = 2;
    private static final int CANCEL_ID = 3;
    private static final int RALLY_ID = 4;
    private static final int RALLY_X = 520;
    private static final int RALLY_Y = 410;
    private static final @NotNull String RALLY_LABEL = "Rally";
    private static final int CARD_CELLS = 9;
    private static final int KEEP_X = 300;
    private static final int KEEP_Y = 300;
    private static final int GOLD = 2000;
    private static final int SELECTION_CAPACITY = 4;
    private static final int SITE_TRIES = 16;
    private static final int SITE_STEP = SkirmishScenario.TILE_SIZE;
    private static final @NotNull String CANCEL_LABEL = "Cancel";
    private static final @NotNull String BARRACKS_LABEL = "barracks";
    private static final @NotNull String KEEPER_LABEL = "keeper";
    private static final @NotNull String SUPPLY_NAME = "supply";
    private static final int QUEUE_CAPACITY = 5;

    private static @NotNull List<@NotNull String> getLabels(@NotNull List<@NotNull CommandButton> buttons) {
        return buttons.stream().map(CommandButton::getLabel).toList();
    }

    private static @NotNull CommandButton findButton(@NotNull List<@NotNull CommandButton> buttons, @NotNull String label) {
        return buttons.stream().filter(button -> button.getLabel().equals(label)).findFirst().orElseThrow();
    }

    private static @NotNull String describe(@NotNull CommandRefusal refusal) {
        return refusal.describe(ContentType::getDebugName, ContentType::getDebugName, SUPPLY_NAME);
    }

    /**
     * Places a keep under construction on the first free site on a diagonal away from the worker.
     */
    private static @NotNull Unit placeBuilding(@NotNull StandardProductionSystem production, @NotNull Unit worker,
                                               @NotNull SkirmishScenario content) {
        for (int step = 1; step <= SITE_TRIES; step++) {
            int offset = step * SITE_STEP;
            Unit building = production.placeBuilding(worker, content.getKeep(), KEEP_X + offset, KEEP_Y + offset, 0);

            if (building != null) {
                return building;
            }
        }

        return Assertions.fail("No free site for the keep.");
    }

    @Test
    void refused_buttons_name_the_missing_unit_resource_or_full_queue_and_the_translator_does_not_run_them() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        SkirmishScenario content = skirmish.getScenario();
        GameSession session = skirmish.getSession();
        Game game = session.getGame();
        Player player = session.getLocalPlayer();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();

        Assertions.assertNotNull(player);
        Assertions.assertNotNull(session.getCommandSink());

        StandardCommandCardSource source = new StandardCommandCardSource(game, new IssueOrderCommandType(ORDER_ID, 0),
                                                                         new TrainUnitCommandType(TRAIN_ID),
                                                                         new ConstructBuildingCommandType(CONSTRUCT_ID, 0),
                                                                         new CancelProductionCommandType(CANCEL_ID, 0), null, List.of(),
                                                                         ContentType::getDebugName, CARD_CELLS);
        OrderedSelection selection = new OrderedSelection(SELECTION_CAPACITY);
        CommandTranslator translator = new CommandTranslator(player, session.getCommandSink(), selection, new KeyBindingTable<>(), (x, y) -> null);
        List<CommandRefusal> refusals = new ArrayList<>();
        Unit worker = production.createUnit(player, content.getKeeper(), KEEP_X, KEEP_Y, 0);

        Assertions.assertNotNull(worker);

        translator.setRefusalListener(refusals::add);
        game.getEconomy().setAmount(player, content.getGold(), GOLD);
        selection.select(worker);

        CommandButton barracks = findButton(source.createButtons(player, List.of(worker)), BARRACKS_LABEL);
        CommandRefusal missingKeep = barracks.getAction().findRefusal(translator);

        Assertions.assertNotNull(missingKeep, "the barracks needs a completed keep");
        Assertions.assertSame(CommandRefusal.Reason.MISSING_REQUIREMENT, missingKeep.getReason());
        Assertions.assertSame(content.getKeep(), missingKeep.getUnitType());
        Assertions.assertEquals("Needs keep", describe(missingKeep));
        Assertions.assertTrue(translator.run(barracks.getAction(), false));
        Assertions.assertNull(translator.getPendingAction(), "a refused construction does not wait for its site");
        Assertions.assertEquals(List.of(missingKeep), refusals, "the translator reports the refusal");

        Unit keep = production.createUnit(player, content.getKeep(), KEEP_X, KEEP_Y, 0);

        Assertions.assertNotNull(keep);
        Assertions.assertNull(barracks.getAction().findRefusal(translator), "a completed keep meets the requirement");

        game.getEconomy().setAmount(player, content.getGold(), 0);

        CommandRefusal noGold = barracks.getAction().findRefusal(translator);

        Assertions.assertNotNull(noGold);
        Assertions.assertSame(CommandRefusal.Reason.MISSING_RESOURCE, noGold.getReason());
        Assertions.assertSame(content.getGold(), noGold.getResourceKind());
        Assertions.assertEquals("Not enough gold", describe(noGold));
        Assertions.assertTrue(barracks.getAction().getCost().getAmount(content.getGold()) > 0, "the button states the cost");

        game.getEconomy().setAmount(player, content.getGold(), GOLD);
        selection.select(keep);

        CommandButton keeper = findButton(source.createButtons(player, List.of(keep)), KEEPER_LABEL);

        for (int entry = 0; entry < QUEUE_CAPACITY; entry++) {
            Assertions.assertNull(keeper.getAction().findRefusal(translator), "entry " + entry + " fits the queue");
            production.enqueueUnit(keep, content.getKeeper());
        }

        Assertions.assertSame(CommandRefusal.QUEUE_FULL, keeper.getAction().findRefusal(translator));
        Assertions.assertEquals("Queue is full", describe(CommandRefusal.QUEUE_FULL));
        Assertions.assertEquals(1, keeper.getAction().getSupplyCost(), "the button states the supply");
    }

    @Test
    void the_cancel_button_shows_for_something_to_cancel_and_cancels_the_last_entry_or_the_construction() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        SkirmishScenario content = skirmish.getScenario();
        GameSession session = skirmish.getSession();
        Game game = session.getGame();
        Player player = session.getLocalPlayer();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();

        Assertions.assertNotNull(player);
        Assertions.assertNotNull(session.getCommandSink());

        StandardCommandCardSource source = new StandardCommandCardSource(game, new IssueOrderCommandType(ORDER_ID, 0),
                                                                         new TrainUnitCommandType(TRAIN_ID),
                                                                         new ConstructBuildingCommandType(CONSTRUCT_ID, 0),
                                                                         new CancelProductionCommandType(CANCEL_ID, 0), null, List.of(),
                                                                         ContentType::getDebugName, CARD_CELLS);
        OrderedSelection selection = new OrderedSelection(SELECTION_CAPACITY);
        CommandTranslator translator = new CommandTranslator(player, session.getCommandSink(), selection, new KeyBindingTable<>(), (x, y) -> null);
        Unit keep = production.createUnit(player, content.getKeep(), KEEP_X, KEEP_Y, 0);

        Assertions.assertNotNull(keep);

        game.getEconomy().setAmount(player, content.getGold(), GOLD);
        selection.select(keep);

        List<CommandButton> idle = source.createButtons(player, List.of(keep));

        Assertions.assertFalse(getLabels(idle).contains(CANCEL_LABEL), "an empty queue has nothing to cancel");
        Assertions.assertInstanceOf(TrainAction.class, idle.getFirst().getAction());

        production.enqueueUnit(keep, content.getKeeper());
        production.enqueueUnit(keep, content.getKeeper());

        List<CommandButton> training = source.createButtons(player, List.of(keep));

        Assertions.assertEquals(CANCEL_LABEL, training.getLast().getLabel());
        Assertions.assertNull(training.getLast().getHotkey());
        Assertions.assertSame(source.getCancelAction(), training.getLast().getAction());

        translator.run(source.getCancelAction(), false);
        session.runFrame();

        Assertions.assertEquals(1, production.getQueueLength(keep), "the last entry is cancelled");

        Unit worker = production.createUnit(player, content.getKeeper(), KEEP_X, KEEP_Y, 0);

        Assertions.assertNotNull(worker);

        Unit building = placeBuilding(production, worker, content);

        Assertions.assertEquals(List.of(CANCEL_LABEL), getLabels(source.createButtons(player, List.of(building))));
        Assertions.assertInstanceOf(CancelAction.class, source.createButtons(player, List.of(building)).getFirst().getAction());

        selection.select(building);
        translator.run(source.getCancelAction(), false);
        session.runFrame();

        Assertions.assertFalse(game.getUnits().isAllocated(building.getIndex()), "the construction is cancelled");
    }

    @Test
    void the_rally_button_shows_for_a_producer_and_sets_its_rally_point_that_the_view_reads() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        SkirmishScenario content = skirmish.getScenario();
        GameSession session = skirmish.getSession();
        Game game = session.getGame();
        Player player = session.getLocalPlayer();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();

        Assertions.assertNotNull(player);
        Assertions.assertNotNull(session.getCommandSink());

        StandardCommandCardSource source = new StandardCommandCardSource(game, new IssueOrderCommandType(ORDER_ID, 0),
                                                                         new TrainUnitCommandType(TRAIN_ID),
                                                                         new ConstructBuildingCommandType(CONSTRUCT_ID, 0),
                                                                         new CancelProductionCommandType(CANCEL_ID, 0),
                                                                         new SetRallyPointCommandType(RALLY_ID), List.of(),
                                                                         ContentType::getDebugName, CARD_CELLS);
        OrderedSelection selection = new OrderedSelection(SELECTION_CAPACITY);
        CommandTranslator translator = new CommandTranslator(player, session.getCommandSink(), selection, new KeyBindingTable<>(), (x, y) -> null);
        Unit keep = production.createUnit(player, content.getKeep(), KEEP_X, KEEP_Y, 0);
        Unit worker = production.createUnit(player, content.getKeeper(), KEEP_X, KEEP_Y, 0);
        RallyAction rally = source.getRallyAction();

        Assertions.assertNotNull(keep);
        Assertions.assertNotNull(worker);
        Assertions.assertNotNull(rally);
        Assertions.assertTrue(getLabels(source.createButtons(player, List.of(keep))).contains(RALLY_LABEL), "a producer shows the rally button");
        Assertions.assertFalse(getLabels(source.createButtons(player, List.of(worker))).contains(RALLY_LABEL), "a worker shows none");
        Assertions.assertFalse(session.getView().getUnitView(keep).hasRallyPoint());

        selection.select(keep);

        Assertions.assertTrue(rally.issue(translator, CommandTarget.at(RALLY_X, RALLY_Y), false));

        session.runFrame();

        Assertions.assertTrue(session.getView().getUnitView(keep).hasRallyPoint(), "the command sets the rally point");
        Assertions.assertEquals(RALLY_X, session.getView().getUnitView(keep).getRallyPointX());
        Assertions.assertEquals(RALLY_Y, session.getView().getUnitView(keep).getRallyPointY());

        selection.select(worker);

        Assertions.assertFalse(rally.issue(translator, CommandTarget.at(RALLY_X, RALLY_Y), false), "a worker takes no rally point");
    }
}

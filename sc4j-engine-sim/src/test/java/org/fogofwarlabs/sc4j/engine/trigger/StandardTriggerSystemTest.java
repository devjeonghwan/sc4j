package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.engine.combat.CombatSystem;
import org.fogofwarlabs.sc4j.engine.combat.StandardCombatSystem;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine default trigger system in an engine-only game: annihilation defeats a player without units and makes the last player win,
 * elimination counts buildings only, timers, waits and messages run in order, repeating triggers run again, units are created, given away
 * and removed in an area, and the mod's own conditions and actions run under their type ids.
 */
final class StandardTriggerSystemTest {
    private static final int UNIT_LIMIT = 16;
    private static final int MOD_CONDITION = StandardTriggerConditionUtils.TYPE_COUNT;
    private static final int MOD_ACTION = StandardTriggerActionUtils.TYPE_COUNT;
    private static final int HOLDING_AMOUNT = 2;

    private final @NotNull UnitType tower;
    private final @NotNull UnitType soldier;
    private final @NotNull List<@NotNull String> events;

    StandardTriggerSystemTest() {
        this.tower   = new UnitType("tower");
        this.soldier = new UnitType("soldier");
        this.events  = new ArrayList<>();
    }

    @Test
    void annihilated_players_lose_and_the_last_player_wins() {
        Game game = this.createGame(StandardTriggerUtils.createAnnihilation());
        StandardTriggerSystem triggers = (StandardTriggerSystem) game.getTriggers();
        Unit soldier = this.createUnit(game, 1, this.soldier, 50, 50);

        this.createUnit(game, 0, this.tower, 10, 10);
        this.runFrames(game, 3);

        Assertions.assertEquals(PlayerOutcome.PLAYING, triggers.getOutcome(game.getPlayer(0)));
        Assertions.assertFalse(triggers.isGameOver());
        Assertions.assertEquals(4, triggers.getRunningTriggers().size());

        game.getCombat().dealDamage(soldier, Integer.MAX_VALUE, null, 0);
        this.runFrames(game, 2);

        Assertions.assertEquals(PlayerOutcome.DEFEAT, triggers.getOutcome(game.getPlayer(1)));
        Assertions.assertEquals(PlayerOutcome.VICTORY, triggers.getOutcome(game.getPlayer(0)));
        Assertions.assertEquals(PlayerOutcome.PLAYING, triggers.getOutcome(game.getPlayer(2)));
        Assertions.assertTrue(triggers.isGameOver());
        Assertions.assertEquals(List.of("outcome 1 DEFEAT", "outcome 0 VICTORY"), this.events);
    }

    @Test
    void eliminated_players_lose_without_buildings() {
        Game game = this.createGame(StandardTriggerUtils.createElimination());
        StandardTriggerSystem triggers = (StandardTriggerSystem) game.getTriggers();

        this.createUnit(game, 0, this.tower, 10, 10);
        this.createUnit(game, 1, this.soldier, 50, 50);
        this.runFrames(game, 1);

        Assertions.assertEquals(PlayerOutcome.DEFEAT, triggers.getOutcome(game.getPlayer(1)));
    }

    @Test
    void timers_waits_and_messages_run_in_order() {
        TriggerDefinition timed = new TriggerDefinition.Builder()
                .addPlayer(0)
                .addCondition(new ConditionRecord.Builder(StandardTriggerConditionUtils.ELAPSED_FRAMES).setAmount(2).build())
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.SET_TIMER).setTimer(1).setAmount(5).build())
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.DISPLAY_MESSAGE).setText("started").build())
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.WAIT).setAmount(3).build())
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.DISPLAY_MESSAGE).setText("waited").build())
                .build();
        TriggerDefinition onTimer = new TriggerDefinition.Builder()
                .addPlayer(0)
                .setRepeating(true)
                .addCondition(new ConditionRecord.Builder(StandardTriggerConditionUtils.ELAPSED_FRAMES).setAmount(3).build())
                .addCondition(new ConditionRecord.Builder(StandardTriggerConditionUtils.TIMER).setTimer(1).setComparison(Comparison.EXACTLY).build())
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.DISPLAY_MESSAGE).setText("timer").build())
                .build();
        Game game = this.createGame(List.of(timed, onTimer));
        StandardTriggerSystem triggers = (StandardTriggerSystem) game.getTriggers();

        this.runFrames(game, 2);

        Assertions.assertEquals(List.of("message 0 started"), this.events);
        Assertions.assertEquals(5, triggers.getTimer(1));

        this.runFrames(game, 3);

        Assertions.assertEquals(List.of("message 0 started", "message 0 waited"), this.events);

        this.runFrames(game, 4);

        Assertions.assertEquals(List.of("message 0 started", "message 0 waited", "message 0 timer", "message 0 timer", "message 0 timer"),
                                this.events);
        Assertions.assertTrue(triggers.getRunningTriggers().get(0).isDone());
        Assertions.assertFalse(triggers.getRunningTriggers().get(1).isDone());
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> triggers.getTimer(StandardTriggerSystem.TIMER_COUNT));
    }

    @Test
    void repeat_trigger_action_makes_a_trigger_fire_again() {
        TriggerDefinition once = new TriggerDefinition.Builder()
                .addPlayer(0)
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.DISPLAY_MESSAGE).setText("once").build())
                .build();
        TriggerDefinition repeated = new TriggerDefinition.Builder()
                .addPlayer(1)
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.REPEAT_TRIGGER).build())
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.DISPLAY_MESSAGE).setText("again").build())
                .build();
        Game game = this.createGame(List.of(once, repeated));
        StandardTriggerSystem triggers = (StandardTriggerSystem) game.getTriggers();

        this.runFrames(game, 3);

        Assertions.assertEquals(List.of("message 0 once", "message 1 again", "message 1 again", "message 1 again"), this.events);
        Assertions.assertTrue(triggers.getRunningTriggers().get(0).isDone());
        Assertions.assertTrue(triggers.getRunningTriggers().get(1).isRepeating());
        Assertions.assertFalse(triggers.getRunningTriggers().get(1).isDone());
    }

    @Test
    void unit_actions_create_give_and_remove_units_in_an_area() {
        TriggerArea area = new TriggerArea("camp", 0, 0, 99, 99);
        TriggerDefinition create = new TriggerDefinition.Builder()
                .addPlayer(0)
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.CREATE_UNITS).setUnitType(this.soldier)
                                                                                            .setArea(area)
                                                                                            .setAmount(3)
                                                                                            .build())
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.GIVE_UNITS).setUnitType(this.soldier)
                                                                                          .setArea(area)
                                                                                          .setAmount(1)
                                                                                          .setTargetPlayer(PlayerSelector.ofSlot(1))
                                                                                          .build())
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.REMOVE_UNITS).setUnitType(this.soldier).setArea(area).build())
                .build();
        TriggerDefinition count = new TriggerDefinition.Builder()
                .addPlayer(1)
                .addCondition(new ConditionRecord.Builder(StandardTriggerConditionUtils.UNIT_COUNT).setUnitType(this.soldier)
                                                                                                   .setArea(area)
                                                                                                   .setComparison(Comparison.EXACTLY)
                                                                                                   .setAmount(1)
                                                                                                   .build())
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.DISPLAY_MESSAGE).setText("given").build())
                .build();
        Game game = this.createGame(List.of(create, count));
        Player first = game.getPlayer(0);
        Player second = game.getPlayer(1);

        this.runFrames(game, 1);

        Assertions.assertEquals(0, game.getContent().getProgress().getUnitCount(first, this.soldier));
        Assertions.assertEquals(1, game.getContent().getProgress().getUnitCount(second, this.soldier));
        Assertions.assertEquals(List.of("message 1 given"), this.events);
        Assertions.assertTrue(area.contains(50, 50));
        Assertions.assertFalse(area.contains(100, 50));
    }

    @Test
    void mod_conditions_and_actions_run_under_their_type_ids() {
        TriggerDefinition holding = new TriggerDefinition.Builder()
                .addPlayer(0)
                .addCondition(new ConditionRecord.Builder(MOD_CONDITION).setAmount(HOLDING_AMOUNT).build())
                .addAction(new ActionRecord.Builder(MOD_ACTION).setText("held").build())
                .build();
        TriggerDefinition failing = new TriggerDefinition.Builder()
                .addPlayer(1)
                .addCondition(new ConditionRecord.Builder(MOD_CONDITION).setAmount(HOLDING_AMOUNT + 1).build())
                .addAction(new ActionRecord.Builder(MOD_ACTION).setText("failed").build())
                .build();
        StandardTriggerSystem.Builder triggers = new StandardTriggerSystem.Builder()
                .addTriggers(List.of(holding, failing))
                .addCondition(MOD_CONDITION, (context, condition) -> condition.getAmount() == HOLDING_AMOUNT)
                .addAction(MOD_ACTION, (context, trigger, action) -> this.events.add("mod " + trigger.getPlayer().getIndex() + " " + action.getText()));
        Game game = this.createGame(triggers);

        this.runFrames(game, 2);

        Assertions.assertEquals(List.of("mod 0 held"), this.events);
    }

    private void runFrames(@NotNull Game game, int count) {
        for (int frame = 0; frame < count; frame++) {
            game.getFrameRunner().runFrame();
        }
    }

    private @NotNull Unit createUnit(@NotNull Game game, int player, @NotNull UnitType type, int x, int y) {
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(player), type, x, y, 0);

        Assertions.assertNotNull(unit);

        return unit;
    }

    private @NotNull Game createGame(@NotNull List<@NotNull TriggerDefinition> definitions) {
        return this.createGame(new StandardTriggerSystem.Builder().addTriggers(definitions));
    }

    private @NotNull Game createGame(StandardTriggerSystem.@NotNull Builder triggers) {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).build())
                .useSystem(CombatSystem.KEY, context -> new StandardCombatSystem())
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY).build())
                .useSystem(TriggerSystem.KEY, context -> triggers.build())
                .addHook(StandardTriggerSystem.MESSAGE, context -> (player, text) -> this.events.add("message " + player.getIndex() + " " + text))
                .addHook(StandardTriggerSystem.OUTCOME, context -> (player, outcome) -> this.events.add("outcome " + player.getIndex() + " " + outcome))
                .addFramePhase(TriggerSystem.KEY);

        StandardCombatSystem.addColumns(builder);
        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        builder.getContent().getUnitTypes().register(this.tower, new UnitDefinition.Builder().setHitPoints(100).addTrait(UnitTrait.BUILDING).build());
        builder.getContent().getUnitTypes().register(this.soldier, new UnitDefinition.Builder().setHitPoints(10).build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0]))
                .addPlayer(new PlayerSlot(0, ControllerKind.COMPUTER, 0, 0, null))
                .addPlayer(new PlayerSlot(1, ControllerKind.COMPUTER, 1, 1, null))
                .addPlayer(new PlayerSlot(2, ControllerKind.NEUTRAL, 2, 2, null))
                .build();

        return Game.create(builder.build(), setup);
    }
}

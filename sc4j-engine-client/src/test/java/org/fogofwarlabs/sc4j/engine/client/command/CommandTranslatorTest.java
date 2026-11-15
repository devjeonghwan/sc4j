package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.client.input.*;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tests the input to command translation: bound keys run actions, targeted actions wait for a left press on the map, right presses cancel or
 * issue the default action, Shift queues, nothing runs without a selection, and a shared targeting mode keeps an action that refuses its
 * target.
 */
final class CommandTranslatorTest {
    private static final int CAPACITY = 4;
    private static final int MAP_EDGE = 100;
    private static final float POINTER_X = 40;
    private static final float POINTER_Y = 30;
    private static final float HUD_Y = 150;

    /**
     * Records what each action issues, in order.
     */
    private static final class RecordingAction implements CommandAction {
        private final @NotNull String name;
        private final boolean targeted;
        private final @NotNull List<String> issued;

        private RecordingAction(@NotNull String name, boolean targeted, @NotNull List<String> issued) {
            this.name     = name;
            this.targeted = targeted;
            this.issued   = issued;
        }

        @Override
        public boolean isTargeted() {
            return this.targeted;
        }

        @Override
        public boolean issue(@NotNull CommandTranslator translator, @Nullable CommandTarget target, boolean queued) {
            this.issued.add(this.name + (target != null ? " " + target.getX() + "," + target.getY() : "") + (queued ? " queued" : "") + " x" +
                            translator.getSelection().getSize());

            return true;
        }
    }

    /**
     * Fails if the translator submits a command itself: the recording actions submit nothing.
     */
    private static final class FailingSink implements CommandSink {
        @Override
        public <C> void submit(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command) {
            Assertions.fail("no command expected");
        }
    }

    private static @NotNull InputEvent createKey(@NotNull Key key) {
        return InputEvent.createKey(InputEvent.Type.KEY_PRESSED, key, 0);
    }

    private static @NotNull InputEvent createPress(@NotNull PointerButton button, float y, int modifiers) {
        return InputEvent.createButtonPressed(button, modifiers, POINTER_X, y, false);
    }

    @Test
    void handle_input_should_run_bound_actions_and_issue_targeted_ones_at_the_pointed_target() {
        List<String> issued = new ArrayList<>();
        KeyBindingTable<CommandAction> bindings = new KeyBindingTable<>();
        OrderedSelection selection = new OrderedSelection(CAPACITY);
        CommandTranslator translator = new CommandTranslator(new Player(0), new FailingSink(), selection, bindings,
                                                             (x, y) -> y < MAP_EDGE ? CommandTarget.at((int) x, (int) y) : null);
        RecordingAction stop = new RecordingAction("stop", false, issued);
        RecordingAction attack = new RecordingAction("attack", true, issued);

        bindings.bind(Key.S, 0, stop, false);
        bindings.bind(Key.A, 0, attack, false);
        translator.setDefaultAction(new RecordingAction("move", true, issued));

        Assertions.assertFalse(translator.handleInput(createKey(Key.S)), "nothing selected");

        selection.select(new Unit(1));
        selection.add(new Unit(2));

        Assertions.assertTrue(translator.handleInput(createKey(Key.S)));
        Assertions.assertTrue(translator.handleInput(createKey(Key.A)));
        Assertions.assertSame(attack, translator.getPendingAction());
        Assertions.assertFalse(translator.handleInput(createPress(PointerButton.LEFT, HUD_Y, 0)), "the HUD is no target");
        Assertions.assertTrue(translator.handleInput(createPress(PointerButton.LEFT, POINTER_Y, KeyModifier.SHIFT.getMask())));
        Assertions.assertNull(translator.getPendingAction());
        Assertions.assertTrue(translator.handleInput(createPress(PointerButton.RIGHT, POINTER_Y, 0)));

        translator.handleInput(createKey(Key.A));

        Assertions.assertTrue(translator.handleInput(createPress(PointerButton.RIGHT, POINTER_Y, 0)), "a right press cancels");

        translator.handleInput(createKey(Key.A));

        Assertions.assertTrue(translator.handleInput(createKey(Key.ESCAPE)), "Escape cancels");
        Assertions.assertFalse(translator.handleInput(createKey(Key.ESCAPE)), "nothing left to cancel");
        Assertions.assertEquals(List.of("stop x2", "attack 40,30 queued x2", "move 40,30 x2"), issued);
    }

    @Test
    void shared_targeting_mode_keeps_an_action_that_refuses_its_target_pending() {
        List<String> issued = new ArrayList<>();
        OrderedSelection selection = new OrderedSelection(CAPACITY);
        TargetingMode targeting = new TargetingMode();
        Unit shown = new Unit(1);
        CommandTranslator translator = new CommandTranslator(new Player(0), new FailingSink(), selection, new KeyBindingTable<>(),
                                                             (x, y) -> CommandTarget.at((int) x, (int) y, x < MAP_EDGE ? shown : null),
                                                             targeting);
        CommandAction place = new CommandAction() {
            @Override
            public boolean isTargeted() {
                return true;
            }

            @Override
            public boolean issue(@NotNull CommandTranslator owner, @Nullable CommandTarget target, boolean queued) {
                Assertions.assertSame(this, owner.getPendingAction(), "the action is still pending while it is issued");
                Assertions.assertNotNull(target);

                issued.add(target.getX() + "," + target.getY() + (target.getUnit() != null ? " unit" : ""));

                return target.getUnit() == null;
            }
        };

        selection.select(new Unit(2));
        targeting.start(place);

        Assertions.assertSame(place, translator.getPendingAction(), "the interface started the action in the shared mode");
        Assertions.assertTrue(translator.handleInput(createPress(PointerButton.LEFT, POINTER_Y, 0)), "the refused press is taken");
        Assertions.assertSame(place, targeting.getAction(), "a refused target keeps the action waiting");

        selection.clear();

        Assertions.assertTrue(translator.handleInput(InputEvent.createButtonPressed(PointerButton.LEFT, 0, MAP_EDGE, POINTER_Y, false)),
                              "a pending action is issued after the selection emptied");
        Assertions.assertFalse(targeting.isActive());
        Assertions.assertFalse(targeting.cancel(), "nothing left to cancel");
        Assertions.assertEquals(List.of("40,30 unit", "100,30"), issued);
    }

    @Test
    void get_default_action_and_get_refusal_listener_should_return_the_set_values() {
        CommandTranslator translator = new CommandTranslator(new Player(0), new FailingSink(), new OrderedSelection(CAPACITY), new KeyBindingTable<>(),
                                                             (x, y) -> null);
        RecordingAction move = new RecordingAction("move", true, new ArrayList<>());
        Consumer<CommandRefusal> listener = refusal -> {
        };

        Assertions.assertNull(translator.getDefaultAction());
        Assertions.assertNull(translator.getRefusalListener());

        translator.setDefaultAction(move);
        translator.setRefusalListener(listener);

        Assertions.assertSame(move, translator.getDefaultAction());
        Assertions.assertSame(listener, translator.getRefusalListener());
    }
}

package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests an engine-only trigger system without a mod: its frame phase also runs in paused frames, the players' triggers run in the system's
 * order, conditions are tested in list order up to the first that fails, and an action that does not finish holds its trigger until a later
 * run resumes it.
 */
final class TriggerSystemTest {
    private static final int CONDITION_ALWAYS = 0;
    private static final int CONDITION_FRAME_AT_LEAST = 1;
    private static final int CONDITION_NEVER = 2;
    private static final int CONDITION_COUNT = 3;

    private static final int ACTION_LOG = 0;
    private static final int ACTION_WAIT = 1;
    private static final int ACTION_UNPAUSE = 2;
    private static final int ACTION_COUNT = 3;

    /**
     * One operand record: a type and an amount.
     */
    private static final class Operands {
        private final int type;
        private final int amount;

        private Operands(int type, int amount) {
            this.type   = type;
            this.amount = amount;
        }
    }

    /**
     * One trigger of a player with its action cursor and the runs its current wait has left.
     */
    private static final class Script {
        private final int owner;
        private final @NotNull Operands @NotNull [] conditions;
        private final @NotNull Operands @NotNull [] actions;

        private int cursor;
        private int waitLeft;

        private Script(int owner, @NotNull Operands @NotNull [] conditions, @NotNull Operands @NotNull [] actions) {
            this.owner      = owner;
            this.conditions = conditions;
            this.actions    = actions;

            this.cursor   = 0;
            this.waitLeft = -1;
        }
    }

    /**
     * The context of the test system: the game, the current player and the log the conditions and actions write.
     */
    private static final class ScriptContext implements TriggerContext {
        private final @NotNull List<@NotNull String> log;

        private @Nullable Game game;
        private @Nullable Player currentPlayer;

        private ScriptContext(@NotNull List<@NotNull String> log) {
            this.log = log;

            this.game          = null;
            this.currentPlayer = null;
        }

        @Override
        public @NotNull Game getGame() {
            Game current = this.game;

            if (current == null) {
                throw Exceptions.newIllegalStateException("The context is not attached.");
            }

            return current;
        }

        @Override
        public @Nullable Player getCurrentPlayer() {
            return this.currentPlayer;
        }
    }

    /**
     * Runs the scripts of every player in player order; a script whose cursor is at its first action tests its conditions first. An
     * unregistered condition fails and an unregistered action is skipped, as in {@link StandardTriggerSystem}.
     */
    private static final class ScriptSystem implements TriggerSystem {
        private final @NotNull ScriptContext context;
        private final @NotNull List<@NotNull Script> scripts;
        private final @NotNull TriggerRegistry<TriggerCondition<ScriptContext, Operands>> conditions;
        private final @NotNull TriggerRegistry<TriggerAction<ScriptContext, Script, Operands>> actions;

        private ScriptSystem(@NotNull List<@NotNull String> log, @NotNull List<@NotNull Script> scripts) {
            this.context    = new ScriptContext(log);
            this.scripts    = scripts;
            this.conditions = new TriggerRegistry<>(CONDITION_COUNT);
            this.actions    = new TriggerRegistry<>(ACTION_COUNT);

            this.conditions.register(CONDITION_ALWAYS, (context, condition) -> true);
            this.conditions.register(CONDITION_FRAME_AT_LEAST, (context, condition) -> {
                context.log.add("frame test " + condition.amount);

                return context.getGame().getClock().getFrame() >= condition.amount;
            });
            this.conditions.register(CONDITION_NEVER, (context, condition) -> {
                context.log.add("never");

                return false;
            });
            this.conditions.freeze();

            this.actions.register(ACTION_LOG, (context, trigger, action) -> {
                Player player = context.getCurrentPlayer();

                context.log.add("player " + (player != null ? player.getIndex() : -1) + " action " + action.amount + " frame " +
                                context.getGame().getClock().getFrame());

                return true;
            });
            this.actions.register(ACTION_WAIT, (context, trigger, action) -> {
                if (trigger.waitLeft < 0) {
                    trigger.waitLeft = action.amount;
                }

                if (trigger.waitLeft > 0) {
                    trigger.waitLeft--;

                    return false;
                }

                trigger.waitLeft = -1;

                return true;
            });
            this.actions.register(ACTION_UNPAUSE, (context, trigger, action) -> {
                context.getGame().getClock().setPaused(false);

                return true;
            });
            this.actions.freeze();
        }

        @Override
        public void attach(@NotNull Game game) {
            this.context.game = game;
        }

        @Override
        public void run(@NotNull Game game) {
            for (int playerIndex = 0; playerIndex < game.getPlayerCount(); playerIndex++) {
                this.context.currentPlayer = game.getPlayer(playerIndex);

                for (Script script : this.scripts) {
                    if (script.owner == playerIndex) {
                        this.runScript(script);
                    }
                }
            }

            this.context.currentPlayer = null;
        }

        private void runScript(@NotNull Script script) {
            if (script.cursor >= script.actions.length) {
                return;
            }

            if (script.cursor == 0) {
                for (Operands condition : script.conditions) {
                    TriggerCondition<ScriptContext, Operands> registeredCondition = this.conditions.find(condition.type);

                    if (registeredCondition == null || !registeredCondition.test(this.context, condition)) {
                        return;
                    }
                }
            }

            while (script.cursor < script.actions.length) {
                Operands action = script.actions[script.cursor];
                TriggerAction<ScriptContext, Script, Operands> registeredAction = this.actions.find(action.type);

                if (registeredAction != null && !registeredAction.execute(this.context, script, action)) {
                    return;
                }

                script.cursor++;
            }
        }
    }

    @Test
    void trigger_system_runs_in_paused_frames_and_resumes_a_waiting_action() {
        List<String> log = new ArrayList<>();
        Script script = new Script(1, new Operands[]{new Operands(CONDITION_ALWAYS, 0)},
                                   new Operands[]{new Operands(ACTION_LOG, 1), new Operands(ACTION_WAIT, 2), new Operands(ACTION_UNPAUSE, 0),
                                                  new Operands(ACTION_LOG, 2)});
        Game game = createGame(log, List.of(script));

        game.getClock().setPaused(true);

        for (int frame = 0; frame < 4; frame++) {
            game.getFrameRunner().runFrame();
        }

        Assertions.assertEquals(List.of("player 1 action 1 frame 0", "player 1 action 2 frame 0"), log);
        Assertions.assertFalse(game.getClock().isPaused());
        Assertions.assertEquals(1, game.getClock().getFrame());
    }

    @Test
    void trigger_system_tests_conditions_in_list_order_up_to_the_first_that_fails() {
        List<String> log = new ArrayList<>();
        Script blocked = new Script(0, new Operands[]{new Operands(CONDITION_NEVER, 0), new Operands(CONDITION_FRAME_AT_LEAST, 0)},
                                    new Operands[]{new Operands(ACTION_LOG, 9)});
        Script later = new Script(0, new Operands[]{new Operands(CONDITION_FRAME_AT_LEAST, 2)}, new Operands[]{new Operands(ACTION_LOG, 3)});
        Script second = new Script(1, new Operands[]{new Operands(CONDITION_ALWAYS, 0)}, new Operands[]{new Operands(ACTION_LOG, 4)});
        Game game = createGame(log, List.of(second, blocked, later));

        for (int frame = 0; frame < 2; frame++) {
            game.getFrameRunner().runFrame();
        }

        Assertions.assertEquals(List.of("never", "frame test 2", "player 1 action 4 frame 1",
                                        "never", "frame test 2", "player 0 action 3 frame 2"), log);
        Assertions.assertSame(game.getModSystem(TriggerSystem.KEY), game.getTriggers());
    }

    @Test
    void trigger_system_fails_an_unknown_condition_and_skips_an_unknown_action() {
        List<String> log = new ArrayList<>();
        Script unknownCondition = new Script(0, new Operands[]{new Operands(CONDITION_COUNT, 0), new Operands(CONDITION_ALWAYS, 0)},
                                             new Operands[]{new Operands(ACTION_LOG, 5)});
        Script unknownAction = new Script(0, new Operands[]{new Operands(CONDITION_ALWAYS, 0)},
                                          new Operands[]{new Operands(ACTION_COUNT, 0), new Operands(ACTION_LOG, 6)});
        Game game = createGame(log, List.of(unknownCondition, unknownAction));

        game.getFrameRunner().runFrame();

        Assertions.assertEquals(List.of("player 0 action 6 frame 1"), log);
    }

    @Test
    void get_triggers_should_fail_without_a_registered_trigger_system() {
        Game game = Game.create(new GameDefinition.Builder().build(), createSetup());

        Assertions.assertFalse(game.hasSystem(TriggerSystem.KEY));
        Assertions.assertThrows(IllegalStateException.class, game::getTriggers);
    }

    private static @NotNull Game createGame(@NotNull List<@NotNull String> log, @NotNull List<@NotNull Script> scripts) {
        GameDefinition definition = new GameDefinition.Builder().useSystem(TriggerSystem.KEY, context -> new ScriptSystem(log, scripts))
                                                                .addFramePhase(TriggerSystem.KEY)
                                                                .build();

        return Game.create(definition, createSetup());
    }

    private static @NotNull GameSetup createSetup() {
        return new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                            .addPlayer(new PlayerSlot(1, ControllerKind.COMPUTER, 1, 1, null))
                                                                            .build();
    }
}

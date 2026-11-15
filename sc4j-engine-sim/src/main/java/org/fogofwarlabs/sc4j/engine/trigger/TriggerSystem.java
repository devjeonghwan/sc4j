package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.frame.FramePhase;
import org.fogofwarlabs.sc4j.engine.game.frame.PhaseSchedule;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.jetbrains.annotations.NotNull;

/**
 * Evaluates the triggers of a game: the map or mission script logic of a mod, as conditions that are tested and actions that run when they
 * hold. The engine fixes when the triggers run and what they are built from: the system is a frame phase of the mod's phase list (by default
 * also in paused frames, so that a trigger can end a pause), and a trigger is made of the {@link TriggerCondition} and {@link TriggerAction}
 * of its types, which the mod registers by type id in a {@link TriggerRegistry} and which read a {@link TriggerContext}. Which conditions and
 * actions exist, how triggers are stored, the timer of the evaluation and the order of the players' triggers are the mod's. A mod without
 * triggers registers no trigger system.
 * <p>
 * Determinism contract: {@link #run} runs at the system's position in the frame phase list, in every frame its schedule names. It walks the
 * players and their triggers in an order that depends only on the game state; it tests the conditions of a trigger in list order up to the
 * first that does not hold and runs the actions of a trigger whose conditions held in list order, up to the first that does not finish, at
 * which the next run of the trigger resumes. It draws random values only from the game's generator and allocates nothing per frame.
 */
public interface TriggerSystem extends GameSystem, FramePhase {
    /**
     * The key of the trigger system.
     */
    @NotNull SystemKey<TriggerSystem> KEY = SystemKey.of("triggers", TriggerSystem.class);

    /**
     * @return the frames the triggers run in: {@link PhaseSchedule#RUNNING_AND_PAUSED} unless overridden, so that a trigger can end a pause
     */
    @Override
    default @NotNull PhaseSchedule getSchedule() {
        return PhaseSchedule.RUNNING_AND_PAUSED;
    }


    /**
     * Evaluates the triggers of the current frame.
     *
     * @param game the game
     */
    @Override
    void run(@NotNull Game game);
}

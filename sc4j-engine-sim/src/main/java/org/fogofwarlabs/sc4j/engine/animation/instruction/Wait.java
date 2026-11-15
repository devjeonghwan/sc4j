package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.animation.AnimationContext;
import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.fogofwarlabs.sc4j.engine.animation.AnimationStep;
import org.jetbrains.annotations.NotNull;

/**
 * Pauses the program for a fixed number of ticks: it goes on at the next position in the update {@code ticks} updates
 * later.
 */
public final class Wait extends AnimationInstruction {
    private final int ticks;

    /**
     * Creates a {@link Wait} instance.
     *
     * @param ticks the number of updates, at least {@code 1}
     * @throws IllegalArgumentException if the ticks are below {@code 1}
     */
    public Wait(int ticks) {
        if (ticks < 1) {
            throw Exceptions.newIllegalArgumentException("A wait lasts at least 1 tick: {0}", ticks);
        }

        this.ticks = ticks;
    }

    /**
     * @return the number of updates the program pauses
     */
    public int getTicks() {
        return this.ticks;
    }


    /**
     * Runs this instruction for the animated object of the context.
     * <p>
     * Sets the wait and pauses the program.
     *
     * @param context the running animation of one object
     * @return how the program continues
     */
    @Override
    public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
        context.setWait(this.ticks);

        return AnimationStep.WAIT;
    }
}

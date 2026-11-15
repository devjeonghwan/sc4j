package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.engine.animation.AnimationContext;
import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.fogofwarlabs.sc4j.engine.animation.AnimationStep;
import org.jetbrains.annotations.NotNull;

/**
 * Moves the unit of the animated object forward ({@link org.fogofwarlabs.sc4j.engine.animation.AnimationEffects#move}) and
 * goes on.
 */
public final class Move extends AnimationInstruction {
    private final int distance;

    /**
     * Creates a {@link Move} instance.
     *
     * @param distance the distance in pixels
     */
    public Move(int distance) {
        this.distance = distance;
    }

    /**
     * @return the distance in pixels
     */
    public int getDistance() {
        return this.distance;
    }


    /**
     * Runs this instruction for the animated object of the context.
     * <p>
     * Moves the unit and continues at the next position.
     *
     * @param context the running animation of one object
     * @return how the program continues
     */
    @Override
    public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
        context.getEffects().move(context.getImage(), this.distance);

        return AnimationStep.CONTINUE;
    }
}

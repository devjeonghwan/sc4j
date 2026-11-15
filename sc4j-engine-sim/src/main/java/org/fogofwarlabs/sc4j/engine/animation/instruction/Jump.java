package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.animation.AnimationContext;
import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.fogofwarlabs.sc4j.engine.animation.AnimationStep;
import org.fogofwarlabs.sc4j.engine.animation.BranchInstruction;
import org.jetbrains.annotations.NotNull;

/**
 * Continues the program at a fixed position, such as the start of an animation of the same program.
 */
public final class Jump extends AnimationInstruction implements BranchInstruction {
    private final int targetPosition;

    /**
     * Creates a {@link Jump} instance.
     *
     * @param targetPosition the position the program continues at, at least {@code 0}
     * @throws IllegalArgumentException if the target position is negative
     */
    public Jump(int targetPosition) {
        if (targetPosition < 0) {
            throw Exceptions.newIllegalArgumentException("The jump target must not be negative: {0}", targetPosition);
        }

        this.targetPosition = targetPosition;
    }

    /**
     * @return the position the program continues at
     */
    @Override
    public int getTargetPosition() {
        return this.targetPosition;
    }


    /**
     * Runs this instruction for the animated object of the context.
     * <p>
     * Continues at the target position.
     *
     * @param context the running animation of one object
     * @return how the program continues
     */
    @Override
    public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
        context.jump(this.targetPosition);

        return AnimationStep.CONTINUE;
    }
}

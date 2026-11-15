package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.animation.*;
import org.jetbrains.annotations.NotNull;

/**
 * Calls a shared block: pushes the next position on the call stack and continues at a fixed position. A {@link Return}
 * in the block goes back.
 */
public final class Call extends AnimationInstruction implements BranchInstruction {
    private final int targetPosition;

    /**
     * Creates a {@link Call} instance.
     *
     * @param targetPosition the position of the called block, at least {@code 0}
     * @throws IllegalArgumentException if the target position is negative
     */
    public Call(int targetPosition) {
        if (targetPosition < 0) {
            throw Exceptions.newIllegalArgumentException("The call target must not be negative: {0}", targetPosition);
        }

        this.targetPosition = targetPosition;
    }

    /**
     * @return the position of the called block
     */
    @Override
    public int getTargetPosition() {
        return this.targetPosition;
    }


    /**
     * Runs this instruction for the animated object of the context.
     * <p>
     * Pushes the next position of this instruction ({@link AnimationProgram#getNextPosition}) and continues at the called block; a full call
     * stack fails with an {@link IllegalStateException}.
     *
     * @param context the running animation of one object
     * @return how the program continues
     */
    @Override
    public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
        context.pushCall(context.getProgram().getNextPosition(context.getPosition()));
        context.jump(this.targetPosition);

        return AnimationStep.CONTINUE;
    }
}

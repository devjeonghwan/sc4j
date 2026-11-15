package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.animation.AnimationContext;
import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.fogofwarlabs.sc4j.engine.animation.AnimationStep;
import org.jetbrains.annotations.NotNull;

/**
 * Shows a frame on the animated object ({@link org.fogofwarlabs.sc4j.engine.animation.AnimationTarget#showFrame}) and
 * goes on.
 */
public final class ShowFrame extends AnimationInstruction {
    private final int frame;

    /**
     * Creates a {@link ShowFrame} instance.
     *
     * @param frame the frame number, at least {@code 0}
     * @throws IllegalArgumentException if the frame number is negative
     */
    public ShowFrame(int frame) {
        if (frame < 0) {
            throw Exceptions.newIllegalArgumentException("The frame number must not be negative: {0}", frame);
        }

        this.frame = frame;
    }

    /**
     * @return the frame number
     */
    public int getFrame() {
        return this.frame;
    }


    /**
     * Runs this instruction for the animated object of the context.
     * <p>
     * Shows the frame and continues at the next position.
     *
     * @param context the running animation of one object
     * @return how the program continues
     */
    @Override
    public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
        context.getTarget().showFrame(context.getImage(), this.frame);

        return AnimationStep.CONTINUE;
    }
}

package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.engine.animation.AnimationContext;
import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.fogofwarlabs.sc4j.engine.animation.AnimationStep;
import org.jetbrains.annotations.NotNull;

/**
 * Ends the program; the animated object keeps the frame it shows.
 */
public final class End extends AnimationInstruction {
    /**
     * The instruction; it has no operand, so every program shares it.
     */
    public static final @NotNull End INSTANCE = new End();

    private End() {
    }


    /**
     * Runs this instruction for the animated object of the context.
     * <p>
     * Ends the program.
     *
     * @param context the running animation of one object
     * @return how the program continues
     */
    @Override
    public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
        return AnimationStep.END;
    }
}

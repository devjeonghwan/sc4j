package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.engine.animation.AnimationContext;
import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.fogofwarlabs.sc4j.engine.animation.AnimationStep;
import org.jetbrains.annotations.NotNull;

/**
 * Returns from a called block: pops the newest return position of the call stack and continues there.
 */
public final class Return extends AnimationInstruction {
    /**
     * The instruction; it has no operand, so every program shares it.
     */
    public static final @NotNull Return INSTANCE = new Return();

    private Return() {
    }


    /**
     * Runs this instruction for the animated object of the context.
     * <p>
     * Continues at the popped return position; an empty call stack fails with an {@link IllegalStateException}.
     *
     * @param context the running animation of one object
     * @return how the program continues
     */
    @Override
    public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
        context.jump(context.popCall());

        return AnimationStep.CONTINUE;
    }
}

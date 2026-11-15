package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.animation.AnimationContext;
import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.fogofwarlabs.sc4j.engine.animation.AnimationStep;
import org.jetbrains.annotations.NotNull;

/**
 * Pauses the program for a random number of ticks of an inclusive range, drawn with exactly one call of
 * {@link org.fogofwarlabs.sc4j.engine.random.DeterministicRandom#nextRange} from the animation source.
 */
public final class WaitRandom extends AnimationInstruction {
    private final int minimum;
    private final int maximum;

    /**
     * Creates a {@link WaitRandom} instance.
     *
     * @param minimum the lowest number of updates, at least {@code 1}
     * @param maximum the highest number of updates, at least {@code minimum}
     * @throws IllegalArgumentException if the minimum is below {@code 1} or above the maximum
     */
    public WaitRandom(int minimum, int maximum) {
        if (minimum < 1 || maximum < minimum) {
            throw Exceptions.newIllegalArgumentException("A random wait lasts 1 tick or more, minimum first: {0}..{1}", minimum, maximum);
        }

        this.minimum = minimum;
        this.maximum = maximum;
    }

    /**
     * @return the lowest number of updates the program pauses
     */
    public int getMinimum() {
        return this.minimum;
    }

    /**
     * @return the highest number of updates the program pauses
     */
    public int getMaximum() {
        return this.maximum;
    }


    /**
     * Runs this instruction for the animated object of the context.
     * <p>
     * Draws the wait, sets it and pauses the program.
     *
     * @param context the running animation of one object
     * @return how the program continues
     */
    @Override
    public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
        context.setWait(context.getRandom().nextRange(context.getRandomSource(), this.minimum, this.maximum));

        return AnimationStep.WAIT;
    }
}

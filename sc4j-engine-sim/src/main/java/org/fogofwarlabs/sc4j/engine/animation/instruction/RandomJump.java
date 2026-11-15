package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.animation.AnimationContext;
import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.fogofwarlabs.sc4j.engine.animation.AnimationStep;
import org.fogofwarlabs.sc4j.engine.animation.BranchInstruction;
import org.jetbrains.annotations.NotNull;

/**
 * Continues the program at a fixed position with a chance of {@code chance} in {@value #CHANCE_RANGE}, otherwise at the
 * next position. It draws exactly one value from {@code 0} up to {@value #CHANCE_RANGE} minus one with
 * {@link org.fogofwarlabs.sc4j.engine.random.DeterministicRandom#nextRange} from the animation source, also when the
 * chance is {@code 0} or {@value #CHANCE_RANGE}, and jumps when the value is below the chance.
 */
public final class RandomJump extends AnimationInstruction implements BranchInstruction {
    /**
     * The number of equally likely values a draw picks from.
     */
    public static final int CHANCE_RANGE = 256;

    private final int chance;
    private final int targetPosition;

    /**
     * Creates a {@link RandomJump} instance.
     *
     * @param chance         the chance in {@value #CHANCE_RANGE}, {@code 0} (never) up to {@value #CHANCE_RANGE} (always)
     * @param targetPosition the position the program continues at, at least {@code 0}
     * @throws IllegalArgumentException if the chance is out of range or the target position is negative
     */
    public RandomJump(int chance, int targetPosition) {
        if (chance < 0 || chance > CHANCE_RANGE || targetPosition < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid random jump: chance {0} to {1}", chance, targetPosition);
        }

        this.chance         = chance;
        this.targetPosition = targetPosition;
    }

    /**
     * @return the chance in {@value #CHANCE_RANGE} that the program jumps
     */
    public int getChance() {
        return this.chance;
    }

    /**
     * @return the position the program continues at when it jumps
     */
    @Override
    public int getTargetPosition() {
        return this.targetPosition;
    }


    /**
     * Runs this instruction for the animated object of the context.
     * <p>
     * Draws one value and continues at the target position if it is below the chance.
     *
     * @param context the running animation of one object
     * @return how the program continues
     */
    @Override
    public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
        if (context.getRandom().nextRange(context.getRandomSource(), 0, CHANCE_RANGE - 1) < this.chance) {
            context.jump(this.targetPosition);
        }

        return AnimationStep.CONTINUE;
    }
}

package org.fogofwarlabs.sc4j.engine.animation.system;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.animation.InstructionBudget;
import org.fogofwarlabs.sc4j.engine.entity.Image;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

/**
 * Thrown by a bounded update ({@link AnimationRunner#update(Image, InstructionBudget)}) when its
 * {@link InstructionBudget} is used up before an instruction, so that the caller can tell an exhausted budget from other
 * failures and report it in its own terms. The state then names the instruction that did not run.
 */
public final class InstructionBudgetExhaustedException extends IllegalStateException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an {@link InstructionBudgetExhaustedException} instance.
     *
     * @param index    the index of the object being updated
     * @param position the position of the instruction that did not run
     * @return the created {@link InstructionBudgetExhaustedException} instance
     */
    static @NotNull InstructionBudgetExhaustedException create(int index, int position) {
        String message = Exceptions.format("The instruction budget of object {0} is used up at position {1}.", index, position);

        return new InstructionBudgetExhaustedException(index, position, message);
    }


    private final int index;
    private final int position;

    /**
     * Creates an {@link InstructionBudgetExhaustedException} instance.
     *
     * @param index    the index of the object being updated
     * @param position the position of the instruction that did not run
     * @param message  the message
     */
    private InstructionBudgetExhaustedException(int index, int position, @NotNull String message) {
        super(message);

        this.index    = index;
        this.position = position;
    }

    /**
     * @return the index of the object being updated
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * @return the position of the instruction that did not run
     */
    public int getPosition() {
        return this.position;
    }
}

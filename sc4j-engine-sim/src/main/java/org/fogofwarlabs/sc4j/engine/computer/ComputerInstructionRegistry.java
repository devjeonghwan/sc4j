package org.fogofwarlabs.sc4j.engine.computer;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the {@link ComputerInstruction} of every opcode of one computer script format: one slot per opcode, {@code 0} up to the opcode
 * count minus one. The opcode is the key the scripts of the format store, as the command id is the key of a command stream, so the registry
 * is indexed by it; an interpreter finds the instruction of the opcode it reads here.
 * <p>
 * The mod fills a registry before the first frame and then {@link #freeze freezes} it; registering again before the freeze replaces the
 * instruction of the opcode. What an opcode without an instruction means is the interpreter's rule (for example: the script fails). A lookup
 * is an array read and allocates nothing.
 *
 * @param <I> the instruction type of the registry
 */
public final class ComputerInstructionRegistry<I extends ComputerInstruction<?>> {
    private final @Nullable Object @NotNull [] instructions;
    private boolean frozen;

    /**
     * Creates an empty {@link ComputerInstructionRegistry} instance.
     *
     * @param opcodeCount the number of opcodes, {@code 0} to {@code opcodeCount - 1}
     * @throws IllegalArgumentException if the count is negative
     */
    public ComputerInstructionRegistry(int opcodeCount) {
        if (opcodeCount < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid computer script opcode count: {0}", opcodeCount);
        }

        this.instructions = new Object[opcodeCount];
        this.frozen       = false;
    }

    /**
     * @return the number of opcodes
     */
    public int getCount() {
        return this.instructions.length;
    }

    /**
     * @return {@code true} if the registration has ended, {@code false} otherwise
     */
    public boolean isFrozen() {
        return this.frozen;
    }

    /**
     * Finds the instruction of an opcode.
     *
     * @param opcode the opcode, any value
     * @return the instruction, or {@code null} if the opcode has none or lies outside the registry
     */
    public @Nullable I find(int opcode) {
        return opcode >= 0 && opcode < this.instructions.length ? this.cast(this.instructions[opcode]) : null;
    }

    /**
     * Registers the instruction of an opcode, replacing a previous one.
     *
     * @param opcode      the opcode
     * @param instruction the instruction of the opcode
     * @throws IllegalArgumentException if the opcode lies outside the registry
     * @throws IllegalStateException    if the registry is frozen
     */
    public void register(int opcode, @NotNull I instruction) {
        if (this.frozen) {
            throw Exceptions.newIllegalStateException("The computer instruction registry is frozen; the opcode {0} cannot be registered.", opcode);
        }

        if (opcode < 0 || opcode >= this.instructions.length) {
            throw Exceptions.newIllegalArgumentException("Computer script opcode outside the table of {0}: {1}", this.instructions.length, opcode);
        }

        this.instructions[opcode] = instruction;
    }

    /**
     * Ends the registration; later registrations fail. Freezing again has no effect.
     */
    public void freeze() {
        this.frozen = true;
    }

    @SuppressWarnings("unchecked")
    private @Nullable I cast(@Nullable Object instruction) {
        return (I) instruction;
    }
}

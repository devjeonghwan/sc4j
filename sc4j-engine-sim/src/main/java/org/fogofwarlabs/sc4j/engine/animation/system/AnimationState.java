package org.fogofwarlabs.sc4j.engine.animation.system;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.animation.AnimationLibrary;
import org.fogofwarlabs.sc4j.engine.animation.AnimationProgram;
import org.fogofwarlabs.sc4j.engine.animation.AnimationSet;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;

/**
 * Holds the running animation of every animated object of one game in arrays indexed by the object index, {@code 0} up
 * to {@link #getLength()} minus one; an owner whose indices start at {@code 1} leaves index {@code 0} unused.
 * <p>
 * Per index: the animation set and kind that were started, the running program, the position of the next instruction,
 * the wait timer (the number of later updates that only count down before the program goes on), the call stack (depth
 * and {@link #getCallStackCapacity()} entries, entry {@code 0} the oldest) and the running flag. The accessors are
 * public so that the owner of the objects can save, restore and inspect the state by index, one value at a time in any
 * order (a position set before the program is kept and checked when the program is set); the runner
 * ({@link AnimationRunner}) changes it while the programs run.
 * <p>
 * These index accessors are the documented {@code int} form of the animation state: the owner names its objects by
 * index, and a mod's adapter may address the state by the same index. Positions are program positions; a mod whose own
 * scripts address code by byte offset converts with its own map between offsets and positions, which the engine does not
 * hold.
 * <p>
 * Determinism contract: the state hash mixes every index in ascending order: the set index, the kind index and the
 * program index ({@code -1} for none), the position, the wait timer, the call depth, every call stack entry and the
 * running flag.
 */
public final class AnimationState implements StateHashContributor {
    /**
     * The kind index of an object that runs no animation.
     */
    public static final int NO_KIND = -1;

    /**
     * The index mixed into the state hash for a missing set or program.
     */
    private static final int NO_INDEX = -1;

    private final int callStackCapacity;
    private int length;

    private @Nullable AnimationSet @NotNull [] sets;
    private int @NotNull [] kindIndices;
    private @Nullable AnimationProgram @NotNull [] programs;
    private int @NotNull [] positions;
    private int @NotNull [] waits;
    private int @NotNull [] callDepths;
    private int @NotNull [] callEntries;
    private boolean @NotNull [] running;

    /**
     * Creates an {@link AnimationState} instance in which no object runs an animation.
     *
     * @param length            the number of indices, at least {@code 0}
     * @param callStackCapacity the number of return positions each call stack holds, at least {@code 0}
     * @throws IllegalArgumentException if a value is negative
     */
    public AnimationState(int length, int callStackCapacity) {
        if (length < 0 || callStackCapacity < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid animation state of {0} indices and call stacks of {1}", length, callStackCapacity);
        }

        this.callStackCapacity = callStackCapacity;
        this.length            = length;

        this.sets        = new AnimationSet[length];
        this.kindIndices = new int[length];
        this.programs    = new AnimationProgram[length];
        this.positions   = new int[length];
        this.waits       = new int[length];
        this.callDepths  = new int[length];
        this.callEntries = new int[length * callStackCapacity];
        this.running     = new boolean[length];

        Arrays.fill(this.kindIndices, NO_KIND);
    }

    /**
     * @return the number of indices
     */
    public int getLength() {
        return this.length;
    }

    /**
     * @return the number of return positions each call stack holds
     */
    public int getCallStackCapacity() {
        return this.callStackCapacity;
    }

    /**
     * Returns the animation set an object was started with.
     *
     * @param index the object index
     * @return the set, or {@code null} if no animation was started since the index was cleared
     */
    public @Nullable AnimationSet getSet(int index) {
        return this.sets[index];
    }

    /**
     * Returns the kind index of the animation an object was started with.
     *
     * @param index the object index
     * @return the kind index, or {@link #NO_KIND}
     */
    public int getKindIndex(int index) {
        return this.kindIndices[index];
    }

    /**
     * Sets the kind index an object reports, for example when its state is restored.
     *
     * @param index     the object index
     * @param kindIndex the kind index, or {@link #NO_KIND}
     * @throws IllegalArgumentException if the kind index is below {@link #NO_KIND}
     */
    public void setKindIndex(int index, int kindIndex) {
        if (kindIndex < NO_KIND) {
            throw Exceptions.newIllegalArgumentException("Invalid animation kind index: {0}", kindIndex);
        }

        this.kindIndices[index] = kindIndex;
    }

    /**
     * Returns the running program of an object.
     *
     * @param index the object index
     * @return the program, or {@code null} if no animation was started since the index was cleared
     */
    public @Nullable AnimationProgram getProgram(int index) {
        return this.programs[index];
    }

    /**
     * Sets the program of an object, keeping its position, for example when its state is restored.
     *
     * @param index   the object index
     * @param program the program
     * @throws IllegalArgumentException if the object's position lies outside the program
     */
    public void setProgram(int index, @NotNull AnimationProgram program) {
        if (!program.contains(this.positions[index])) {
            throw Exceptions.newIllegalArgumentException("The position {0} lies outside the program {1}.", this.positions[index], program);
        }

        this.programs[index] = program;
    }

    /**
     * Returns the position of the next instruction of an object.
     *
     * @param index the object index
     * @return the position
     */
    public int getPosition(int index) {
        return this.positions[index];
    }

    /**
     * Sets the position of the next instruction of an object, for example when its state is restored. An object without a
     * program keeps the position for the program set later ({@link #setProgram}).
     *
     * @param index    the object index
     * @param position the position in the object's program, at least {@code 0}
     * @throws IllegalArgumentException if the position is negative or lies outside the object's program
     */
    public void setPosition(int index, int position) {
        AnimationProgram program = this.programs[index];

        if (position < 0 || (program != null && !program.contains(position))) {
            throw Exceptions.newIllegalArgumentException("The position {0} lies outside the program {1}.", position, program);
        }

        this.positions[index] = position;
    }

    /**
     * Returns the wait timer of an object.
     *
     * @param index the object index
     * @return the number of later updates that only count down before the program goes on
     */
    public int getWait(int index) {
        return this.waits[index];
    }

    /**
     * Sets the wait timer of an object.
     *
     * @param index the object index
     * @param wait  the number of later updates that only count down before the program goes on, at least {@code 0}
     * @throws IllegalArgumentException if the wait is negative
     */
    public void setWait(int index, int wait) {
        if (wait < 0) {
            throw Exceptions.newIllegalArgumentException("The wait timer must not be negative: {0}", wait);
        }

        this.waits[index] = wait;
    }

    /**
     * Returns the call depth of an object.
     *
     * @param index the object index
     * @return the number of return positions on its call stack
     */
    public int getCallDepth(int index) {
        return this.callDepths[index];
    }

    /**
     * Sets the call depth of an object; the entries keep their values.
     *
     * @param index the object index
     * @param depth the depth, {@code 0} up to {@link #getCallStackCapacity()}
     * @throws IndexOutOfBoundsException if the depth is out of range
     */
    public void setCallDepth(int index, int depth) {
        if (depth < 0 || depth > this.callStackCapacity) {
            throw Exceptions.newIndexOutOfBoundsException("The call depth must be 0..{0}: {1}", this.callStackCapacity, depth);
        }

        this.callDepths[index] = depth;
    }

    /**
     * Returns one entry of the call stack of an object, in use or not.
     *
     * @param index the object index
     * @param level the entry, {@code 0} up to {@link #getCallStackCapacity()} minus one
     * @return the return position of the entry
     * @throws IndexOutOfBoundsException if the level is out of range
     */
    public int getCallEntry(int index, int level) {
        return this.callEntries[this.getCallEntryOffset(index, level)];
    }

    /**
     * Sets one entry of the call stack of an object, in use or not.
     *
     * @param index    the object index
     * @param level    the entry, {@code 0} up to {@link #getCallStackCapacity()} minus one
     * @param position the return position
     * @throws IndexOutOfBoundsException if the level is out of range
     */
    public void setCallEntry(int index, int level, int position) {
        this.callEntries[this.getCallEntryOffset(index, level)] = position;
    }

    private int getCallEntryOffset(int index, int level) {
        if (level < 0 || level >= this.callStackCapacity) {
            throw Exceptions.newIndexOutOfBoundsException("The call stack level must be 0..{0}: {1}", this.callStackCapacity - 1, level);
        }

        return index * this.callStackCapacity + level;
    }

    /**
     * Checks whether an object runs its program.
     *
     * @param index the object index
     * @return {@code true} if the program runs, {@code false} if it ended, was stopped or never started
     */
    public boolean isRunning(int index) {
        return this.running[index];
    }

    /**
     * Sets whether an object runs its program, for example when its state is restored.
     *
     * @param index   the object index
     * @param running whether the program runs
     * @throws IllegalStateException if the program should run but the object has none
     */
    public void setRunning(int index, boolean running) {
        if (running && this.programs[index] == null) {
            throw Exceptions.newIllegalStateException("The object {0} has no animation program to run.", index);
        }

        this.running[index] = running;
    }


    /**
     * Starts an animation on an object: sets its set, kind, program and position, clears its wait timer and call stack
     * depth and marks it running. No instruction runs.
     *
     * @param index     the object index
     * @param set       the animation set
     * @param kindIndex the kind index
     * @param program   the program
     * @param position  the start position in the program
     * @throws IllegalArgumentException if the position lies outside the program
     */
    public void start(int index, @NotNull AnimationSet set, int kindIndex, @NotNull AnimationProgram program, int position) {
        if (!program.contains(position)) {
            throw Exceptions.newIllegalArgumentException("The position {0} lies outside the program {1}.", position, program);
        }

        this.sets[index]        = set;
        this.kindIndices[index] = kindIndex;
        this.programs[index]    = program;
        this.positions[index]   = position;
        this.waits[index]       = 0;
        this.callDepths[index]  = 0;
        this.running[index]     = true;
    }

    /**
     * Clears an object: no set, kind or program, position, wait timer, call depth and every call stack entry {@code 0},
     * not running.
     *
     * @param index the object index
     */
    public void clear(int index) {
        this.sets[index]        = null;
        this.kindIndices[index] = NO_KIND;
        this.programs[index]    = null;
        this.positions[index]   = 0;
        this.waits[index]       = 0;
        this.callDepths[index]  = 0;
        this.running[index]     = false;

        Arrays.fill(this.callEntries, index * this.callStackCapacity, (index + 1) * this.callStackCapacity, 0);
    }

    /**
     * Grows the state to at least a number of indices, for an owner whose pool grows; the new indices are cleared. This
     * allocates and is not meant for every frame.
     *
     * @param minimumLength the number of indices the state must have
     */
    public void ensureLength(int minimumLength) {
        if (minimumLength <= this.length) {
            return;
        }

        int oldLength = this.length;

        this.length      = minimumLength;
        this.sets        = Arrays.copyOf(this.sets, minimumLength);
        this.kindIndices = Arrays.copyOf(this.kindIndices, minimumLength);
        this.programs    = Arrays.copyOf(this.programs, minimumLength);
        this.positions   = Arrays.copyOf(this.positions, minimumLength);
        this.waits       = Arrays.copyOf(this.waits, minimumLength);
        this.callDepths  = Arrays.copyOf(this.callDepths, minimumLength);
        this.callEntries = Arrays.copyOf(this.callEntries, minimumLength * this.callStackCapacity);
        this.running     = Arrays.copyOf(this.running, minimumLength);

        Arrays.fill(this.kindIndices, oldLength, minimumLength, NO_KIND);
    }

    /**
     * Sets the position of the next instruction as the runner left it, which may be the program length when the last
     * instruction paused or ended the program.
     *
     * @param index    the object index
     * @param position the position
     */
    void storePosition(int index, int position) {
        this.positions[index] = position;
    }

    /**
     * Ends the program of an object as the runner reached it: stores the position and clears the running flag.
     *
     * @param index    the object index
     * @param position the position after the ending instruction
     */
    void storeEnd(int index, int position) {
        this.positions[index] = position;
        this.running[index]   = false;
    }

    /**
     * Mixes this part of the simulation state into the hash: every index in ascending order, as the class description
     * lists.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        hash.mix(this.length);

        for (int index = 0; index < this.length; index++) {
            this.contributeStateHash(hash, index);
        }
    }

    /**
     * Mixes the state of one object into the hash, in the order of the class description.
     *
     * @param hash  the hash being computed
     * @param index the object index
     */
    public void contributeStateHash(@NotNull StateHash hash, int index) {
        AnimationSet set = this.sets[index];
        AnimationProgram program = this.programs[index];

        hash.mix(set != null ? set.getIndex() : NO_INDEX);
        hash.mix(this.kindIndices[index]);
        hash.mix(program != null ? program.getIndex() : NO_INDEX);
        hash.mix(this.positions[index]);
        hash.mix(this.waits[index]);
        hash.mix(this.callDepths[index]);

        for (int level = 0; level < this.callStackCapacity; level++) {
            hash.mix(this.callEntries[index * this.callStackCapacity + level]);
        }

        hash.mixBoolean(this.running[index]);
    }

    /**
     * Writes every index in ascending order, after the number of indices: the set index, the kind index and the program index ({@code -1}
     * for none), the position, the wait timer, the call depth, every call stack entry and the running flag.
     *
     * @param writer the writer of the snapshot
     */
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInt(this.length);

        for (int index = 0; index < this.length; index++) {
            AnimationSet set = this.sets[index];
            AnimationProgram program = this.programs[index];

            writer.writeInt(set != null ? set.getIndex() : NO_INDEX);
            writer.writeInt(this.kindIndices[index]);
            writer.writeInt(program != null ? program.getIndex() : NO_INDEX);
            writer.writeInt(this.positions[index]);
            writer.writeInt(this.waits[index]);
            writer.writeInt(this.callDepths[index]);

            for (int level = 0; level < this.callStackCapacity; level++) {
                writer.writeInt(this.callEntries[index * this.callStackCapacity + level]);
            }

            writer.writeBoolean(this.running[index]);
        }
    }

    /**
     * Reads every index back in the order of {@link #writeSnapshot}, replacing the current values; the state grows to the written number of
     * indices first.
     *
     * @param reader  the reader of the snapshot
     * @param library the library whose sets and programs the indices name
     * @throws IOException if the snapshot ends early, holds fewer indices than this state or names no set or program of the library
     */
    public void readSnapshot(@NotNull StateSnapshotReader reader, @NotNull AnimationLibrary library) throws IOException {
        int writtenLength = reader.readInt();

        this.ensureLength(writtenLength);

        if (writtenLength != this.length) {
            throw Exceptions.newIOException("The state snapshot holds {0} animations where the game holds {1}.", writtenLength, this.length);
        }

        for (int index = 0; index < this.length; index++) {
            int setIndex = reader.readInt();
            int kindIndex = reader.readInt();
            int programIndex = reader.readInt();

            if (setIndex < NO_INDEX || setIndex >= library.getSets().size() || programIndex < NO_INDEX ||
                programIndex >= library.getPrograms().size() || kindIndex < NO_KIND) {
                throw Exceptions.newIOException("The state snapshot names the animation set {0}, kind {1} and program {2}.",
                                                setIndex, kindIndex, programIndex);
            }

            this.sets[index]        = setIndex != NO_INDEX ? library.getSet(setIndex) : null;
            this.kindIndices[index] = kindIndex;
            this.programs[index]    = programIndex != NO_INDEX ? library.getPrograms().get(programIndex) : null;
            this.positions[index]   = reader.readInt();
            this.waits[index]       = reader.readInt();
            this.callDepths[index]  = reader.readInt();

            for (int level = 0; level < this.callStackCapacity; level++) {
                this.callEntries[index * this.callStackCapacity + level] = reader.readInt();
            }

            this.running[index] = reader.readBoolean();
        }
    }
}

package org.fogofwarlabs.sc4j.engine.entity.pool;

import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

/**
 * Links slot indices {@code 1} up to the capacity into one doubly linked list; {@link #NONE} ends the list.
 * <p>
 * The list keeps no membership flag: the owner of the list adds an index only when it is not linked and removes it only
 * when it is. The raw setters write one link or end as they are, for mods that reproduce another engine's list writes;
 * they do not update the size.
 */
public final class SlotList implements SnapshotContributor {
    /**
     * The index that ends the list and names no slot.
     */
    public static final int NONE = 0;

    private final int @NotNull [] nextIndices;
    private final int @NotNull [] previousIndices;

    private int firstIndex;
    private int lastIndex;
    private int size;

    /**
     * Creates an empty {@link SlotList} instance.
     *
     * @param capacity the highest slot index
     */
    public SlotList(int capacity) {
        this.nextIndices     = new int[capacity + 1];
        this.previousIndices = new int[capacity + 1];

        this.firstIndex = NONE;
        this.lastIndex  = NONE;
        this.size       = 0;
    }

    /**
     * @return the first index, or {@link #NONE} if the list is empty
     */
    public int getFirst() {
        return this.firstIndex;
    }

    /**
     * Sets the first index as it is, without relinking.
     *
     * @param index the first index, or {@link #NONE}
     */
    public void setFirst(int index) {
        this.firstIndex = index;
    }

    /**
     * @return the last index, or {@link #NONE} if the list is empty
     */
    public int getLast() {
        return this.lastIndex;
    }

    /**
     * Sets the last index as it is, without relinking.
     *
     * @param index the last index, or {@link #NONE}
     */
    public void setLast(int index) {
        this.lastIndex = index;
    }

    /**
     * Returns the index after a listed index.
     *
     * @param index the listed index
     * @return the next index, or {@link #NONE} after the last index
     */
    public int getNext(int index) {
        return this.nextIndices[index];
    }

    /**
     * Sets the index after an index as it is, without relinking.
     *
     * @param index the index
     * @param next  the next index, or {@link #NONE}
     */
    public void setNext(int index, int next) {
        this.nextIndices[index] = next;
    }

    /**
     * Returns the index before a listed index.
     *
     * @param index the listed index
     * @return the previous index, or {@link #NONE} before the first index
     */
    public int getPrevious(int index) {
        return this.previousIndices[index];
    }

    /**
     * Sets the index before an index as it is, without relinking.
     *
     * @param index    the index
     * @param previous the previous index, or {@link #NONE}
     */
    public void setPrevious(int index, int previous) {
        this.previousIndices[index] = previous;
    }

    /**
     * @return the number of indices the structured operations linked
     */
    public int getSize() {
        return this.size;
    }

    /**
     * @return {@code true} if the list has no first index, {@code false} otherwise
     */
    public boolean isEmpty() {
        return this.firstIndex == NONE;
    }


    /**
     * Links an index before the first index.
     *
     * @param index the unlinked index
     */
    public void addFirst(int index) {
        int following = this.firstIndex;

        this.previousIndices[index] = NONE;
        this.nextIndices[index]     = following;

        if (following == NONE) {
            this.lastIndex = index;
        } else {
            this.previousIndices[following] = index;
        }

        this.firstIndex = index;
        this.size++;
    }

    /**
     * Links an index after the last index.
     *
     * @param index the unlinked index
     */
    public void addLast(int index) {
        int preceding = this.lastIndex;

        this.previousIndices[index] = preceding;
        this.nextIndices[index]     = NONE;

        if (preceding == NONE) {
            this.firstIndex = index;
        } else {
            this.nextIndices[preceding] = index;
        }

        this.lastIndex = index;
        this.size++;
    }

    /**
     * Links an index after a listed index.
     *
     * @param existing the listed index
     * @param index    the unlinked index
     */
    public void addAfter(int existing, int index) {
        int following = this.nextIndices[existing];

        this.previousIndices[index] = existing;
        this.nextIndices[index]     = following;
        this.nextIndices[existing]  = index;

        if (following == NONE) {
            this.lastIndex = index;
        } else {
            this.previousIndices[following] = index;
        }

        this.size++;
    }

    /**
     * Unlinks a listed index; its own links become {@link #NONE}.
     *
     * @param index the listed index
     */
    public void remove(int index) {
        int preceding = this.previousIndices[index];
        int following = this.nextIndices[index];

        if (preceding == NONE) {
            this.firstIndex = following;
        } else {
            this.nextIndices[preceding] = following;
        }

        if (following == NONE) {
            this.lastIndex = preceding;
        } else {
            this.previousIndices[following] = preceding;
        }

        this.previousIndices[index] = NONE;
        this.nextIndices[index]     = NONE;
        this.size--;
    }

    /**
     * Unlinks every index.
     */
    public void clear() {
        Arrays.fill(this.nextIndices, NONE);
        Arrays.fill(this.previousIndices, NONE);

        this.firstIndex = NONE;
        this.lastIndex  = NONE;
        this.size       = 0;
    }

    /**
     * Writes the list: its first and last index, its size and every link.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInt(this.firstIndex);
        writer.writeInt(this.lastIndex);
        writer.writeInt(this.size);
        writer.writeInts(this.nextIndices);
        writer.writeInts(this.previousIndices);
    }

    /**
     * Reads the list back, replacing its ends, size and links.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or holds another capacity
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        this.firstIndex = reader.readInt();
        this.lastIndex  = reader.readInt();
        this.size       = reader.readInt();

        reader.readInts(this.nextIndices);
        reader.readInts(this.previousIndices);
    }
}

package org.fogofwarlabs.sc4j.beaconlink.protocol.file;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Tells why a node list file could not be read: the kind, the position of the refused entry (for {@link NodeListProblemKind#INVALID_ENTRY})
 * and a detail text in English. A user interface shows its own text for the kind and may add the detail.
 */
public final class NodeListProblem {
    /**
     * The entry position of a problem that concerns no entry.
     */
    public static final int NO_ENTRY = -1;

    private final @NotNull NodeListProblemKind kind;
    private final int entryIndex;
    private final @NotNull String detail;

    /**
     * Creates a {@link NodeListProblem} instance.
     *
     * @param kind       why the file could not be read
     * @param entryIndex the position of the refused entry in the {@code nodes} array, from {@code 0}, or {@link #NO_ENTRY}
     * @param detail     the detail text in English
     */
    public NodeListProblem(@NotNull NodeListProblemKind kind, int entryIndex, @NotNull String detail) {
        this.kind       = kind;
        this.entryIndex = entryIndex;
        this.detail     = detail;
    }

    /**
     * @return why the file could not be read
     */
    public @NotNull NodeListProblemKind getKind() {
        return this.kind;
    }

    /**
     * @return the position of the refused entry in the {@code nodes} array, from {@code 0}, or {@link #NO_ENTRY}
     */
    public int getEntryIndex() {
        return this.entryIndex;
    }

    /**
     * @return the detail text in English
     */
    public @NotNull String getDetail() {
        return this.detail;
    }


    /**
     * @return the kind, the entry number (from {@code 1}) when the problem concerns an entry, and the detail
     */
    @Override
    public @NotNull String toString() {
        if (this.entryIndex == NO_ENTRY) {
            return Exceptions.format("{0}: {1}", this.kind, this.detail);
        }

        return Exceptions.format("{0} (entry {1}): {2}", this.kind, this.entryIndex + 1, this.detail);
    }
}

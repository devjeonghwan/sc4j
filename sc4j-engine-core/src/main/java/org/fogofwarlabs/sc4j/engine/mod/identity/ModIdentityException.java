package org.fogofwarlabs.sc4j.engine.mod.identity;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;

/**
 * Signals that a file of the engine formats (a replay, a saved game) was written by another mod identity than the one that loads it: it would
 * not run the same game, so it is refused. {@link #getMismatch()} names the first differing part (the reason), {@link #getRecordedIdentity()}
 * the identity of the writer. A file that is damaged or of another kind is reported by a plain {@link IOException} instead.
 * <p>
 * The exception survives serialization with both: the recorded identity travels in its wire form ({@link ModIdentity#write}).
 */
public final class ModIdentityException extends IOException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@link ModIdentityException} instance for a refused file.
     *
     * @param fileKind the kind of the file for people, such as {@code replay} or {@code saved game}
     * @param mismatch the first part in which the identities differ
     * @param recorded the identity of the mod that wrote the file
     * @param local    the identity of the mod that loads it
     * @return the created {@link ModIdentityException} instance
     */
    public static @NotNull ModIdentityException create(@NotNull String fileKind, @NotNull ModIdentityMismatch mismatch,
                                                       @NotNull ModIdentity recorded, @NotNull ModIdentity local) {
        String message = Exceptions.format("The {0} was written by {1}, which differs from {2} in its {3}.",
                                           fileKind, recorded.describe(), local.describe(), mismatch.getDescription());

        return new ModIdentityException(mismatch, recorded, message);
    }


    private final @NotNull ModIdentityMismatch mismatch;
    /**
     * The identity of the writer; serialized in its wire form ({@code writeObject}), so it is not final.
     */
    private transient @NotNull ModIdentity recordedIdentity;

    /**
     * Creates a {@link ModIdentityException} instance.
     *
     * @param mismatch         the first part in which the identities differ
     * @param recordedIdentity the identity of the mod that wrote the file
     * @param message          the formatted message
     */
    private ModIdentityException(@NotNull ModIdentityMismatch mismatch, @NotNull ModIdentity recordedIdentity, @NotNull String message) {
        super(message);

        this.mismatch         = mismatch;
        this.recordedIdentity = recordedIdentity;
    }

    /**
     * @return the first part in which the writer's identity differs from the loading one, the reason of the refusal
     */
    public @NotNull ModIdentityMismatch getMismatch() {
        return this.mismatch;
    }

    /**
     * @return the identity of the mod that wrote the file
     */
    public @NotNull ModIdentity getRecordedIdentity() {
        return this.recordedIdentity;
    }

    @Serial
    private void writeObject(@NotNull ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
        this.recordedIdentity.write(output);
    }

    @Serial
    private void readObject(@NotNull ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        this.recordedIdentity = ModIdentity.read(input);
    }
}

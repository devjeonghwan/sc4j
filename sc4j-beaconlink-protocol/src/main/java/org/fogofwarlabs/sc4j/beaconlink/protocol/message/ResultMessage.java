package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Answers a room, signal, announce or graft request with a status and a value.
 * <p>
 * Body: the status (one byte, {@link ResultStatus}) and the value (eight bytes): the lifetime in seconds of an accepted
 * {@link AnnounceMessage}, otherwise {@code 0}.
 */
public final class ResultMessage extends Message {
    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or the status is unknown
     */
    public static @Nullable ResultMessage read(@NotNull WireReader reader) {
        ResultStatus status = ResultStatus.find(reader.readUnsigned8());
        long value = reader.readInt64();

        if (status == null || reader.isFailed()) {
            return null;
        }

        return new ResultMessage(status, value);
    }


    private final @NotNull ResultStatus status;
    private final long value;

    /**
     * Creates a {@link ResultMessage} instance.
     *
     * @param status the status
     * @param value  the value: the lifetime in seconds of an accepted announcement, otherwise {@code 0}
     */
    public ResultMessage(@NotNull ResultStatus status, long value) {
        this.status = status;
        this.value  = value;
    }

    /**
     * @return {@link MessageKind#RESULT}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.RESULT;
    }

    /**
     * @return the status
     */
    public @NotNull ResultStatus getStatus() {
        return this.status;
    }

    /**
     * @return the value: the lifetime in seconds of an accepted announcement, otherwise {@code 0}
     */
    public long getValue() {
        return this.value;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        writer.writeUnsigned8(this.status.getCode());
        writer.writeInt64(this.value);
    }
}

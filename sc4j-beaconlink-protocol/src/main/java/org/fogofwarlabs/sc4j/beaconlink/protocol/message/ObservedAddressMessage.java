package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.CoordinateReport;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;

/**
 * Answers a registration with the source address the node observed, so that a client learns its outside address without an outside
 * service, and with the node's network coordinate, with which the client updates its own coordinate from the round trip time of the
 * registration.
 * <p>
 * Body: the status (one byte, {@link ObservedAddressStatus}), the observed address, the granted lifetime in seconds (two bytes, {@code 0}
 * unless registered), the address token (length byte, at most {@value RegisterMessage#MAXIMUM_TOKEN_LENGTH} bytes; present with
 * {@link ObservedAddressStatus#TOKEN_REQUIRED}) and the node's {@link CoordinateReport}.
 */
public final class ObservedAddressMessage extends Message {
    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or a field is out of range
     */
    public static @Nullable ObservedAddressMessage read(@NotNull WireReader reader) {
        ObservedAddressStatus status = ObservedAddressStatus.find(reader.readUnsigned8());
        InetSocketAddress address = reader.readAddress();
        int lifetimeSeconds = reader.readUnsigned16();
        int tokenLength = reader.readUnsigned8();

        if (tokenLength > RegisterMessage.MAXIMUM_TOKEN_LENGTH) {
            reader.fail();
        }

        byte[] token = reader.readBytes(tokenLength);
        CoordinateReport coordinate = CoordinateReport.read(reader);

        if (status == null || address == null || coordinate == null || reader.isFailed()) {
            return null;
        }

        return new ObservedAddressMessage(status, address, lifetimeSeconds, token, coordinate);
    }


    private final @NotNull ObservedAddressStatus status;
    private final @NotNull InetSocketAddress address;
    private final int lifetimeSeconds;
    private final byte @NotNull [] token;
    private final @NotNull CoordinateReport coordinate;

    /**
     * Creates an {@link ObservedAddressMessage} instance.
     *
     * @param status          the registration status
     * @param address         the observed source address, resolved
     * @param lifetimeSeconds the granted lifetime in seconds
     * @param token           the address token to repeat, or an empty array
     * @param coordinate      the node's network coordinate and its error
     * @throws IllegalArgumentException if the address is not resolved, or the lifetime or the token is out of range
     */
    public ObservedAddressMessage(@NotNull ObservedAddressStatus status, @NotNull InetSocketAddress address, int lifetimeSeconds,
                                  byte @NotNull [] token, @NotNull CoordinateReport coordinate) {
        if (address.isUnresolved() || lifetimeSeconds < 0 || lifetimeSeconds > RegisterMessage.MAXIMUM_LIFETIME_SECONDS ||
            token.length > RegisterMessage.MAXIMUM_TOKEN_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The address {0}, the lifetime {1} or the token length {2} is out of range.",
                                                         address, lifetimeSeconds, token.length);
        }

        this.status          = status;
        this.address         = address;
        this.lifetimeSeconds = lifetimeSeconds;
        this.token           = token.clone();
        this.coordinate      = coordinate;
    }

    /**
     * @return {@link MessageKind#OBSERVED_ADDRESS}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.OBSERVED_ADDRESS;
    }

    /**
     * @return the registration status
     */
    public @NotNull ObservedAddressStatus getStatus() {
        return this.status;
    }

    /**
     * @return the source address the node observed
     */
    public @NotNull InetSocketAddress getAddress() {
        return this.address;
    }

    /**
     * @return the granted lifetime in seconds, {@code 0} unless registered
     */
    public int getLifetimeSeconds() {
        return this.lifetimeSeconds;
    }

    /**
     * @return a copy of the address token to repeat in the next registration, empty if none
     */
    public byte @NotNull [] getToken() {
        return this.token.clone();
    }

    /**
     * @return the node's network coordinate and its error
     */
    public @NotNull CoordinateReport getCoordinate() {
        return this.coordinate;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        writer.writeUnsigned8(this.status.getCode());
        writer.writeAddress(this.address);
        writer.writeUnsigned16(this.lifetimeSeconds);
        writer.writeUnsigned8(this.token.length);
        writer.writeBytes(this.token);
        this.coordinate.write(writer);
    }
}

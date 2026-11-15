package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerNameUtils;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerStamp;
import org.fogofwarlabs.sc4j.beaconlink.protocol.room.ModFingerprint;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers the sender at a node, or removes its registration (lifetime {@code 0}). The first registration from an address carries no token;
 * the node answers {@link ObservedAddressStatus#TOKEN_REQUIRED} with an address token, and the repeated registration with that token
 * registers the sender at its observed address.
 * <p>
 * The connection number tells the node which connection of the player's key registers: a client session keeps one number for all its
 * registrations, so that a registration of the same connection from a new address (a NAT that changed the client's port) moves it, while
 * another connection of the key is registered beside it, up to the node's limit per key ({@link ObservedAddressStatus#TOO_MANY_CONNECTIONS}
 * beyond it). The connection start is the epoch millisecond at which the client made the connection number.
 * <p>
 * The display name and the stamp nonce are the player's stamp ({@link #getStamp}): the node registers only a sender whose stamp binds the
 * name to its key ({@link ObservedAddressStatus#INVALID_NAME} otherwise).
 * <p>
 * Body: the address token (length byte, at most {@value #MAXIMUM_TOKEN_LENGTH} bytes), the requested lifetime in seconds (two bytes), the
 * display name (UTF-8, at most {@value #MAXIMUM_NAME_LENGTH} bytes), the stamp nonce (eight bytes), the mod fingerprint, the connection
 * number (eight bytes), the connection start (eight bytes) and the flags (one byte, all zero).
 */
public final class RegisterMessage extends Message {
    /**
     * The largest address token length.
     */
    public static final int MAXIMUM_TOKEN_LENGTH = 32;
    /**
     * The largest UTF-8 length of the display name.
     */
    public static final int MAXIMUM_NAME_LENGTH = PlayerNameUtils.MAXIMUM_LENGTH;
    /**
     * The largest requested lifetime in seconds.
     */
    public static final int MAXIMUM_LIFETIME_SECONDS = 0xffff;

    /**
     * Reads the body.
     *
     * @param reader the body reader
     * @return the body, or {@code null} if the reader failed or a field is out of range
     */
    public static @Nullable RegisterMessage read(@NotNull WireReader reader) {
        int tokenLength = reader.readUnsigned8();

        if (tokenLength > MAXIMUM_TOKEN_LENGTH) {
            reader.fail();
        }

        byte[] token = reader.readBytes(tokenLength);
        int lifetimeSeconds = reader.readUnsigned16();
        String displayName = reader.readText(MAXIMUM_NAME_LENGTH);
        long stampNonce = reader.readInt64();
        ModFingerprint fingerprint = ModFingerprint.read(reader);
        long connection = reader.readInt64();
        long connectionStartMillis = reader.readInt64();
        int flags = reader.readUnsigned8();

        if (fingerprint == null || flags != 0 || reader.isFailed()) {
            return null;
        }

        return new RegisterMessage(token, lifetimeSeconds, displayName, stampNonce, fingerprint, connection, connectionStartMillis);
    }


    private final byte @NotNull [] token;
    private final int lifetimeSeconds;

    private final @NotNull String displayName;
    private final long stampNonce;
    private final @NotNull ModFingerprint fingerprint;

    private final long connection;
    private final long connectionStartMillis;

    /**
     * Creates a {@link RegisterMessage} instance.
     *
     * @param token                 the address token of the node's last answer, or an empty array for the first registration
     * @param lifetimeSeconds       the requested lifetime in seconds, {@code 0} to remove the registration
     * @param displayName           the display name, the name of the player's stamp
     * @param stampNonce            the nonce of the player's stamp
     * @param fingerprint           the fingerprint of the mod the client runs
     * @param connection            the client's connection number, the same for all registrations of the connection
     * @param connectionStartMillis the epoch millisecond at which the client made the connection number
     * @throws IllegalArgumentException if the token, the lifetime or the name is out of range
     */
    public RegisterMessage(byte @NotNull [] token, int lifetimeSeconds, @NotNull String displayName, long stampNonce,
                           @NotNull ModFingerprint fingerprint, long connection, long connectionStartMillis) {
        if (token.length > MAXIMUM_TOKEN_LENGTH || lifetimeSeconds < 0 || lifetimeSeconds > MAXIMUM_LIFETIME_SECONDS ||
            WireWriter.getTextLength(displayName) > MAXIMUM_NAME_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The token length {0}, the lifetime {1} or the name {2} is out of range.",
                                                         token.length, lifetimeSeconds, displayName);
        }

        this.token           = token.clone();
        this.lifetimeSeconds = lifetimeSeconds;

        this.displayName = displayName;
        this.stampNonce  = stampNonce;
        this.fingerprint = fingerprint;

        this.connection            = connection;
        this.connectionStartMillis = connectionStartMillis;
    }

    /**
     * @return {@link MessageKind#REGISTER}
     */
    @Override
    public @NotNull MessageKind getKind() {
        return MessageKind.REGISTER;
    }

    /**
     * @return a copy of the address token, empty for a first registration
     */
    public byte @NotNull [] getToken() {
        return this.token.clone();
    }

    /**
     * @return the requested lifetime in seconds, {@code 0} to remove the registration
     */
    public int getLifetimeSeconds() {
        return this.lifetimeSeconds;
    }

    /**
     * @return the display name
     */
    public @NotNull String getDisplayName() {
        return this.displayName;
    }

    /**
     * @return the nonce of the player's stamp
     */
    public long getStampNonce() {
        return this.stampNonce;
    }

    /**
     * Returns the player's stamp the registration carries; it is not checked.
     *
     * @param sender the key of the registering player, the envelope's sender
     * @return the stamp of the sender's key, the display name and the stamp nonce
     */
    public @NotNull PlayerStamp getStamp(@NotNull IdentityKey sender) {
        return new PlayerStamp(sender, this.displayName, this.stampNonce);
    }

    /**
     * @return the fingerprint of the mod the client runs
     */
    public @NotNull ModFingerprint getFingerprint() {
        return this.fingerprint;
    }

    /**
     * @return the client's connection number
     */
    public long getConnection() {
        return this.connection;
    }

    /**
     * @return the epoch millisecond at which the client made the connection number
     */
    public long getConnectionStartMillis() {
        return this.connectionStartMillis;
    }


    /**
     * Writes the body fields.
     *
     * @param writer the writer
     */
    @Override
    public void writeBody(@NotNull WireWriter writer) {
        writer.writeUnsigned8(this.token.length);
        writer.writeBytes(this.token);
        writer.writeUnsigned16(this.lifetimeSeconds);
        writer.writeText(this.displayName, MAXIMUM_NAME_LENGTH);
        writer.writeInt64(this.stampNonce);
        this.fingerprint.write(writer);
        writer.writeInt64(this.connection);
        writer.writeInt64(this.connectionStartMillis);
        writer.writeUnsigned8(0);
    }
}

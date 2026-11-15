package org.fogofwarlabs.sc4j.beaconlink.protocol.community;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.coordinate.NetworkCoordinate;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerNameUtils;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.HomePeerListUtils;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.NodeEndpoint;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireReader;
import org.fogofwarlabs.sc4j.beaconlink.protocol.wire.WireWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Holds what a player's presence tells: the display name, the mod the player runs (its id, its version and a short digest of the whole mod
 * identity, so that others see whether they can play together), the status and the away text, the channel the player chats in, the X25519
 * key that whispers to the player are sealed with, the peers the player is registered at, through which whispers reach it, and the player's
 * settled network coordinate when it has one, from which the others estimate their round trip time to the player. One content is signed
 * into a presence record of every topic the player takes part in ({@link PresenceRecord}).
 * <p>
 * On the wire: the display name (at most {@value #MAXIMUM_NAME_LENGTH} UTF-8 bytes), the mod id (at most {@value #MAXIMUM_MOD_ID_LENGTH}),
 * the mod version (at most {@value #MAXIMUM_VERSION_LENGTH}), the identity digest ({@value #IDENTITY_DIGEST_LENGTH} bytes), the status
 * code (one byte), the away text (at most {@value #MAXIMUM_AWAY_TEXT_LENGTH} UTF-8 bytes, empty for none), the channel name (at most
 * {@value TopicKey#MAXIMUM_CHANNEL_NAME_LENGTH} UTF-8 bytes, empty for none), the whisper key ({@value #WHISPER_KEY_LENGTH} bytes), the home
 * peers ({@link HomePeerListUtils}) and the optional {@link NetworkCoordinate} (flag and coordinate).
 */
public final class PresenceContent {
    /**
     * The largest UTF-8 length of a display name.
     */
    public static final int MAXIMUM_NAME_LENGTH = PlayerNameUtils.MAXIMUM_LENGTH;
    /**
     * The largest UTF-8 length of a mod id.
     */
    public static final int MAXIMUM_MOD_ID_LENGTH = 48;
    /**
     * The largest UTF-8 length of a mod version.
     */
    public static final int MAXIMUM_VERSION_LENGTH = 24;
    /**
     * The largest UTF-8 length of an away text.
     */
    public static final int MAXIMUM_AWAY_TEXT_LENGTH = 32;
    /**
     * The length of the identity digest.
     */
    public static final int IDENTITY_DIGEST_LENGTH = 8;
    /**
     * The length of an X25519 whisper key.
     */
    public static final int WHISPER_KEY_LENGTH = 32;
    /**
     * The largest length of the wire form.
     */
    public static final int MAXIMUM_ENCODED_LENGTH = 1 + MAXIMUM_NAME_LENGTH + 1 + MAXIMUM_MOD_ID_LENGTH + 1 + MAXIMUM_VERSION_LENGTH +
                                                     IDENTITY_DIGEST_LENGTH + 1 + 1 + MAXIMUM_AWAY_TEXT_LENGTH +
                                                     1 + TopicKey.MAXIMUM_CHANNEL_NAME_LENGTH + WHISPER_KEY_LENGTH +
                                                     HomePeerListUtils.MAXIMUM_ENCODED_LENGTH + NetworkCoordinate.MAXIMUM_OPTIONAL_LENGTH;

    /**
     * Reads a content from the wire.
     *
     * @param reader the reader
     * @return the content, or {@code null} if the reader failed or a field is out of range
     */
    public static @Nullable PresenceContent read(@NotNull WireReader reader) {
        String displayName = reader.readText(MAXIMUM_NAME_LENGTH);
        String modId = reader.readText(MAXIMUM_MOD_ID_LENGTH);
        String modVersion = reader.readText(MAXIMUM_VERSION_LENGTH);
        byte[] identityDigest = reader.readBytes(IDENTITY_DIGEST_LENGTH);
        PresenceStatus status = PresenceStatus.find(reader.readUnsigned8());
        String awayText = reader.readText(MAXIMUM_AWAY_TEXT_LENGTH);
        String channel = reader.readText(TopicKey.MAXIMUM_CHANNEL_NAME_LENGTH);
        byte[] whisperKey = reader.readBytes(WHISPER_KEY_LENGTH);
        List<NodeEndpoint> homePeers = HomePeerListUtils.read(reader);
        NetworkCoordinate coordinate = NetworkCoordinate.readOptional(reader);

        if (status == null || homePeers == null || reader.isFailed()) {
            reader.fail();

            return null;
        }

        return new PresenceContent(displayName, modId, modVersion, identityDigest, status, awayText, channel, whisperKey, homePeers, coordinate);
    }


    private final @NotNull String displayName;
    private final @NotNull String modId;
    private final @NotNull String modVersion;
    private final byte @NotNull [] identityDigest;
    private final @NotNull PresenceStatus status;
    private final @NotNull String awayText;
    private final @NotNull String channel;
    private final byte @NotNull [] whisperKey;
    private final @NotNull List<@NotNull NodeEndpoint> homePeers;
    private final @Nullable NetworkCoordinate coordinate;

    /**
     * Creates a {@link PresenceContent} instance without an away text.
     *
     * @param displayName    the player's display name
     * @param modId          the id of the mod the player runs
     * @param modVersion     the version of the mod the player runs
     * @param identityDigest the first {@value #IDENTITY_DIGEST_LENGTH} bytes of a digest of the player's whole mod identity
     * @param status         the status
     * @param channel        the name of the channel the player chats in, empty for none
     * @param whisperKey     the X25519 public key whispers to the player are sealed with
     * @param homePeers      the peers the player is registered at, at most {@value HomePeerListUtils#MAXIMUM_COUNT}
     * @param coordinate     the player's settled network coordinate, or {@code null} if it has none yet
     * @throws IllegalArgumentException if a text is too long, a byte field has the wrong length or there are too many home peers
     */
    public PresenceContent(@NotNull String displayName, @NotNull String modId, @NotNull String modVersion, byte @NotNull [] identityDigest,
                           @NotNull PresenceStatus status, @NotNull String channel, byte @NotNull [] whisperKey,
                           @NotNull List<@NotNull NodeEndpoint> homePeers, @Nullable NetworkCoordinate coordinate) {
        this(displayName, modId, modVersion, identityDigest, status, "", channel, whisperKey, homePeers, coordinate);
    }

    /**
     * Creates a {@link PresenceContent} instance.
     *
     * @param displayName    the player's display name
     * @param modId          the id of the mod the player runs
     * @param modVersion     the version of the mod the player runs
     * @param identityDigest the first {@value #IDENTITY_DIGEST_LENGTH} bytes of a digest of the player's whole mod identity
     * @param status         the status
     * @param awayText       the text others see while the player is away, empty for none
     * @param channel        the name of the channel the player chats in, empty for none
     * @param whisperKey     the X25519 public key whispers to the player are sealed with
     * @param homePeers      the peers the player is registered at, at most {@value HomePeerListUtils#MAXIMUM_COUNT}
     * @param coordinate     the player's settled network coordinate, or {@code null} if it has none yet
     * @throws IllegalArgumentException if a text is too long, a byte field has the wrong length or there are too many home peers
     */
    public PresenceContent(@NotNull String displayName, @NotNull String modId, @NotNull String modVersion, byte @NotNull [] identityDigest,
                           @NotNull PresenceStatus status, @NotNull String awayText, @NotNull String channel, byte @NotNull [] whisperKey,
                           @NotNull List<@NotNull NodeEndpoint> homePeers, @Nullable NetworkCoordinate coordinate) {
        if (WireWriter.getTextLength(displayName) > MAXIMUM_NAME_LENGTH || WireWriter.getTextLength(modId) > MAXIMUM_MOD_ID_LENGTH ||
            WireWriter.getTextLength(modVersion) > MAXIMUM_VERSION_LENGTH || WireWriter.getTextLength(awayText) > MAXIMUM_AWAY_TEXT_LENGTH ||
            WireWriter.getTextLength(channel) > TopicKey.MAXIMUM_CHANNEL_NAME_LENGTH) {
            throw Exceptions.newIllegalArgumentException("A text of the presence of {0} ({1} {2}, channel {3}) is too long.",
                                                         displayName, modId, modVersion, channel);
        }

        if (identityDigest.length != IDENTITY_DIGEST_LENGTH || whisperKey.length != WHISPER_KEY_LENGTH) {
            throw Exceptions.newIllegalArgumentException("The identity digest has {0} bytes, not {1}, or the whisper key {2}, not {3}.",
                                                         identityDigest.length, IDENTITY_DIGEST_LENGTH, whisperKey.length, WHISPER_KEY_LENGTH);
        }

        this.displayName    = displayName;
        this.modId          = modId;
        this.modVersion     = modVersion;
        this.identityDigest = identityDigest.clone();
        this.status         = status;
        this.awayText       = awayText;
        this.channel        = channel;
        this.whisperKey     = whisperKey.clone();
        this.homePeers      = HomePeerListUtils.copy(homePeers);
        this.coordinate     = coordinate;
    }

    /**
     * @return the player's display name
     */
    public @NotNull String getDisplayName() {
        return this.displayName;
    }

    /**
     * @return the id of the mod the player runs
     */
    public @NotNull String getModId() {
        return this.modId;
    }

    /**
     * @return the version of the mod the player runs
     */
    public @NotNull String getModVersion() {
        return this.modVersion;
    }

    /**
     * @return a copy of the digest of the player's whole mod identity; equal digests mean the players can play together
     */
    public byte @NotNull [] getIdentityDigest() {
        return this.identityDigest.clone();
    }

    /**
     * @return the status
     */
    public @NotNull PresenceStatus getStatus() {
        return this.status;
    }

    /**
     * @return the text others see while the player is away, empty for none
     */
    public @NotNull String getAwayText() {
        return this.awayText;
    }

    /**
     * @return the name of the channel the player chats in, empty for none
     */
    public @NotNull String getChannel() {
        return this.channel;
    }

    /**
     * @return a copy of the X25519 public key whispers to the player are sealed with
     */
    public byte @NotNull [] getWhisperKey() {
        return this.whisperKey.clone();
    }

    /**
     * @return the peers the player is registered at, unmodifiable
     */
    public @NotNull List<@NotNull NodeEndpoint> getHomePeers() {
        return this.homePeers;
    }

    /**
     * @return the player's settled network coordinate, or {@code null} if it had none
     */
    public @Nullable NetworkCoordinate getCoordinate() {
        return this.coordinate;
    }


    /**
     * Writes the content.
     *
     * @param writer the writer
     */
    public void write(@NotNull WireWriter writer) {
        writer.writeText(this.displayName, MAXIMUM_NAME_LENGTH);
        writer.writeText(this.modId, MAXIMUM_MOD_ID_LENGTH);
        writer.writeText(this.modVersion, MAXIMUM_VERSION_LENGTH);
        writer.writeBytes(this.identityDigest);
        writer.writeUnsigned8(this.status.getCode());
        writer.writeText(this.awayText, MAXIMUM_AWAY_TEXT_LENGTH);
        writer.writeText(this.channel, TopicKey.MAXIMUM_CHANNEL_NAME_LENGTH);
        writer.writeBytes(this.whisperKey);
        HomePeerListUtils.write(writer, this.homePeers);
        NetworkCoordinate.writeOptional(writer, this.coordinate);
    }

    /**
     * @param other the other object
     * @return {@code true} if the other object is a content with the same fields, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof PresenceContent content && this.displayName.equals(content.displayName) && this.modId.equals(content.modId) &&
               this.modVersion.equals(content.modVersion) && Arrays.equals(this.identityDigest, content.identityDigest) &&
               this.status == content.status && this.awayText.equals(content.awayText) && this.channel.equals(content.channel) &&
               Arrays.equals(this.whisperKey, content.whisperKey) &&
               this.homePeers.equals(content.homePeers) && Objects.equals(this.coordinate, content.coordinate);
    }

    /**
     * @return the hash code of the name, the mod, the status and the channel
     */
    @Override
    public int hashCode() {
        return this.displayName.hashCode() ^ this.modId.hashCode() ^ this.status.hashCode() ^ this.channel.hashCode();
    }

    /**
     * @return the name, the mod, the status and the channel, for logs
     */
    @Override
    public @NotNull String toString() {
        String where = this.channel.isEmpty() ? "" : " in " + this.channel;

        return this.displayName + " (" + this.modId + " " + this.modVersion + ") " + this.status + where;
    }
}

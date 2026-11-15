package org.fogofwarlabs.sc4j.beaconlink.peer.account;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Holds one player account of an install as it was read: its folder {@code <install>/userdata/beaconlink/accounts/<id>/}, its player key
 * (the identity: the tag and the id come from it), its name with the player stamp that binds the name to the key, and when it was last
 * used. The name is fixed when the account is made: another name needs another stamp, and so another account. The folder keeps the key file
 * ({@value #KEY_FILE}), the account file with the name and the stamp ({@value #ACCOUNT_FILE}), the friends ({@value #FRIENDS_FILE}), the
 * ignored players ({@value #IGNORED_FILE}) and the lock file the processes that use the account hold ({@value #LOCK_FILE}).
 * <p>
 * The object does not change; {@link AccountStore} returns a new one after a change.
 */
public final class Account {
    /**
     * The name of the key file.
     */
    public static final @NotNull String KEY_FILE = "identity.key";
    /**
     * The name of the account file: the name, the player stamp and the time of the last use.
     */
    public static final @NotNull String ACCOUNT_FILE = "account.json";
    /**
     * The name of the friend list, a shared {@link PlayerListStore}.
     */
    public static final @NotNull String FRIENDS_FILE = "friends.json";
    /**
     * The most friends of an account.
     */
    public static final int MAXIMUM_FRIENDS = 16;
    /**
     * The name of the list of the players the account ignores (blocks), a shared {@link PlayerListStore}.
     */
    public static final @NotNull String IGNORED_FILE = "blocked.json";
    /**
     * The most players an account ignores.
     */
    public static final int MAXIMUM_IGNORED = 256;
    /**
     * The name of the file every process that uses the account holds with a shared lock ({@link AccountLock}).
     */
    public static final @NotNull String LOCK_FILE = "account.lock";

    private static final int ID_BYTES = 8;
    private static final @NotNull String ID_DIGEST = "SHA-256";

    /**
     * Creates the id of a player key: the first sixteen hexadecimal digits of the SHA-256 hash of the key, the name of its account folder.
     * The key's tag ({@link IdentityKey#getTag()}) is the start of the id.
     *
     * @param key the player key
     * @return the id
     */
    public static @NotNull String createId(@NotNull IdentityKey key) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(ID_DIGEST).digest(key.getBytes()), 0, ID_BYTES);
        } catch (NoSuchAlgorithmException exception) {
            throw Exceptions.newIllegalStateException(exception, "The JDK has no {0} digest.", ID_DIGEST);
        }
    }


    private final @NotNull String id;
    private final @NotNull Path directory;
    private final @NotNull IdentityKey key;

    private final @NotNull PlayerStamp stamp;
    private final int stampDifficulty;
    private final long lastUsedMillis;

    /**
     * Creates a {@link Account} instance; nothing is read or written.
     *
     * @param directory       the account folder
     * @param stamp           the player stamp: the player key and the name it binds to the key
     * @param stampDifficulty the difficulty the stamp meets
     * @param lastUsedMillis  the epoch millisecond of the last use, {@code 0} if never used
     */
    Account(@NotNull Path directory, @NotNull PlayerStamp stamp, int stampDifficulty, long lastUsedMillis) {
        this.id        = createId(stamp.getKey());
        this.directory = directory;
        this.key       = stamp.getKey();

        this.stamp           = stamp;
        this.stampDifficulty = stampDifficulty;
        this.lastUsedMillis  = lastUsedMillis;
    }

    /**
     * @return the account id, the name of its folder: the first sixteen hexadecimal digits of the SHA-256 hash of the key
     */
    public @NotNull String getId() {
        return this.id;
    }

    /**
     * @return the account folder
     */
    public @NotNull Path getDirectory() {
        return this.directory;
    }

    /**
     * @return the player key
     */
    public @NotNull IdentityKey getKey() {
        return this.key;
    }

    /**
     * @return the name, fixed when the account was made
     */
    public @NotNull String getName() {
        return this.stamp.getName();
    }

    /**
     * @return the tag: the first six hexadecimal digits of the SHA-256 hash of the key, the same whatever the name
     */
    public @NotNull String getTag() {
        return this.key.getTag();
    }

    /**
     * @return the name and the tag, {@code Name#abcd12}
     */
    public @NotNull String getTaggedName() {
        return this.getName() + PlayerNameUtils.TAG_SEPARATOR + this.getTag();
    }

    /**
     * @return the player stamp that binds the name to the key
     */
    public @NotNull PlayerStamp getStamp() {
        return this.stamp;
    }

    /**
     * @return the number of leading zero bits the stamp hash has at least
     */
    public int getStampDifficulty() {
        return this.stampDifficulty;
    }

    /**
     * @return the epoch millisecond of the last use, {@code 0} if the account was never used
     */
    public long getLastUsedMillis() {
        return this.lastUsedMillis;
    }


    /**
     * Reads the account's key pair.
     *
     * @return the key pair
     * @throws IOException if the key file cannot be read, is broken, or holds another key than the account's
     */
    public @NotNull IdentityKeyPair readKeyPair() throws IOException {
        IdentityKeyPair keyPair = IdentityKeyFileUtils.read(this.directory.resolve(KEY_FILE));

        if (!keyPair.getPublicKey().equals(this.key)) {
            throw Exceptions.newIOException("The key file of the account {0} holds another key.", this.id);
        }

        return keyPair;
    }

    /**
     * Opens the account's friends ({@value #FRIENDS_FILE}) for one user: a list the launcher and every game of the account share, at most
     * {@value #MAXIMUM_FRIENDS} players.
     *
     * @return the user's view of the friends
     */
    public @NotNull PlayerListStore openFriends() {
        return PlayerListStore.open(this.directory.resolve(FRIENDS_FILE), MAXIMUM_FRIENDS);
    }

    /**
     * Opens the players the account ignores ({@value #IGNORED_FILE}) for one user: a list the launcher and every game of the account share,
     * at most {@value #MAXIMUM_IGNORED} players.
     *
     * @return the user's view of the ignored players
     */
    public @NotNull PlayerListStore openIgnored() {
        return PlayerListStore.open(this.directory.resolve(IGNORED_FILE), MAXIMUM_IGNORED);
    }


    /**
     * @return the tagged name, for logs
     */
    @Override
    public @NotNull String toString() {
        return this.getTaggedName();
    }
}

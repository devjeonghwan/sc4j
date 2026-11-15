package org.fogofwarlabs.sc4j.beaconlink.client;

import org.fogofwarlabs.sc4j.beaconlink.peer.account.Account;
import org.fogofwarlabs.sc4j.beaconlink.peer.account.PlayerListStore;
import org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.KnownNodes;
import org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.PeerStore;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerStamp;
import org.fogofwarlabs.sc4j.engine.net.beaconlink.BeaconLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds what the Beacon.Link sessions of one run use, as the run's command line names it and nothing else: the account the player chose
 * ({@link Account}: its key, stamp, shared friend list and shared ignore list, {@link PlayerListStore}s) or none for a guest, the seeds the
 * sessions contact first, and the peers file that every process of the computer shares ({@link PeerStore}). Creating it reads and writes
 * nothing.
 * <p>
 * A run without an account plays as a guest named {@value BeaconLink#GUEST_NAME}: its player key, stamp, friends and ignored players are made
 * at the first use and kept in memory for the run only. A run without a peers file neither reads nor keeps learned peers.
 */
public final class ClientSetup {
    /**
     * Creates a {@link ClientSetup} instance; nothing is read or created.
     *
     * @param account   the account of the run, or {@code null} for a guest
     * @param seeds     the seeds in list order
     * @param peersFile the shared peers file, or {@code null} to neither read nor keep learned peers
     * @return the created {@link ClientSetup} instance
     */
    public static @NotNull ClientSetup of(@Nullable Account account, @NotNull List<@NotNull ConfiguredNode> seeds, @Nullable Path peersFile) {
        return new ClientSetup(account, seeds, peersFile == null ? null : peersFile.toAbsolutePath().normalize());
    }

    private static @NotNull List<@NotNull IdentityKey> changeGuestList(@NotNull List<@NotNull IdentityKey> list, @NotNull IdentityKey player,
                                                                       boolean added) {
        List<IdentityKey> changed = new ArrayList<>(list);

        changed.remove(player);

        if (added) {
            changed.add(player);
        }

        return List.copyOf(changed);
    }


    private final @Nullable Account account;
    private final @NotNull List<@NotNull ConfiguredNode> seeds;
    private final @Nullable Path peersFile;

    private @Nullable PlayerListStore friends;
    private @Nullable PlayerListStore ignored;
    private @Nullable IdentityKeyPair guestKey;
    private @Nullable PlayerStamp guestStamp;
    private @NotNull List<@NotNull IdentityKey> guestFriends;
    private @NotNull List<@NotNull IdentityKey> guestIgnored;

    private ClientSetup(@Nullable Account account, @NotNull List<@NotNull ConfiguredNode> seeds, @Nullable Path peersFile) {
        this.account   = account;
        this.seeds     = List.copyOf(seeds);
        this.peersFile = peersFile;

        this.friends      = null;
        this.ignored      = null;
        this.guestKey     = null;
        this.guestStamp   = null;
        this.guestFriends = List.of();
        this.guestIgnored = List.of();
    }

    /**
     * @return the account of the run, or {@code null} for a guest
     */
    public @Nullable Account getAccount() {
        return this.account;
    }

    /**
     * @return the seeds in list order
     */
    public @NotNull List<@NotNull ConfiguredNode> getSeeds() {
        return this.seeds;
    }


    /**
     * @return the absolute shared peers file, or {@code null} if the run neither reads nor keeps learned peers
     */
    public @Nullable Path getPeersFile() {
        return this.peersFile;
    }

    /**
     * Opens the nodes the sessions know: the seeds and the learned peers of the shared peers file.
     *
     * @return the known nodes
     */
    public @NotNull KnownNodes openKnownNodes() {
        return KnownNodes.open(this.seeds, this.peersFile);
    }

    /**
     * Reads the player's key pair: the account's, or for a guest a key pair made at the first call.
     *
     * @param random the source of a guest's private key
     * @return the player's key pair
     * @throws IOException if the account's key file cannot be read or is broken
     */
    public @NotNull IdentityKeyPair readKey(@NotNull SecureRandom random) throws IOException {
        Account chosen = this.account;

        if (chosen != null) {
            return chosen.readKeyPair();
        }

        IdentityKeyPair guest = this.guestKey;

        if (guest == null) {
            guest         = IdentityKeyPair.generate(random);
            this.guestKey = guest;
        }

        return guest;
    }

    /**
     * Reads the player's stamp, which names the player: the account's, made with the account, or for a guest a stamp of the name
     * {@value BeaconLink#GUEST_NAME} found at the first call (about two to the power of the difficulty hashes) and kept in memory.
     *
     * @param keyPair    the player's key pair ({@link #readKey})
     * @param difficulty the stamp difficulty of a guest's stamp
     * @return the stamp of the player key
     */
    public @NotNull PlayerStamp readStamp(@NotNull IdentityKeyPair keyPair, int difficulty) {
        Account chosen = this.account;

        if (chosen != null) {
            return chosen.getStamp();
        }

        PlayerStamp guest = this.guestStamp;

        if (guest == null || !guest.getKey().equals(keyPair.getPublicKey()) || !guest.isValid(difficulty)) {
            guest           = PlayerStamp.solve(keyPair.getPublicKey(), BeaconLink.GUEST_NAME, difficulty);
            this.guestStamp = guest;
        }

        return guest;
    }

    /**
     * Reads the player's friends: the account's shared friend list as this run last read it, or a guest's of this run.
     *
     * @return the friends' player keys, the oldest addition first
     */
    public @NotNull List<@NotNull IdentityKey> readFriends() {
        PlayerListStore shared = this.getFriendList();

        return shared != null ? shared.getPlayers() : new ArrayList<>(this.guestFriends);
    }

    /**
     * Adds a friend: to the account's shared friend list at once, or for a guest in memory.
     *
     * @param friend    the friend's player key
     * @param nowMillis the current time
     */
    public void addFriend(@NotNull IdentityKey friend, long nowMillis) {
        PlayerListStore shared = this.getFriendList();

        if (shared != null) {
            shared.add(friend, nowMillis);
        } else {
            this.guestFriends = changeGuestList(this.guestFriends, friend, true);
        }
    }

    /**
     * Removes a friend: from the account's shared friend list at once, or for a guest in memory.
     *
     * @param friend    the friend's player key
     * @param nowMillis the current time
     */
    public void removeFriend(@NotNull IdentityKey friend, long nowMillis) {
        PlayerListStore shared = this.getFriendList();

        if (shared != null) {
            shared.remove(friend, nowMillis);
        } else {
            this.guestFriends = changeGuestList(this.guestFriends, friend, false);
        }
    }

    /**
     * Reads the account's shared friend list again when it is due ({@link PlayerListStore#reloadWhenDue}), so that the run follows the
     * changes the account's other processes make.
     *
     * @param nowMillis the current time
     * @return {@code true} if the friends changed, {@code false} otherwise or for a guest
     */
    public boolean reloadFriendsWhenDue(long nowMillis) {
        PlayerListStore shared = this.getFriendList();

        return shared != null && shared.reloadWhenDue(nowMillis);
    }

    private @Nullable PlayerListStore getFriendList() {
        Account chosen = this.account;
        PlayerListStore opened = this.friends;

        if (chosen != null && opened == null) {
            opened       = chosen.openFriends();
            this.friends = opened;
        }

        return opened;
    }

    /**
     * Reads the players the account ignores: the account's shared ignore list as this run last read it, or a guest's of this run.
     *
     * @return the ignored players' keys, the oldest addition first
     */
    public @NotNull List<@NotNull IdentityKey> readIgnored() {
        PlayerListStore shared = this.getIgnoreList();

        return shared != null ? shared.getPlayers() : new ArrayList<>(this.guestIgnored);
    }

    /**
     * Ignores a player: in the account's shared ignore list at once, or for a guest in memory.
     *
     * @param player    the player's key
     * @param nowMillis the current time
     */
    public void addIgnored(@NotNull IdentityKey player, long nowMillis) {
        PlayerListStore shared = this.getIgnoreList();

        if (shared != null) {
            shared.add(player, nowMillis);
        } else {
            this.guestIgnored = changeGuestList(this.guestIgnored, player, true);
        }
    }

    /**
     * Stops ignoring a player: in the account's shared ignore list at once, or for a guest in memory.
     *
     * @param player    the player's key
     * @param nowMillis the current time
     */
    public void removeIgnored(@NotNull IdentityKey player, long nowMillis) {
        PlayerListStore shared = this.getIgnoreList();

        if (shared != null) {
            shared.remove(player, nowMillis);
        } else {
            this.guestIgnored = changeGuestList(this.guestIgnored, player, false);
        }
    }

    /**
     * Reads the account's shared ignore list again when it is due ({@link PlayerListStore#reloadWhenDue}), so that the run follows the
     * changes the account's other processes make.
     *
     * @param nowMillis the current time
     * @return {@code true} if the ignored players changed, {@code false} otherwise or for a guest
     */
    public boolean reloadIgnoredWhenDue(long nowMillis) {
        PlayerListStore shared = this.getIgnoreList();

        return shared != null && shared.reloadWhenDue(nowMillis);
    }

    private @Nullable PlayerListStore getIgnoreList() {
        Account chosen = this.account;
        PlayerListStore opened = this.ignored;

        if (chosen != null && opened == null) {
            opened       = chosen.openIgnored();
            this.ignored = opened;
        }

        return opened;
    }
}

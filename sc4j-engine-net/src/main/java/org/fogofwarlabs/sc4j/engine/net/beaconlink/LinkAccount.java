package org.fogofwarlabs.sc4j.engine.net.beaconlink;

import org.jetbrains.annotations.NotNull;

/**
 * Describes the account a run plays with: the name the player chose for it when the account was made (it never changes), the player of its
 * key, and an id that names the account's files. Other players see the account as {@code Name#tag} ({@link #getTaggedName()}); a mod shows
 * that name wherever it names the player.
 */
public final class LinkAccount {
    private static final char TAG_SEPARATOR = '#';

    private final @NotNull String id;
    private final @NotNull String name;
    private final @NotNull LinkPlayer player;

    /**
     * Creates a {@link LinkAccount} instance.
     *
     * @param id     the account id: lowercase hexadecimal digits that stay the same while the key does, usable as a file name
     * @param name   the account name
     * @param player the player of the account's key
     */
    public LinkAccount(@NotNull String id, @NotNull String name, @NotNull LinkPlayer player) {
        this.id     = id;
        this.name   = name;
        this.player = player;
    }

    /**
     * @return the account id: lowercase hexadecimal digits that stay the same while the key does, usable as a file name
     */
    public @NotNull String getId() {
        return this.id;
    }

    /**
     * @return the account name, the display name of the account's sessions
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the player of the account's key
     */
    public @NotNull LinkPlayer getPlayer() {
        return this.player;
    }

    /**
     * @return the name and the player's tag, {@code Name#abcd12}
     */
    public @NotNull String getTaggedName() {
        return this.name + TAG_SEPARATOR + this.player.getTag();
    }
}

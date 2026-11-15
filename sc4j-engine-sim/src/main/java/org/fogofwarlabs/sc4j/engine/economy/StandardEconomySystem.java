package org.fogofwarlabs.sc4j.engine.economy;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Holds the resources and supply of every player in flat arrays addressed by the player, kind and faction indices: the engine
 * default {@link EconomySystem}. A mod registers it under {@link EconomySystem#KEY}; the arrays are sized when it is
 * attached, from the game's player count and the content's resource kind and faction counts, and start at {@code 0}.
 * {@snippet :
 * builder.useSystem(EconomySystem.KEY, context -> new StandardEconomySystem());
 *}
 * <p>
 * Determinism contract: the system mixes the amounts, then the provided and the used supply into the state hash, each in
 * player order, then kind or faction order.
 */
public final class StandardEconomySystem implements EconomySystem, StateHashContributor, SnapshotContributor {
    private int playerCount;
    private int resourceKindCount;
    private int factionCount;

    private int @NotNull [] amounts;
    private int @NotNull [] supplyProvided;
    private int @NotNull [] supplyUsed;

    /**
     * Creates a {@link StandardEconomySystem} instance; it holds no value until it is attached.
     */
    public StandardEconomySystem() {
        this.playerCount       = 0;
        this.resourceKindCount = 0;
        this.factionCount      = 0;

        this.amounts        = new int[0];
        this.supplyProvided = new int[0];
        this.supplyUsed     = new int[0];
    }

    /**
     * Resolves what this system uses from the game. Called once, after every system exists.
     *
     * @param game the game
     */
    @Override
    public void attach(@NotNull Game game) {
        ContentDefinition content = game.getContent().getDefinition();

        this.playerCount       = game.getPlayerCount();
        this.resourceKindCount = content.getResourceKinds().getCount();
        this.factionCount      = content.getFactions().getCount();

        this.amounts        = new int[this.playerCount * this.resourceKindCount];
        this.supplyProvided = new int[this.playerCount * this.factionCount];
        this.supplyUsed     = new int[this.playerCount * this.factionCount];
    }

    /**
     * Returns the amount of a resource kind a player holds.
     *
     * @param player       the player
     * @param resourceKind the resource kind
     * @return the amount
     */
    @Override
    public int getAmount(@NotNull Player player, @NotNull ResourceKind resourceKind) {
        return this.amounts[this.getAmountIndex(player, resourceKind)];
    }

    /**
     * Sets the amount of a resource kind a player holds.
     *
     * @param player       the player
     * @param resourceKind the resource kind
     * @param amount       the amount
     */
    @Override
    public void setAmount(@NotNull Player player, @NotNull ResourceKind resourceKind, int amount) {
        this.amounts[this.getAmountIndex(player, resourceKind)] = amount;
    }

    /**
     * Checks whether a player holds every amount of a cost.
     *
     * @param player the player
     * @param cost   the cost
     * @return {@code true} if the player can pay the cost, {@code false} otherwise
     */
    @Override
    public boolean canAfford(@NotNull Player player, @NotNull ResourceCost cost) {
        for (int index = 0; index < cost.getCount(); index++) {
            if (this.getAmount(player, cost.getKind(index)) < cost.getAmount(index)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Takes a cost from a player if the player can pay all of it.
     *
     * @param player the player
     * @param cost   the cost
     * @return {@code true} if the cost was taken, {@code false} if nothing changed
     */
    @Override
    public boolean spend(@NotNull Player player, @NotNull ResourceCost cost) {
        if (!this.canAfford(player, cost)) {
            return false;
        }

        for (int index = 0; index < cost.getCount(); index++) {
            this.amounts[this.getAmountIndex(player, cost.getKind(index))] -= cost.getAmount(index);
        }

        return true;
    }

    /**
     * Gives a cost back to a player, for example when production is cancelled.
     *
     * @param player the player
     * @param cost   the cost
     */
    @Override
    public void refund(@NotNull Player player, @NotNull ResourceCost cost) {
        for (int index = 0; index < cost.getCount(); index++) {
            this.amounts[this.getAmountIndex(player, cost.getKind(index))] += cost.getAmount(index);
        }
    }

    /**
     * Returns the supply the units of a faction provide to a player.
     *
     * @param player  the player
     * @param faction the faction
     * @return the provided supply
     */
    @Override
    public int getSupplyProvided(@NotNull Player player, @NotNull Faction faction) {
        return this.supplyProvided[this.getSupplyIndex(player, faction)];
    }

    /**
     * Returns the supply the units of a faction use for a player.
     *
     * @param player  the player
     * @param faction the faction
     * @return the used supply
     */
    @Override
    public int getSupplyUsed(@NotNull Player player, @NotNull Faction faction) {
        return this.supplyUsed[this.getSupplyIndex(player, faction)];
    }

    /**
     * Changes the supply the units of a faction provide to a player.
     *
     * @param player  the player
     * @param faction the faction
     * @param change  the change, negative when a provider is lost
     */
    @Override
    public void addSupplyProvided(@NotNull Player player, @NotNull Faction faction, int change) {
        this.supplyProvided[this.getSupplyIndex(player, faction)] += change;
    }

    /**
     * Changes the supply the units of a faction use for a player.
     *
     * @param player  the player
     * @param faction the faction
     * @param change  the change, negative when a unit is lost
     */
    @Override
    public void addSupplyUsed(@NotNull Player player, @NotNull Faction faction, int change) {
        this.supplyUsed[this.getSupplyIndex(player, faction)] += change;
    }

    private int getAmountIndex(@NotNull Player player, @NotNull ResourceKind resourceKind) {
        int playerIndex = player.getIndex();
        int kindIndex = resourceKind.getIndex();

        if (playerIndex < 0 || playerIndex >= this.playerCount || kindIndex < 0 || kindIndex >= this.resourceKindCount) {
            throw Exceptions.newIndexOutOfBoundsException("No resource {0} of {1} in this game.", resourceKind, player);
        }

        return playerIndex * this.resourceKindCount + kindIndex;
    }

    private int getSupplyIndex(@NotNull Player player, @NotNull Faction faction) {
        int playerIndex = player.getIndex();
        int factionIndex = faction.getIndex();

        if (playerIndex < 0 || playerIndex >= this.playerCount || factionIndex < 0 || factionIndex >= this.factionCount) {
            throw Exceptions.newIndexOutOfBoundsException("No supply of faction {0} of {1} in this game.", faction, player);
        }

        return playerIndex * this.factionCount + factionIndex;
    }

    /**
     * Mixes this part of the simulation state into the hash: the amounts, the provided and the used supply.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        hash.mixArray(this.amounts);
        hash.mixArray(this.supplyProvided);
        hash.mixArray(this.supplyUsed);
    }

    /**
     * Writes this part of the simulation state: the resource amounts, then the supply provided and used.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInts(this.amounts);
        writer.writeInts(this.supplyProvided);
        writer.writeInts(this.supplyUsed);
    }

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or holds another number of values
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readInts(this.amounts);
        reader.readInts(this.supplyProvided);
        reader.readInts(this.supplyUsed);
    }
}

package org.fogofwarlabs.sc4j.engine.economy;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Holds what each player owns and may spend: the amount of every resource kind of the content, and the supply each faction
 * provides and uses. Production, harvesting, triggers and computer players read and change it here.
 * <p>
 * Resource kinds and factions are the identities registered in the game content.
 * <p>
 * Determinism contract: every operation changes only the named player's values, draws no random value and allocates
 * nothing; {@link #spend} changes all amounts of a cost or none. An implementation that holds the values mixes them into
 * the state hash in player order, then kind order.
 */
public interface EconomySystem extends GameSystem {
    /**
     * The key of the economy system.
     */
    @NotNull SystemKey<EconomySystem> KEY = SystemKey.of("economy", EconomySystem.class);

    /**
     * Returns the amount of a resource kind a player holds.
     *
     * @param player       the player
     * @param resourceKind the resource kind
     * @return the amount
     */
    int getAmount(@NotNull Player player, @NotNull ResourceKind resourceKind);

    /**
     * Sets the amount of a resource kind a player holds.
     *
     * @param player       the player
     * @param resourceKind the resource kind
     * @param amount       the amount
     */
    void setAmount(@NotNull Player player, @NotNull ResourceKind resourceKind, int amount);

    /**
     * Checks whether a player holds every amount of a cost.
     *
     * @param player the player
     * @param cost   the cost
     * @return {@code true} if the player can pay the cost, {@code false} otherwise
     */
    boolean canAfford(@NotNull Player player, @NotNull ResourceCost cost);

    /**
     * Takes a cost from a player if the player can pay all of it.
     *
     * @param player the player
     * @param cost   the cost
     * @return {@code true} if the cost was taken, {@code false} if nothing changed
     */
    boolean spend(@NotNull Player player, @NotNull ResourceCost cost);

    /**
     * Gives a cost back to a player, for example when production is cancelled.
     *
     * @param player the player
     * @param cost   the cost
     */
    void refund(@NotNull Player player, @NotNull ResourceCost cost);

    /**
     * Returns the supply the units of a faction provide to a player.
     *
     * @param player  the player
     * @param faction the faction
     * @return the provided supply
     */
    int getSupplyProvided(@NotNull Player player, @NotNull Faction faction);

    /**
     * Returns the supply the units of a faction use for a player.
     *
     * @param player  the player
     * @param faction the faction
     * @return the used supply
     */
    int getSupplyUsed(@NotNull Player player, @NotNull Faction faction);

    /**
     * Changes the supply the units of a faction provide to a player.
     *
     * @param player  the player
     * @param faction the faction
     * @param change  the change, negative when a provider is lost
     */
    void addSupplyProvided(@NotNull Player player, @NotNull Faction faction, int change);

    /**
     * Changes the supply the units of a faction use for a player.
     *
     * @param player  the player
     * @param faction the faction
     * @param change  the change, negative when a unit is lost
     */
    void addSupplyUsed(@NotNull Player player, @NotNull Faction faction, int change);
}

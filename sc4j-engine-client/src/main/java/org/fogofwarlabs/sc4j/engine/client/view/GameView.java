package org.fogofwarlabs.sc4j.engine.client.view;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Shows the client what it may see of a running game: the frame, the players, the map, the units and what a player sees of them, and the
 * players' resources. The view is read only; the client changes the game only through commands
 * ({@link org.fogofwarlabs.sc4j.engine.command.CommandSink}).
 * <p>
 * Identities cross the view as objects: units and players are the game's own {@link Unit} and {@link Player} objects, which name them in
 * commands; their state is read through {@link #getUnitView}, never changed. Positions and sizes are world pixels.
 */
public interface GameView {
    /**
     * @return the number of frames that advanced the game
     */
    int getFrame();

    /**
     * @return the players of the game in slot order
     */
    @NotNull List<@NotNull Player> getPlayers();

    /**
     * @return the player this client plays, or {@code null} for an observer or a replay viewer
     */
    @Nullable Player getLocalPlayer();

    /**
     * @return the terrain of the map
     */
    @NotNull TerrainGrid getTerrain();

    /**
     * @return the map width in world pixels
     */
    default int getMapWidth() {
        return this.getTerrain().getTileGrid().getWorldWidth();
    }

    /**
     * @return the map height in world pixels
     */
    default int getMapHeight() {
        return this.getTerrain().getTileGrid().getWorldHeight();
    }

    /**
     * Returns the units of the game in pool order. A view may refill one list at each call, so the list is read before the next call.
     *
     * @return the units at the time of the call, a read-only list
     */
    @NotNull List<@NotNull Unit> getUnits();

    /**
     * Returns the read-only state of a unit.
     *
     * @param unit the unit
     * @return the unit's view
     */
    @NotNull UnitView getUnitView(@NotNull Unit unit);

    /**
     * Returns the projectiles in flight in pool order, for their pictures. A view may refill one list at each call, so the list is read
     * before the next call. The default has none.
     *
     * @return the projectiles at the time of the call, a read-only list
     */
    default @NotNull List<@NotNull ProjectileView> getProjectiles() {
        return List.of();
    }

    /**
     * Finds the placed units whose bounds overlap a world rectangle, edges inclusive, in pool order.
     *
     * @param left   the left edge
     * @param top    the top edge
     * @param right  the right edge
     * @param bottom the bottom edge
     * @param result the collection the found units are added to
     */
    void findUnits(int left, int top, int right, int bottom, @NotNull Collection<@NotNull Unit> result);

    /**
     * Finds the unit shown at a world position: of the placed units whose bounds hold the position, the one with the lowest bottom edge
     * (drawn last), the first in pool order among equals.
     *
     * @param x the world x
     * @param y the world y
     * @return the unit, or {@code null} if no unit holds the position
     */
    @Nullable Unit findUnitAt(int x, int y);

    /**
     * Checks whether a player currently sees a world position.
     *
     * @param player the player
     * @param x      the world x
     * @param y      the world y
     * @return {@code true} if the player sees the position, {@code false} otherwise
     */
    boolean isVisible(@NotNull Player player, int x, int y);

    /**
     * Checks whether a player has ever seen a world position.
     *
     * @param player the player
     * @param x      the world x
     * @param y      the world y
     * @return {@code true} if the position is explored, {@code false} otherwise
     */
    boolean isExplored(@NotNull Player player, int x, int y);

    /**
     * Checks whether a player sees a unit.
     *
     * @param player the player
     * @param unit   the unit
     * @return {@code true} if the player sees the unit, {@code false} otherwise
     */
    boolean isUnitVisible(@NotNull Player player, @NotNull Unit unit);

    /**
     * @return the resource kinds of the game in their order
     */
    @NotNull List<@NotNull ResourceKind> getResourceKinds();

    /**
     * Returns the amount of a resource a player holds.
     *
     * @param player       the player
     * @param resourceKind the resource kind
     * @return the amount
     */
    int getResourceAmount(@NotNull Player player, @NotNull ResourceKind resourceKind);

    /**
     * Returns the faction a player plays.
     *
     * @param player the player
     * @return the faction, or {@code null} for a player without faction (for example neutral)
     */
    @Nullable Faction getFaction(@NotNull Player player);

    /**
     * Returns the supply a player's units of a faction use.
     *
     * @param player  the player
     * @param faction the faction
     * @return the used supply
     */
    int getSupplyUsed(@NotNull Player player, @NotNull Faction faction);

    /**
     * Returns the supply a player's units of a faction provide.
     *
     * @param player  the player
     * @param faction the faction
     * @return the provided supply
     */
    int getSupplyProvided(@NotNull Player player, @NotNull Faction faction);

    /**
     * Returns the team colour of a player.
     *
     * @param player the player
     * @return the colour, packed {@code 0xRRGGBB}
     */
    int getTeamColor(@NotNull Player player);
}

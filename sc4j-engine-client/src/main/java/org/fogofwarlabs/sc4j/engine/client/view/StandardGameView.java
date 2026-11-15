package org.fogofwarlabs.sc4j.engine.client.view;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.entity.Projectile;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.PlayerSlot;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.projectile.ProjectileSystem;
import org.fogofwarlabs.sc4j.engine.spatial.UnitBounds;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Shows an engine {@link Game} to the client: the engine default {@link GameView}. Units come from the game's unit pool, visibility from its
 * {@link org.fogofwarlabs.sc4j.engine.visibility.VisibilitySystem}, resources and supply from its
 * {@link org.fogofwarlabs.sc4j.engine.economy.EconomySystem}; the mod supplies the unit bounds, the terrain, the resource kinds and the team
 * colours by colour index. A player's faction and colour come from the setup's slot of the same index; a player without setup slot has no faction and
 * the colour of its own index.
 * <p>
 * The view keeps one {@link StandardUnitView} per pool slot, created on first use, so reading units every frame allocates nothing.
 */
public final class StandardGameView implements GameView {
    private final @NotNull Game game;
    private final @Nullable Player localPlayer;
    private final @NotNull TerrainGrid terrain;
    private final @NotNull UnitBounds<Unit> bounds;
    private final int fractionBits;
    private final @NotNull List<@NotNull ResourceKind> resourceKinds;
    private final int @NotNull [] teamColors;

    private final @Nullable Faction @NotNull [] factions;
    private final int @NotNull [] playerColors;
    private final @NotNull List<@NotNull Player> players;

    private final @Nullable StandardUnitView @NotNull [] unitViews;
    private final @NotNull List<@NotNull Unit> units;
    private final @NotNull List<@NotNull Unit> unitList;

    private final int projectileDirectionCount;
    private final @Nullable StandardProjectileView @NotNull [] projectileViews;
    private final @NotNull List<@NotNull ProjectileView> projectiles;
    private final @NotNull List<@NotNull ProjectileView> projectileList;

    /**
     * Creates a {@link StandardGameView} instance.
     *
     * @param game          the game
     * @param localPlayer   the player this client plays, or {@code null} for an observer or a replay viewer
     * @param terrain       the terrain of the game's map
     * @param bounds        the bounds of the game's units, the same the game's systems use
     * @param fractionBits  the fraction bits of the units' hit points and damage layer points (the combat system's fixed-point unit)
     * @param resourceKinds the resource kinds of the game in their order, copied
     * @param teamColors    the team colours by colour index, packed {@code 0xRRGGBB}, copied
     * @throws IllegalArgumentException if no team colour is given
     */
    public StandardGameView(@NotNull Game game,
                            @Nullable Player localPlayer,
                            @NotNull TerrainGrid terrain,
                            @NotNull UnitBounds<Unit> bounds,
                            int fractionBits,
                            @NotNull List<@NotNull ResourceKind> resourceKinds,
                            int @NotNull [] teamColors) {
        if (teamColors.length == 0) {
            throw Exceptions.newIllegalArgumentException("A game view needs at least one team colour.");
        }

        this.game          = game;
        this.localPlayer   = localPlayer;
        this.terrain       = terrain;
        this.bounds        = bounds;
        this.fractionBits  = fractionBits;
        this.resourceKinds = List.copyOf(resourceKinds);
        this.teamColors    = teamColors.clone();

        List<Player> gamePlayers = new ArrayList<>(game.getPlayerCount());

        this.factions     = new Faction[game.getPlayerCount()];
        this.playerColors = new int[game.getPlayerCount()];

        for (int index = 0; index < game.getPlayerCount(); index++) {
            gamePlayers.add(game.getPlayer(index));
            this.playerColors[index] = index;
        }

        for (PlayerSlot slot : game.getPlayers()) {
            int index = slot.getSlotIndex();

            if (index >= 0 && index < this.factions.length) {
                this.factions[index]     = slot.getFaction();
                this.playerColors[index] = slot.getColorIndex();
            }
        }

        this.players = List.copyOf(gamePlayers);

        this.unitViews = new StandardUnitView[game.getUnits().getCapacity() + 1];
        this.units     = new ArrayList<>();
        this.unitList  = Collections.unmodifiableList(this.units);

        ProjectileSystem projectileSystem = game.findModSystem(ProjectileSystem.KEY);

        this.projectileDirectionCount = projectileSystem != null ? projectileSystem.getDirectionCount() : WorldProjection.DIRECTION_COUNT;
        this.projectileViews          = new StandardProjectileView[game.getProjectiles().getCapacity() + 1];
        this.projectiles              = new ArrayList<>();
        this.projectileList           = Collections.unmodifiableList(this.projectiles);
    }

    /**
     * @return the number of frames that advanced the game
     */
    @Override
    public int getFrame() {
        return this.game.getClock().getFrame();
    }

    /**
     * @return the players of the game in slot order
     */
    @Override
    public @NotNull List<@NotNull Player> getPlayers() {
        return this.players;
    }

    /**
     * @return the player this client plays, or {@code null} for an observer or a replay viewer
     */
    @Override
    public @Nullable Player getLocalPlayer() {
        return this.localPlayer;
    }

    /**
     * @return the terrain of the map
     */
    @Override
    public @NotNull TerrainGrid getTerrain() {
        return this.terrain;
    }

    /**
     * Returns the units of the game in pool order. The view refills one list at each call, so reading the units every frame allocates nothing.
     *
     * @return the units at the time of the call, a read-only list the next call refills
     */
    @Override
    public @NotNull List<@NotNull Unit> getUnits() {
        EntityPool<Unit> pool = this.game.getUnits();
        SlotList used = pool.getUsedSlots();

        this.units.clear();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            this.units.add(pool.get(index));
        }

        return this.unitList;
    }

    /**
     * Returns the read-only state of a unit: the view of its pool slot, created on first use.
     *
     * @param unit the unit
     * @return the unit's view
     */
    @Override
    public @NotNull UnitView getUnitView(@NotNull Unit unit) {
        StandardUnitView view = this.unitViews[unit.getIndex()];

        if (view == null || view.getUnit() != unit) {
            view = new StandardUnitView(this.game, unit, this.bounds, this.fractionBits);

            this.unitViews[unit.getIndex()] = view;
        }

        return view;
    }

    /**
     * Returns the projectiles in flight in pool order, for their pictures: the views of their pool slots, created on first use.
     *
     * @return the projectiles at the time of the call, a read-only list the next call refills
     */
    @Override
    public @NotNull List<@NotNull ProjectileView> getProjectiles() {
        EntityPool<Projectile> pool = this.game.getProjectiles();
        SlotList used = pool.getUsedSlots();

        this.projectiles.clear();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            StandardProjectileView view = this.projectileViews[index];

            if (view == null) {
                view = new StandardProjectileView(pool, pool.get(index), this.projectileDirectionCount);

                this.projectileViews[index] = view;
            }

            this.projectiles.add(view);
        }

        return this.projectileList;
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
    @Override
    public void findUnits(int left, int top, int right, int bottom, @NotNull Collection<@NotNull Unit> result) {
        EntityPool<Unit> pool = this.game.getUnits();
        SlotList used = pool.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit unit = pool.get(index);

            if (this.bounds.isPlaced(unit) &&
                this.bounds.getLeft(unit) <= right && this.bounds.getRight(unit) >= left &&
                this.bounds.getTop(unit) <= bottom && this.bounds.getBottom(unit) >= top) {
                result.add(unit);
            }
        }
    }

    /**
     * Finds the unit shown at a world position: of the placed units whose bounds hold the position, the one with the lowest bottom edge
     * (drawn last), the first in pool order among equals.
     *
     * @param x the world x
     * @param y the world y
     * @return the unit, or {@code null} if no unit holds the position
     */
    @Override
    public @Nullable Unit findUnitAt(int x, int y) {
        EntityPool<Unit> pool = this.game.getUnits();
        SlotList used = pool.getUsedSlots();
        Unit found = null;
        int foundBottom = Integer.MIN_VALUE;

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit unit = pool.get(index);

            if (!this.bounds.isPlaced(unit) ||
                x < this.bounds.getLeft(unit) || x > this.bounds.getRight(unit) ||
                y < this.bounds.getTop(unit) || y > this.bounds.getBottom(unit)) {
                continue;
            }

            int bottom = this.bounds.getBottom(unit);

            if (found == null || bottom > foundBottom) {
                found       = unit;
                foundBottom = bottom;
            }
        }

        return found;
    }

    /**
     * Checks whether a player currently sees a world position.
     *
     * @param player the player
     * @param x      the world x
     * @param y      the world y
     * @return {@code true} if the player sees the position, {@code false} otherwise
     */
    @Override
    public boolean isVisible(@NotNull Player player, int x, int y) {
        return this.game.getVisibility().isVisible(player, x, y);
    }

    /**
     * Checks whether a player has ever seen a world position.
     *
     * @param player the player
     * @param x      the world x
     * @param y      the world y
     * @return {@code true} if the position is explored, {@code false} otherwise
     */
    @Override
    public boolean isExplored(@NotNull Player player, int x, int y) {
        return this.game.getVisibility().isExplored(player, x, y);
    }

    /**
     * Checks whether a player sees a unit.
     *
     * @param player the player
     * @param unit   the unit
     * @return {@code true} if the player sees the unit, {@code false} otherwise
     */
    @Override
    public boolean isUnitVisible(@NotNull Player player, @NotNull Unit unit) {
        return this.game.getVisibility().isUnitVisible(player, unit);
    }

    /**
     * @return the resource kinds of the game in their order
     */
    @Override
    public @NotNull List<@NotNull ResourceKind> getResourceKinds() {
        return this.resourceKinds;
    }

    /**
     * Returns the amount of a resource a player holds.
     *
     * @param player       the player
     * @param resourceKind the resource kind
     * @return the amount
     */
    @Override
    public int getResourceAmount(@NotNull Player player, @NotNull ResourceKind resourceKind) {
        return this.game.getEconomy().getAmount(player, resourceKind);
    }

    /**
     * Returns the faction a player plays.
     *
     * @param player the player
     * @return the faction, or {@code null} for a player without faction (for example neutral)
     */
    @Override
    public @Nullable Faction getFaction(@NotNull Player player) {
        return this.factions[player.getIndex()];
    }

    /**
     * Returns the supply a player's units of a faction use.
     *
     * @param player  the player
     * @param faction the faction
     * @return the used supply
     */
    @Override
    public int getSupplyUsed(@NotNull Player player, @NotNull Faction faction) {
        return this.game.getEconomy().getSupplyUsed(player, faction);
    }

    /**
     * Returns the supply a player's units of a faction provide.
     *
     * @param player  the player
     * @param faction the faction
     * @return the provided supply
     */
    @Override
    public int getSupplyProvided(@NotNull Player player, @NotNull Faction faction) {
        return this.game.getEconomy().getSupplyProvided(player, faction);
    }

    /**
     * Returns the team colour of a player: the colour of its slot's colour index, wrapped into the colour table.
     *
     * @param player the player
     * @return the colour, packed {@code 0xRRGGBB}
     */
    @Override
    public int getTeamColor(@NotNull Player player) {
        return this.teamColors[Math.floorMod(this.playerColors[player.getIndex()], this.teamColors.length)];
    }
}

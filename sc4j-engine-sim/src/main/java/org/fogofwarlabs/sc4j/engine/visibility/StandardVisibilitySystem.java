package org.fogofwarlabs.sc4j.engine.visibility;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.modifier.Stat;
import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.PlayerSlot;
import org.fogofwarlabs.sc4j.engine.game.frame.FramePhase;
import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.fogofwarlabs.sc4j.engine.map.grid.SquareGridTopology;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Decides what each player sees with the sight descriptors ({@link SightDescriptors}) over a {@link TileVisibilityGrid}: the engine default
 * {@link VisibilitySystem}. A mod registers it under {@link VisibilitySystem#KEY} with the map size, the tile size and the largest sight range,
 * and lists it as a frame phase.
 * {@snippet :
 * builder.useSystem(VisibilitySystem.KEY, context -> new StandardVisibilitySystem(terrain, maximumSightRange))
 *        .addFramePhase(VisibilitySystem.KEY, visibility -> (StandardVisibilitySystem) visibility);
 *}
 * <p>
 * The content's sight ranges are tiles: a unit reveals the unit type's sight range after the owner's {@link Stat#SIGHT_RANGE} modifiers, at
 * most the largest sight range; {@link #getSightRange} returns that range in tiles times the tile width, in pixels like every position.
 * Each running frame the phase hides every tile, then reveals the sight of every owned unit of the pool for its owner by a ground reveal; over a
 * terrain the tiles higher than the walk cell the unit stands on block its sight ({@link TileVisibilityGrid#getGroundBlockers}), without a
 * terrain nothing does. A cloaked unit is detected by its owner, by the allies of its owner (players whose setup slots that are not neutral
 * share a team) and, after each phase, by the allies of the owner of every complete detector (a unit whose type is a {@link UnitTrait#DETECTOR})
 * whose sight range in pixels (its sight range in tiles times the tile size) reaches the cloaked unit's position, the Euclidean distance
 * between the positions.
 * <p>
 * Determinism contract: the phase visits the units in slot order; the queries read the state of the last update. Nothing draws a random value or
 * allocates. The grid, the cloaked flags and the detection masks, in slot order, are mixed into the state hash.
 */
public final class StandardVisibilitySystem implements VisibilitySystem, FramePhase, StateHashContributor, SnapshotContributor {
    private static @NotNull GridTopology createTiles(int mapWidth, int mapHeight, int tileSize) {
        if (tileSize <= 0) {
            throw Exceptions.newIllegalArgumentException("The standard visibility system requires a positive tile size: {0}", tileSize);
        }

        return new SquareGridTopology(tileSize, mapWidth, mapHeight);
    }

    private static <T> @NotNull T requireAttached(@Nullable T value) {
        if (value == null) {
            throw Exceptions.newIllegalStateException("The standard visibility system is not attached.");
        }

        return value;
    }

    private final int mapWidth;
    private final @NotNull GridTopology tiles;
    private final int maximumSightRange;
    private final @Nullable TerrainGrid terrain;

    private final @NotNull TileVisibilityGrid grid;
    private final @NotNull SightDescriptors descriptors;

    private @Nullable EntityPool<Unit> units;
    private @Nullable GameContent content;
    private boolean @NotNull [] cloaked;
    private int @NotNull [] detections;
    private int @NotNull [] allies;
    private @Nullable Game game;

    /**
     * Creates a {@link StandardVisibilitySystem} instance without terrain in which no player sees any tile.
     *
     * @param mapWidth          the map width in tiles
     * @param mapHeight         the map height in tiles
     * @param tileSize          the size of a tile in pixels
     * @param maximumSightRange the largest sight range in tiles
     * @throws IllegalArgumentException if a value is not positive
     */
    public StandardVisibilitySystem(int mapWidth, int mapHeight, int tileSize, int maximumSightRange) {
        this(mapWidth, mapHeight, createTiles(mapWidth, mapHeight, tileSize), maximumSightRange, null);
    }

    /**
     * Creates a {@link StandardVisibilitySystem} instance over a terrain in which no player sees any tile; the terrain's elevations block the
     * sight of ground units.
     *
     * @param terrain           the terrain, whose tile elevations are read once
     * @param maximumSightRange the largest sight range in tiles
     * @throws IllegalArgumentException if a value is not positive
     */
    public StandardVisibilitySystem(@NotNull TerrainGrid terrain, int maximumSightRange) {
        this(terrain.getWidth(), terrain.getHeight(), terrain.getTileGrid(), maximumSightRange, terrain);
    }

    private StandardVisibilitySystem(int mapWidth, int mapHeight, @NotNull GridTopology tiles, int maximumSightRange, @Nullable TerrainGrid terrain) {
        this.mapWidth          = mapWidth;
        this.tiles             = tiles;
        this.maximumSightRange = maximumSightRange;
        this.terrain           = terrain;

        this.grid        = terrain != null ? new TileVisibilityGrid(terrain) : new TileVisibilityGrid(mapWidth, mapHeight);
        this.descriptors = new SightDescriptors(mapWidth, maximumSightRange);

        this.units      = null;
        this.content    = null;
        this.cloaked    = new boolean[0];
        this.detections = new int[0];
        this.allies     = new int[0];
        this.game       = null;
    }

    /**
     * @return the tile storage of the system
     */
    public @NotNull TileVisibilityGrid getGrid() {
        return this.grid;
    }


    /**
     * Resolves what this system uses from the game. Called once, after every system exists.
     * <p>
     * Binds the unit pool and the content, holds one cloaked flag and one detection mask per pool slot, and reads the allies of each player from
     * the setup.
     *
     * @param game the game
     */
    @Override
    public void attach(@NotNull Game game) {
        this.units      = game.getUnits();
        this.content    = game.getContent();
        this.cloaked    = new boolean[game.getUnits().getCapacity() + 1];
        this.detections = new int[game.getUnits().getCapacity() + 1];
        this.allies     = new int[game.getPlayerCount()];
        this.game       = game;

        for (int index = 0; index < this.allies.length; index++) {
            this.allies[index] = 1 << index;
        }

        for (PlayerSlot slot : game.getPlayers()) {
            for (PlayerSlot other : game.getPlayers()) {
                if (slot.getSlotIndex() < this.allies.length && other.getSlotIndex() < this.allies.length && slot.getTeam() == other.getTeam() &&
                    slot.getControllerKind() != ControllerKind.NEUTRAL && other.getControllerKind() != ControllerKind.NEUTRAL) {
                    this.allies[slot.getSlotIndex()] |= 1 << other.getSlotIndex();
                }
            }
        }
    }

    /**
     * Checks whether a player currently sees a point.
     *
     * @param player the player
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @return {@code true} if the point is in the player's sight, {@code false} otherwise
     */
    @Override
    public boolean isVisible(@NotNull Player player, int x, int y) {
        int tileIndex = this.findTileIndex(x, y);

        return tileIndex >= 0 && this.grid.isVisible(tileIndex, player.getIndex());
    }

    /**
     * Checks whether a player has ever seen a point.
     *
     * @param player the player
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @return {@code true} if the point is explored by the player, {@code false} otherwise
     */
    @Override
    public boolean isExplored(@NotNull Player player, int x, int y) {
        int tileIndex = this.findTileIndex(x, y);

        return tileIndex >= 0 && this.grid.isExplored(tileIndex, player.getIndex());
    }

    /**
     * Checks whether a player sees a unit: the unit is in the player's sight and, if it is cloaked, detected by the player.
     *
     * @param player the player
     * @param unit   the unit
     * @return {@code true} if the player sees the unit, {@code false} otherwise
     */
    @Override
    public boolean isUnitVisible(@NotNull Player player, @NotNull Unit unit) {
        return this.isVisible(player, unit.getPositionX(), unit.getPositionY()) && this.isDetected(player, unit);
    }

    /**
     * Checks whether a player detects a cloaked or burrowed unit.
     * <p>
     * The standard system detects a cloaked unit for its owner, the owner's allies and the players the detectors of the last phase reached, as
     * the class description says.
     *
     * @param player the player
     * @param unit   the unit
     * @return {@code true} if the unit is not hidden from the player, {@code false} otherwise
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public boolean isDetected(@NotNull Player player, @NotNull Unit unit) {
        requireAttached(this.units);

        int index = unit.getIndex();
        Player owner = unit.getOwner();

        if (!this.cloaked[index] || owner == player || (this.detections[index] & 1 << player.getIndex()) != 0) {
            return true;
        }

        return owner != null && owner.getIndex() < this.allies.length && (this.allies[owner.getIndex()] & 1 << player.getIndex()) != 0;
    }

    /**
     * Returns the current sight range of a unit, after its stat modifiers.
     * <p>
     * The standard system reads the unit type's sight range in tiles and returns it times the tile width; a unit without a type sees nothing.
     *
     * @param unit the unit
     * @return the sight range in pixels
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public int getSightRange(@NotNull Unit unit) {
        return this.findTileSightRange(unit) * this.tiles.getCellWidth();
    }

    /**
     * Reads the sight range of a unit in tiles: the unit type's sight range after the owner's {@link Stat#SIGHT_RANGE} modifiers, {@code 0}
     * for a unit without a type.
     */
    private int findTileSightRange(@NotNull Unit unit) {
        GameContent attachedContent = requireAttached(this.content);
        UnitType type = unit.getType();

        if (type == null) {
            return 0;
        }

        int sightRange = attachedContent.getUnitTypes().getSightRange(type);
        Player owner = unit.getOwner();

        return owner != null ? attachedContent.applyModifiers(Stat.SIGHT_RANGE, owner, type, null, sightRange) : sightRange;
    }

    /**
     * Updates the sight a unit gives its owner after it moved, changed type or changed its sight range.
     * <p>
     * The standard system reveals the unit's sight to its owner now, blocked by the tiles higher than the unit's walk cell; a unit without an
     * owner or sight reveals nothing.
     *
     * @param unit the unit
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public void updateSight(@NotNull Unit unit) {
        Player owner = unit.getOwner();
        int sightRange = this.findTileSightRange(unit);

        if (owner == null || sightRange <= 0) {
            return;
        }

        int playerMask = 1 << owner.getIndex();
        int column = this.tiles.findColumn(unit.getPositionX(), unit.getPositionY());
        int row = this.tiles.findRow(unit.getPositionX(), unit.getPositionY());
        TerrainGrid attachedTerrain = this.terrain;
        int elevation = 0;

        if (attachedTerrain != null) {
            GridTopology walkCells = attachedTerrain.getWalkGrid();

            elevation = attachedTerrain.getElevation(walkCells.findColumn(unit.getPositionX(), unit.getPositionY()),
                                                     walkCells.findRow(unit.getPositionX(), unit.getPositionY()));
        }

        this.descriptors.reveal(this.grid, column, row, false, playerMask, Math.min(sightRange, this.maximumSightRange) - 1,
                                playerMask | this.grid.getGroundBlockers(elevation));
    }

    /**
     * Sets whether a unit is cloaked: hidden from the players that do not detect it.
     *
     * @param unit    the unit
     * @param cloaked whether the unit is cloaked
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public void setCloaked(@NotNull Unit unit, boolean cloaked) {
        requireAttached(this.units);

        this.cloaked[unit.getIndex()] = cloaked;
    }

    /**
     * Runs this phase of the current frame.
     * <p>
     * Hides every tile, then reveals the sight of every unit of the pool in slot order, then detects every cloaked unit in slot order with the
     * detectors in slot order.
     *
     * @param game the game
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public void run(@NotNull Game game) {
        EntityPool<Unit> pool = requireAttached(this.units);
        SlotList used = pool.getUsedSlots();

        this.grid.hideAll();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            this.updateSight(pool.get(index));
        }

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            this.detections[index] = this.cloaked[index] ? this.findDetectingPlayers(pool, pool.get(index)) : 0;
        }
    }

    /**
     * Collects the players that detect a cloaked unit through a detector: the allies of the owner of every complete detector whose sight range
     * reaches the unit.
     */
    private int findDetectingPlayers(@NotNull EntityPool<Unit> pool, @NotNull Unit unit) {
        GameContent attachedContent = requireAttached(this.content);
        Game attachedGame = requireAttached(this.game);
        ProductionSystem production = attachedGame.hasSystem(ProductionSystem.KEY) ? attachedGame.getProduction() : null;
        SlotList used = pool.getUsedSlots();
        int mask = 0;

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit detector = pool.get(index);
            UnitType type = detector.getType();
            Player owner = detector.getOwner();

            if (type == null || owner == null || owner.getIndex() >= this.allies.length ||
                !attachedContent.getUnitTypes().hasTrait(type, UnitTrait.DETECTOR) ||
                production != null && production.isUnderConstruction(detector)) {
                continue;
            }

            long range = this.getSightRange(detector);
            long distanceX = unit.getPositionX() - detector.getPositionX();
            long distanceY = unit.getPositionY() - detector.getPositionY();

            if (distanceX * distanceX + distanceY * distanceY <= range * range) {
                mask |= this.allies[owner.getIndex()];
            }
        }

        return mask;
    }

    /**
     * Mixes this part of the simulation state into the hash: the grid, then the cloaked flag and then the detection mask of every pool slot.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        this.grid.contributeStateHash(hash);

        for (boolean value : this.cloaked) {
            hash.mixBoolean(value);
        }

        hash.mixArray(this.detections);
    }

    /**
     * Writes this part of the simulation state: the sight grid ({@link TileVisibilityGrid#writeSnapshot}), the cloak flags and the detections of
     * every unit slot.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        this.grid.writeSnapshot(writer);
        writer.writeBooleans(this.cloaked);
        writer.writeInts(this.detections);
    }

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or holds another number of values
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        this.grid.readSnapshot(reader);
        reader.readBooleans(this.cloaked);
        reader.readInts(this.detections);
    }

    private int findTileIndex(int x, int y) {
        int column = this.tiles.findColumn(x, y);
        int row = this.tiles.findRow(x, y);

        if (!this.tiles.contains(column, row)) {
            return -1;
        }

        return row * this.mapWidth + column;
    }
}

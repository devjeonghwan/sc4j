package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.WorldLayer;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows where a building would stand: the engine default placement {@link WorldLayer}. While a building's footprint is shown ({@link #show}),
 * the layer snaps it to the tile grid under the pointer (its top-left corner on the tile corner nearest to the pointed top-left corner) and
 * draws each tile it covers through the projection, green where the building may stand and red where it may not, the mod's
 * {@link PlacementGhost} of the building over them ({@link #setGhost}) and the footprint's outline, green when every tile takes the building and
 * red otherwise. The covered tiles are the tiles from the one holding the footprint's top-left world position to the one holding its
 * bottom-right world position, as the standard production system checks them; a tile takes the building when the terrain lets buildings
 * stand there and no placed unit the local player sees that blocks placements ({@link UnitView#isPlacementBlocker}) overlaps it. The snapped
 * centre ({@link #getCenterX}, {@link #getCenterY}) is the target of the build command.
 */
public final class PlacementLayer implements WorldLayer {
    /**
     * The colour the ghost of a building is drawn in where it may stand: its own colours, see-through.
     */
    public static final int GHOST_COLOR = 0xFFFFFFA0;
    /**
     * The colour the ghost of a building is drawn in where it may not stand: reddened, see-through.
     */
    public static final int BLOCKED_GHOST_COLOR = 0xFF8080A0;

    private static final int BUILDABLE_COLOR = 0x40FF4060;
    private static final int BLOCKED_COLOR = 0xFF404070;
    private static final int BUILDABLE_OUTLINE_COLOR = 0x40FF40FF;
    private static final int BLOCKED_OUTLINE_COLOR = 0xFF4040FF;
    private static final float OUTLINE_THICKNESS = 1;
    private static final int HALF_DIVISOR = 2;

    private final @NotNull Canvas canvas;
    private final @NotNull GameView view;
    private final @NotNull WorldPicker picker;

    private final float @NotNull [] corners;
    private final @NotNull List<@NotNull Unit> nearbyUnits;

    private @Nullable PlacementGhost ghost;

    private @Nullable UnitType type;
    private int footprintWidth;
    private int footprintHeight;
    private int left;
    private int top;

    /**
     * Creates a {@link PlacementLayer} instance that shows no footprint and no ghost.
     *
     * @param canvas the canvas the layer draws on
     * @param view   the game view
     * @param picker the picker that converts the pointer to the world
     */
    public PlacementLayer(@NotNull Canvas canvas, @NotNull GameView view, @NotNull WorldPicker picker) {
        this.canvas = canvas;
        this.view   = view;
        this.picker = picker;

        this.corners     = new float[WorldProjection.OUTLINE_LENGTH];
        this.nearbyUnits = new ArrayList<>();

        this.ghost = null;

        this.type            = null;
        this.footprintWidth  = 0;
        this.footprintHeight = 0;
        this.left            = 0;
        this.top             = 0;
    }

    /**
     * @return {@code true} if a footprint is shown, {@code false} otherwise
     */
    public boolean isShown() {
        return this.type != null;
    }

    /**
     * @return the building type whose footprint is shown, or {@code null} while none is shown
     */
    public @Nullable UnitType getType() {
        return this.type;
    }

    /**
     * @return the world x of the snapped footprint's centre
     */
    public int getCenterX() {
        return this.left + this.footprintWidth / HALF_DIVISOR;
    }

    /**
     * @return the world y of the snapped footprint's centre
     */
    public int getCenterY() {
        return this.top + this.footprintHeight / HALF_DIVISOR;
    }

    /**
     * @return the drawer of the building's picture over the footprint, or {@code null} for the footprint alone
     */
    public @Nullable PlacementGhost getGhost() {
        return this.ghost;
    }

    /**
     * Sets the drawer of the building's picture over the footprint.
     *
     * @param ghost the drawer, or {@code null} for the footprint alone
     */
    public void setGhost(@Nullable PlacementGhost ghost) {
        this.ghost = ghost;
    }


    /**
     * Shows the footprint of a building type until {@link #hide}.
     *
     * @param type            the building type
     * @param footprintWidth  the footprint width in world units
     * @param footprintHeight the footprint height in world units
     * @throws IllegalArgumentException if a size is not positive
     */
    public void show(@NotNull UnitType type, int footprintWidth, int footprintHeight) {
        if (footprintWidth <= 0 || footprintHeight <= 0) {
            throw Exceptions.newIllegalArgumentException("A placement footprint must not be empty: {0}x{1}", footprintWidth, footprintHeight);
        }

        this.type            = type;
        this.footprintWidth  = footprintWidth;
        this.footprintHeight = footprintHeight;
    }

    /**
     * Hides the footprint.
     */
    public void hide() {
        this.type = null;
    }

    /**
     * Moves the footprint to the pointer: its centre under the pointer, snapped to the tile grid.
     *
     * @param x the framebuffer column of the pointer
     * @param y the framebuffer row of the pointer
     */
    public void moveTo(float x, float y) {
        GridTopology tiles = this.view.getTerrain().getTileGrid();
        int pointedLeft = this.picker.toWorldX(x, y) - this.footprintWidth / HALF_DIVISOR + tiles.getCellWidth() / HALF_DIVISOR;
        int pointedTop = this.picker.toWorldY(x, y) - this.footprintHeight / HALF_DIVISOR + tiles.getCellHeight() / HALF_DIVISOR;
        int column = tiles.findColumn(pointedLeft, pointedTop);
        int row = tiles.findRow(pointedLeft, pointedTop);

        this.left = tiles.getCenterX(column, row) - tiles.getCellWidth() / HALF_DIVISOR;
        this.top  = tiles.getCenterY(column, row) - tiles.getCellHeight() / HALF_DIVISOR;
    }

    /**
     * Checks whether every tile under the snapped footprint takes the building: the terrain lets buildings stand there and no placed unit the
     * local player sees that blocks placements overlaps it.
     *
     * @return {@code true} if the building may stand on the snapped site, {@code false} otherwise
     */
    public boolean isBuildable() {
        GridTopology tiles = this.view.getTerrain().getTileGrid();
        int right = this.left + this.footprintWidth - 1;
        int bottom = this.top + this.footprintHeight - 1;

        this.findBlockers();

        for (int row = tiles.findRow(this.left, this.top); row <= tiles.findRow(right, bottom); row++) {
            for (int column = tiles.findColumn(this.left, this.top); column <= tiles.findColumn(right, bottom); column++) {
                if (!this.isTileBuildable(column, row)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Draws the layer for a frame: each tile of the shown footprint in the colour of whether it takes the building, the ghost of the building
     * over them and the footprint's outline in the colour of whether the building may stand there.
     *
     * @param viewport the frame's viewport
     */
    @Override
    public void draw(@NotNull WorldViewport viewport) {
        UnitType shown = this.type;

        if (shown == null) {
            return;
        }

        GridTopology tiles = this.view.getTerrain().getTileGrid();
        int right = this.left + this.footprintWidth - 1;
        int bottom = this.top + this.footprintHeight - 1;
        int halfWidth = tiles.getCellWidth() / HALF_DIVISOR;
        int halfHeight = tiles.getCellHeight() / HALF_DIVISOR;
        boolean buildable = true;

        this.findBlockers();

        for (int row = tiles.findRow(this.left, this.top); row <= tiles.findRow(right, bottom); row++) {
            for (int column = tiles.findColumn(this.left, this.top); column <= tiles.findColumn(right, bottom); column++) {
                int centerX = tiles.getCenterX(column, row);
                int centerY = tiles.getCenterY(column, row);
                boolean tileBuildable = this.isTileBuildable(column, row);

                buildable = buildable && tileBuildable;
                viewport.outlineOnScreen(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight, this.corners);
                this.canvas.fillQuadrilateral(this.corners, tileBuildable ? BUILDABLE_COLOR : BLOCKED_COLOR);
            }
        }

        PlacementGhost current = this.ghost;

        if (current != null) {
            current.drawGhost(viewport, shown, this.getCenterX(), this.getCenterY(), buildable ? GHOST_COLOR : BLOCKED_GHOST_COLOR);
        }

        viewport.outlineOnScreen(this.left, this.top, this.left + this.footprintWidth, this.top + this.footprintHeight, this.corners);
        this.canvas.drawQuadrilateral(this.corners, OUTLINE_THICKNESS, buildable ? BUILDABLE_OUTLINE_COLOR : BLOCKED_OUTLINE_COLOR);
    }

    /**
     * Finds the placed units the local player sees (every unit without a local player) that block placements and overlap the footprint.
     */
    private void findBlockers() {
        Player player = this.view.getLocalPlayer();
        List<Unit> units = this.nearbyUnits;

        units.clear();
        this.view.findUnits(this.left, this.top, this.left + this.footprintWidth - 1, this.top + this.footprintHeight - 1, units);
        units.removeIf(unit -> !this.view.getUnitView(unit).isPlacementBlocker() || player != null && !this.view.isUnitVisible(player, unit));
    }

    /**
     * Checks whether a tile takes the building: the terrain lets buildings stand there and no blocker found by {@link #findBlockers} overlaps
     * the tile's part of the footprint.
     */
    private boolean isTileBuildable(int column, int row) {
        TerrainGrid terrain = this.view.getTerrain();
        GridTopology tiles = terrain.getTileGrid();

        if (!terrain.isBuildable(column, row)) {
            return false;
        }

        int cellLeft = tiles.getCenterX(column, row) - tiles.getCellWidth() / HALF_DIVISOR;
        int cellTop = tiles.getCenterY(column, row) - tiles.getCellHeight() / HALF_DIVISOR;
        int tileLeft = Math.max(cellLeft, this.left);
        int tileTop = Math.max(cellTop, this.top);
        int tileRight = Math.min(cellLeft + tiles.getCellWidth() - 1, this.left + this.footprintWidth - 1);
        int tileBottom = Math.min(cellTop + tiles.getCellHeight() - 1, this.top + this.footprintHeight - 1);

        for (int index = 0; index < this.nearbyUnits.size(); index++) {
            UnitView unit = this.view.getUnitView(this.nearbyUnits.get(index));

            if (unit.getLeft() <= tileRight && unit.getRight() >= tileLeft && unit.getTop() <= tileBottom && unit.getBottom() >= tileTop) {
                return false;
            }
        }

        return true;
    }
}

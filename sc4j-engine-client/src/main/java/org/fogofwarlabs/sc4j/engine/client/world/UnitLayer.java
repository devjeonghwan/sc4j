package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.QuadrilateralUtils;
import org.fogofwarlabs.sc4j.engine.client.render.WorldLayer;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Draws the units the local player sees as the footprints of their bounds in the owner's team colour, outlined darker: the engine default
 * unit {@link WorldLayer} for mods without sprites (a mod with sprite sheets draws them through a
 * {@link org.fogofwarlabs.sc4j.engine.client.render.SpriteBatch} layer instead). The footprint is the screen outline of the bounds through the
 * projection, a box from above and a diamond in the isometric projection. Units draw in the depth order of the projection
 * ({@link UnitDrawOrder}); units whose footprint lies outside the shown area are skipped. An observer without local player sees every unit.
 */
public final class UnitLayer implements WorldLayer {
    private static final int FILL_ALPHA = 0xC0;
    private static final int OUTLINE_ALPHA = 0xFF;
    private static final int OUTLINE_SHADE_MASK = 0x7F7F7F;
    private static final float OUTLINE_THICKNESS = 1;

    private final @NotNull Canvas canvas;
    private final @NotNull GameView view;

    private final @NotNull UnitDrawOrder order;
    private final float @NotNull [] corners;

    /**
     * Creates a {@link UnitLayer} instance.
     *
     * @param canvas the canvas the layer draws on
     * @param view   the game view
     */
    public UnitLayer(@NotNull Canvas canvas, @NotNull GameView view) {
        this.canvas = canvas;
        this.view   = view;

        this.order   = new UnitDrawOrder();
        this.corners = new float[WorldProjection.OUTLINE_LENGTH];
    }

    /**
     * Draws the layer for a frame: the footprint of every shown unit the local player sees, in depth order.
     *
     * @param viewport the frame's viewport
     */
    @Override
    public void draw(@NotNull WorldViewport viewport) {
        Player player = this.view.getLocalPlayer();
        List<Unit> units = this.order.sort(this.view, viewport.getProjection());

        for (int index = 0; index < units.size(); index++) {
            Unit unit = units.get(index);
            UnitView unitView = this.view.getUnitView(unit);
            Player owner = unitView.getOwner();

            if (owner == null || !unitView.isPlaced() || player != null && !this.view.isUnitVisible(player, unit)) {
                continue;
            }

            this.drawFootprint(viewport, unitView, this.view.getTeamColor(owner));
        }
    }

    /**
     * Draws the footprint of one unit in a team colour, unless it lies outside the shown area.
     *
     * @param viewport  the frame's viewport
     * @param unitView  the unit's view
     * @param teamColor the owner's team colour, packed {@code 0xRRGGBB}
     */
    void drawFootprint(@NotNull WorldViewport viewport, @NotNull UnitView unitView, int teamColor) {
        viewport.outlineOnScreen(unitView.getLeft(), unitView.getTop(), unitView.getRight() + 1, unitView.getBottom() + 1, this.corners);

        if (!QuadrilateralUtils.overlaps(this.corners, 0, 0, viewport.getWidth(), viewport.getHeight())) {
            return;
        }

        this.canvas.fillQuadrilateral(this.corners, teamColor << Byte.SIZE | FILL_ALPHA);
        this.canvas.drawQuadrilateral(this.corners, OUTLINE_THICKNESS, (teamColor >> 1 & OUTLINE_SHADE_MASK) << Byte.SIZE | OUTLINE_ALPHA);
    }
}

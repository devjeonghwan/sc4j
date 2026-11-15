package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.command.Selection;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.WorldLayer;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Draws the rally points of the selection in the world view: the engine default rally {@link WorldLayer}. Each selected, placed unit of the
 * local player (any selected unit without a local player) that has a rally point ({@link UnitView#hasRallyPoint}) gets a line from its
 * position to the rally point and a marker there: a small square of the ground, through the projection, filled and outlined.
 */
public final class RallyLayer implements WorldLayer {
    /**
     * The colour of the line and the marker's outline.
     */
    public static final int RALLY_COLOR = 0xF0D040E0;

    private static final int MARKER_FILL_COLOR = 0xF0D04070;
    private static final float LINE_THICKNESS = 1;
    /**
     * The half side of the marker's ground square in world units.
     */
    private static final int MARKER_HALF_SIZE = 6;

    private final @NotNull Canvas canvas;
    private final @NotNull GameView view;
    private final @NotNull Selection selection;

    private final float @NotNull [] corners;

    /**
     * Creates a {@link RallyLayer} instance.
     *
     * @param canvas    the canvas the layer draws on
     * @param view      the game view
     * @param selection the local player's selection
     */
    public RallyLayer(@NotNull Canvas canvas, @NotNull GameView view, @NotNull Selection selection) {
        this.canvas    = canvas;
        this.view      = view;
        this.selection = selection;

        this.corners = new float[WorldProjection.OUTLINE_LENGTH];
    }

    /**
     * Draws the layer for a frame: the line and the marker of each selected unit's rally point.
     *
     * @param viewport the frame's viewport
     */
    @Override
    public void draw(@NotNull WorldViewport viewport) {
        Player player = this.view.getLocalPlayer();
        WorldProjection projection = viewport.getProjection();
        List<Unit> units = this.selection.getUnits();

        for (int index = 0; index < units.size(); index++) {
            UnitView unitView = this.view.getUnitView(units.get(index));

            if (!unitView.isPlaced() || player != null && unitView.getOwner() != player || !unitView.hasRallyPoint()) {
                continue;
            }

            int rallyX = unitView.getRallyPointX();
            int rallyY = unitView.getRallyPointY();

            this.canvas.drawLine(viewport.toScreenX(projection.toProjectedX(unitView.getX(), unitView.getY())),
                                 viewport.toScreenY(projection.toProjectedY(unitView.getX(), unitView.getY())),
                                 viewport.toScreenX(projection.toProjectedX(rallyX, rallyY)),
                                 viewport.toScreenY(projection.toProjectedY(rallyX, rallyY)), LINE_THICKNESS, RALLY_COLOR);
            viewport.outlineOnScreen(rallyX - MARKER_HALF_SIZE, rallyY - MARKER_HALF_SIZE, rallyX + MARKER_HALF_SIZE, rallyY + MARKER_HALF_SIZE,
                                     this.corners);
            this.canvas.fillQuadrilateral(this.corners, MARKER_FILL_COLOR);
            this.canvas.drawQuadrilateral(this.corners, LINE_THICKNESS, RALLY_COLOR);
        }
    }
}

package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.command.Selection;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.QuadrilateralUtils;
import org.fogofwarlabs.sc4j.engine.client.render.WorldLayer;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Draws the selection in the world view: the engine default selection {@link WorldLayer}. Each selected unit that is placed gets an outline of
 * its bounds and its hit point (coloured by their share, {@link StatusBarUtils#findHitPointColor}) and damage layer bars below them; the dragged
 * {@link SelectionBox} is drawn as an outline.
 */
public final class SelectionLayer implements WorldLayer {
    private static final int SELECTION_COLOR = 0x40FF40FF;
    private static final float OUTLINE_THICKNESS = 1;
    private static final float BAR_HEIGHT = 3;
    private static final float BAR_GAP = 2;

    private final @NotNull Canvas canvas;
    private final @NotNull GameView view;
    private final @NotNull Selection selection;
    private final @NotNull SelectionBox box;

    private final float @NotNull [] corners;

    /**
     * Creates a {@link SelectionLayer} instance that outlines in green.
     *
     * @param canvas    the canvas the layer draws on
     * @param view      the game view
     * @param selection the local player's selection
     * @param box       the selection box
     */
    public SelectionLayer(@NotNull Canvas canvas, @NotNull GameView view, @NotNull Selection selection, @NotNull SelectionBox box) {
        this.canvas    = canvas;
        this.view      = view;
        this.selection = selection;
        this.box       = box;

        this.corners = new float[WorldProjection.OUTLINE_LENGTH];
    }

    /**
     * Draws the layer for a frame: the outlines and bars of the selected units and the dragged box.
     *
     * @param viewport the frame's viewport
     */
    @Override
    public void draw(@NotNull WorldViewport viewport) {
        List<Unit> units = this.selection.getUnits();

        for (int index = 0; index < units.size(); index++) {
            UnitView unitView = this.view.getUnitView(units.get(index));

            if (!unitView.isPlaced()) {
                continue;
            }

            viewport.outlineOnScreen(unitView.getLeft(), unitView.getTop(), unitView.getRight() + 1, unitView.getBottom() + 1, this.corners);

            float x = QuadrilateralUtils.getLeft(this.corners);
            float width = QuadrilateralUtils.getWidth(this.corners);
            float barY = QuadrilateralUtils.getBottom(this.corners) + BAR_GAP;

            this.canvas.drawQuadrilateral(this.corners, OUTLINE_THICKNESS, SELECTION_COLOR);
            StatusBarUtils.drawBar(this.canvas, x, barY, width, BAR_HEIGHT,
                                   unitView.getHitPoints(), unitView.getMaximumHitPoints(),
                                   StatusBarUtils.findHitPointColor(unitView.getHitPoints(), unitView.getMaximumHitPoints()),
                                   StatusBarUtils.EMPTY_COLOR);

            for (int layer = 0; layer < unitView.getDamageLayerCount(); layer++) {
                StatusBarUtils.drawBar(this.canvas, x, barY + BAR_HEIGHT * (layer + 1), width, BAR_HEIGHT,
                                       unitView.getDamageLayerPoints(layer), unitView.getMaximumDamageLayerPoints(layer),
                                       StatusBarUtils.DAMAGE_LAYER_COLOR, StatusBarUtils.EMPTY_COLOR);
            }
        }

        if (this.box.isDragging()) {
            float left = this.box.getLeft();
            float top = this.box.getTop();

            this.canvas.drawRectangle(left, top, this.box.getRight() - left, this.box.getBottom() - top, OUTLINE_THICKNESS, SELECTION_COLOR);
        }
    }
}

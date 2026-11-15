package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.WorldLayer;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelFormat;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteFrame;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Draws the units the local player sees as frames of their sprite sheets: the engine default unit {@link WorldLayer} for a mod with sprite
 * pictures, on the {@link Canvas} of the world view. Each unit's type names its {@link UnitSprite}, which selects the frame from the unit's
 * pose, its heading turned to the screen through the projection and the game loop; the sheet's anchor is placed on the screen position of
 * the unit's world position (the frame is screen art, scaled with the zoom). A frame is composed once per team colour like the engine sprite
 * shader (the shadow layer as black under the picture, the team colour mixed into the colour layer by the mask) and uploaded as a texture of
 * the canvas ({@link SpriteFrameTextures}); the layer keeps the textures until it is closed. Units draw in the depth order of the projection
 * ({@link UnitDrawOrder}); a unit whose type has no sprite is drawn as its footprint like {@link UnitLayer}, and units whose picture lies
 * outside the shown area are skipped. An observer without local player sees every unit.
 * <p>
 * A sheet with detail sheets ({@link UnitSprite#getSheets}) is placed by its own frame and drawn from the same frame of the detail of the
 * canvas's pixel scale times the zoom; a sheet without details is scaled.
 * <p>
 * The layer draws RGBA sheets; an indexed sheet needs a palette and is drawn as its footprint.
 * <p>
 * As a {@link PlacementGhost} it draws the standing frame of a building type ({@link UnitSprite#selectIdleFrame}) in the local player's team
 * colour (the first player's without one) where the building would stand.
 */
public final class UnitSpriteLayer implements WorldLayer, PlacementGhost, AutoCloseable {
    private static final int OPAQUE_WHITE = 0xFFFFFFFF;

    private final @NotNull Canvas canvas;
    private final @NotNull GameView view;
    private final @NotNull Function<@NotNull UnitType, @Nullable UnitSprite> sprites;

    private final @NotNull UnitDrawOrder order;
    private final @NotNull UnitLayer footprints;
    private final @NotNull SpriteFrameTextures textures;

    /**
     * Creates a {@link UnitSpriteLayer} instance; it uploads nothing until it draws.
     *
     * @param canvas  the canvas the layer draws on
     * @param view    the game view
     * @param sprites the sprite of each unit type, or {@code null} for a type drawn as its footprint
     */
    public UnitSpriteLayer(@NotNull Canvas canvas, @NotNull GameView view, @NotNull Function<@NotNull UnitType, @Nullable UnitSprite> sprites) {
        this.canvas  = canvas;
        this.view    = view;
        this.sprites = sprites;

        this.order      = new UnitDrawOrder();
        this.footprints = new UnitLayer(canvas, view);
        this.textures   = new SpriteFrameTextures(canvas);
    }

    /**
     * Draws the layer for a frame: the picture of every shown unit the local player sees, in depth order.
     *
     * @param viewport the frame's viewport
     */
    @Override
    public void draw(@NotNull WorldViewport viewport) {
        Player player = this.view.getLocalPlayer();
        WorldProjection projection = viewport.getProjection();
        List<Unit> units = this.order.sort(this.view, projection);
        float zoom = viewport.getZoom();
        float pixelsPerUnit = this.canvas.getPixelScale() * zoom;

        for (int index = 0; index < units.size(); index++) {
            Unit unit = units.get(index);
            UnitView unitView = this.view.getUnitView(unit);
            Player owner = unitView.getOwner();
            UnitType type = unitView.getType();

            if (owner == null || type == null || !unitView.isPlaced() || player != null && !this.view.isUnitVisible(player, unit)) {
                continue;
            }

            UnitSprite sprite = this.sprites.apply(type);

            if (sprite == null || sprite.getSheet().getColor().getFormat() != PixelFormat.RGBA_8888) {
                this.footprints.drawFootprint(viewport, unitView, this.view.getTeamColor(owner));

                continue;
            }

            SpriteSheet sheet = sprite.getSheet();
            int frameIndex = sprite.selectFrame(unitView, projection.toScreenDirection(unitView.getHeading()), this.view.getFrame());
            SpriteFrame frame = sheet.getFrame(frameIndex);

            if (frame.getWidth() == 0 || frame.getHeight() == 0) {
                continue;
            }

            float anchorX = projection.toProjectedX(unitView.getX(), unitView.getY());
            float anchorY = projection.toProjectedY(unitView.getX(), unitView.getY());
            float left = viewport.toScreenX(anchorX - sheet.getAnchorX() + frame.getX());
            float top = viewport.toScreenY(anchorY - sheet.getAnchorY() + frame.getY());
            float width = frame.getWidth() * zoom;
            float height = frame.getHeight() * zoom;

            if (left + width < 0 || top + height < 0 || left > viewport.getWidth() || top > viewport.getHeight()) {
                continue;
            }

            SpriteSheet drawn = sprite.getSheets().find(pixelsPerUnit);

            this.canvas.drawTexture(this.textures.getTexture(drawn, frameIndex, drawn.getFrame(frameIndex), this.view.getTeamColor(owner)),
                                    left, top, width, height, OPAQUE_WHITE);
        }
    }

    /**
     * Draws the ghost of a building type at a world position.
     * <p>
     * Draws the type's standing frame anchored at the position, multiplied by the colour; a type without an RGBA sprite draws nothing.
     *
     * @param viewport the frame's viewport
     * @param type     the building type
     * @param x        the world x of the building's position, the centre of its footprint
     * @param y        the world y of the building's position, the centre of its footprint
     * @param color    the packed {@code 0xRRGGBBAA} colour the picture is multiplied by, its alpha the ghost's opacity
     */
    @Override
    public void drawGhost(@NotNull WorldViewport viewport, @NotNull UnitType type, int x, int y, int color) {
        UnitSprite sprite = this.sprites.apply(type);

        if (sprite == null || sprite.getSheet().getColor().getFormat() != PixelFormat.RGBA_8888) {
            return;
        }

        Player player = this.view.getLocalPlayer();
        Player owner = player != null ? player : this.view.getPlayers().getFirst();
        WorldProjection projection = viewport.getProjection();
        SpriteSheet sheet = sprite.getSheet();
        int frameIndex = sprite.selectIdleFrame();
        SpriteFrame frame = sheet.getFrame(frameIndex);
        float zoom = viewport.getZoom();

        if (frame.getWidth() == 0 || frame.getHeight() == 0) {
            return;
        }

        float left = viewport.toScreenX(projection.toProjectedX(x, y) - sheet.getAnchorX() + frame.getX());
        float top = viewport.toScreenY(projection.toProjectedY(x, y) - sheet.getAnchorY() + frame.getY());
        SpriteSheet drawn = sprite.getSheets().find(this.canvas.getPixelScale() * zoom);

        this.canvas.drawTexture(this.textures.getTexture(drawn, frameIndex, drawn.getFrame(frameIndex), this.view.getTeamColor(owner)),
                                left, top, frame.getWidth() * zoom, frame.getHeight() * zoom, color);
    }

    /**
     * Disposes resources: releases the uploaded frame textures.
     */
    @Override
    public void close() {
        this.textures.close();
    }
}

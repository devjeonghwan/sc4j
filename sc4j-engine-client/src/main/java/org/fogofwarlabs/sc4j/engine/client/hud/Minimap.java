package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.command.CommandTarget;
import org.fogofwarlabs.sc4j.engine.client.command.TargetPicker;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.QuadrilateralUtils;
import org.fogofwarlabs.sc4j.engine.client.render.Texture;
import org.fogofwarlabs.sc4j.engine.client.skin.HudSkin;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.client.world.Camera;
import org.fogofwarlabs.sc4j.engine.client.world.TerrainColors;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shows the whole map in a HUD rectangle: the engine default minimap. The rectangle shows the map's bounds on the projected plane of the camera's
 * {@link WorldProjection} at their own aspect, as large as the rectangle allows and centred in it (the map area, {@link #getMapAreaLeft}), so the map
 * looks as in the world view (a square from above, a diamond twice as wide as high in the usual isometric projection); the rest of the rectangle
 * shows the skin's minimap background. It draws the terrain image of {@link TerrainColors} through the projection at the map area's size in
 * framebuffer pixels (uploaded at the first draw and again when the size changes), over it the local player's fog by tile as an image of the same
 * pixels (each pixel takes the fog of the tile under it, so explored ground shows dimmed and seen ground clear up to the tile's edge; uploaded again
 * when a tile's fog changes), the units the player sees as dots in their team colours, and the camera's shown area as an outline: the part of the
 * screen between the HUD's top and bottom rows ({@link #setViewInsets}), cut at the minimap's edges. A left press or drag inside moves the camera's
 * centre to the pointed place; as a {@link TargetPicker} it gives the map position under a framebuffer position, so that a right press on the minimap
 * issues commands there. The skin's minimap background is drawn under the terrain and its border over everything ({@link HudSkin}).
 * <p>
 * Pings ({@link #addPing}) mark world positions for {@link #PING_FRAMES} frames of {@link #PING_FRAME_INTERVAL} milliseconds each
 * ({@link #update}), drawn as squares over the units. The colours of the dots, the fog and the camera outline and the size of a ping come
 * from the minimap's {@link MinimapStyle} ({@link #setStyle}), by default {@link StandardMinimapStyle}.
 */
public final class Minimap extends HudBlock implements TargetPicker, AutoCloseable {
    /**
     * The number of pings shown at once; a further ping is dropped.
     */
    public static final int PING_SLOTS = 8;
    /**
     * The number of frames a ping shows.
     */
    public static final int PING_FRAMES = 16;
    /**
     * The duration of a ping frame in milliseconds.
     */
    public static final long PING_FRAME_INTERVAL = 100;

    private static final int OPAQUE_WHITE = 0xFFFFFFFF;
    private static final int OPAQUE_ALPHA = 0xFF;
    private static final int ALPHA_SHIFT = 24;
    private static final float LINE_THICKNESS = 1;
    private static final float MINIMUM_DOT_SIZE = 2;
    private static final int HALF_DIVISOR = 2;
    private static final int COORDINATES_PER_CORNER = 2;

    private final @NotNull GameView view;
    private final @NotNull Camera camera;
    private final @NotNull TerrainColors colors;
    private @NotNull MinimapStyle style;

    private final float @NotNull [] corners;
    private final @NotNull MinimapPings pings;
    private long pingTime;
    private float topInset;
    private float bottomInset;

    private @Nullable Texture image;
    private @Nullable Texture fogImage;
    private int imageWidth;
    private int imageHeight;
    private int @NotNull [] pixelTiles;
    private int @NotNull [] tileFog;
    private int @NotNull [] nextTileFog;
    private boolean dragging;

    private float areaLeft;
    private float areaTop;
    private float areaWidth;
    private float areaHeight;

    /**
     * Creates a {@link Minimap} instance; it uploads nothing until it draws.
     *
     * @param view   the game view
     * @param camera the camera it shows and moves, whose projection it shows the map in
     * @param colors the terrain colours
     */
    public Minimap(@NotNull GameView view, @NotNull Camera camera, @NotNull TerrainColors colors) {
        this.view   = view;
        this.camera = camera;
        this.colors = colors;
        this.style  = new StandardMinimapStyle();

        this.corners     = new float[WorldProjection.OUTLINE_LENGTH];
        this.pings       = new MinimapPings(PING_SLOTS, PING_FRAMES);
        this.pingTime    = 0;
        this.topInset    = 0;
        this.bottomInset = 0;

        this.image       = null;
        this.fogImage    = null;
        this.imageWidth  = 0;
        this.imageHeight = 0;
        this.pixelTiles  = new int[0];
        this.tileFog     = new int[0];
        this.nextTileFog = new int[0];
        this.dragging    = false;

        this.areaLeft   = 0;
        this.areaTop    = 0;
        this.areaWidth  = 0;
        this.areaHeight = 0;
    }

    /**
     * @return the style that decides the colours and the ping size
     */
    public @NotNull MinimapStyle getStyle() {
        return this.style;
    }

    /**
     * Sets the style that decides the colours and the ping size.
     *
     * @param style the style
     */
    public void setStyle(@NotNull MinimapStyle style) {
        this.style = style;
    }

    /**
     * Sets the rows at the top and the bottom of the screen that the HUD covers: the camera outline shows the part of the screen between them.
     *
     * @param topInset    the covered rows at the top of the screen, in screen units
     * @param bottomInset the covered rows at the bottom of the screen, in screen units
     */
    public void setViewInsets(float topInset, float bottomInset) {
        this.topInset    = topInset;
        this.bottomInset = bottomInset;
    }

    /**
     * @return the left edge of the map area: the part of the rectangle the map's projected bounds fill at their own aspect
     */
    public float getMapAreaLeft() {
        this.updateMapArea();

        return this.areaLeft;
    }

    /**
     * @return the top edge of the map area
     */
    public float getMapAreaTop() {
        this.updateMapArea();

        return this.areaTop;
    }

    /**
     * @return the width of the map area
     */
    public float getMapAreaWidth() {
        this.updateMapArea();

        return this.areaWidth;
    }

    /**
     * @return the height of the map area
     */
    public float getMapAreaHeight() {
        this.updateMapArea();

        return this.areaHeight;
    }

    /**
     * @return the pings of the minimap
     */
    public @NotNull MinimapPings getPings() {
        return this.pings;
    }


    /**
     * Adds a ping in a colour; without a free ping slot it is dropped.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @param color  the packed colour {@code 0xRRGGBBAA}
     */
    public void addPing(int worldX, int worldY, int color) {
        this.pings.add(worldX, worldY, color, false);
    }

    /**
     * Adds a ping in a player's team colour; without a free ping slot it is dropped.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @param player the player whose colour the ping shows
     */
    public void addPing(int worldX, int worldY, @NotNull Player player) {
        this.pings.add(worldX, worldY, player.getIndex(), true);
    }

    /**
     * Moves the pings on for the time since the last frame, one frame per {@link #PING_FRAME_INTERVAL} milliseconds.
     *
     * @param milliseconds the time since the last frame
     */
    public void update(long milliseconds) {
        if (!this.pings.isAnyActive()) {
            this.pingTime = 0;

            return;
        }

        this.pingTime += Math.max(0, milliseconds);

        while (this.pingTime >= PING_FRAME_INTERVAL) {
            this.pingTime -= PING_FRAME_INTERVAL;
            this.pings.advance();
        }
    }

    /**
     * Converts a framebuffer position of the minimap to a world x through the projection.
     *
     * @param pointerX the framebuffer column
     * @param pointerY the framebuffer row
     * @return the world x, inside the map
     */
    public int toWorldX(float pointerX, float pointerY) {
        float worldX = this.camera.getProjection().toWorldX(this.toProjectedX(pointerX), this.toProjectedY(pointerY));

        return Math.clamp((long) Math.floor(worldX), 0, this.view.getMapWidth() - 1);
    }

    /**
     * Converts a framebuffer position of the minimap to a world y through the projection.
     *
     * @param pointerX the framebuffer column
     * @param pointerY the framebuffer row
     * @return the world y, inside the map
     */
    public int toWorldY(float pointerX, float pointerY) {
        float worldY = this.camera.getProjection().toWorldY(this.toProjectedX(pointerX), this.toProjectedY(pointerY));

        return Math.clamp((long) Math.floor(worldY), 0, this.view.getMapHeight() - 1);
    }

    /**
     * Picks the target at a framebuffer position: the map position under it.
     *
     * @param x the framebuffer column
     * @param y the framebuffer row
     * @return the target, or {@code null} if the position lies outside the minimap
     */
    @Override
    public @Nullable CommandTarget pick(float x, float y) {
        return this.contains(x, y) ? CommandTarget.at(this.toWorldX(x, y), this.toWorldY(x, y)) : null;
    }

    /**
     * Draws the block: terrain, fog, units, pings and the camera outline.
     *
     * @param canvas the canvas, between its begin and end
     */
    @Override
    public void draw(@NotNull Canvas canvas) {
        this.updateMapArea();

        int width = Math.max(1, Math.round(this.areaWidth * canvas.getPixelScale()));
        int height = Math.max(1, Math.round(this.areaHeight * canvas.getPixelScale()));
        Texture texture = this.image;

        if (texture == null || width != this.imageWidth || height != this.imageHeight) {
            this.closeImages();

            texture = canvas.uploadTexture(width, height,
                                           this.colors.createImage(this.view.getTerrain(), this.camera.getProjection(), width, height));

            this.image       = texture;
            this.imageWidth  = width;
            this.imageHeight = height;
            this.pixelTiles  = TerrainColors.findPixelTiles(this.view.getTerrain(), this.camera.getProjection(), width, height);
        }

        float scaleX = this.areaWidth / (this.camera.getBoundsRight() - this.camera.getBoundsLeft());
        float scaleY = this.areaHeight / (this.camera.getBoundsBottom() - this.camera.getBoundsTop());

        HudSkin skin = this.getSkin();

        skin.getMinimapBackground().draw(canvas, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        canvas.drawTexture(texture, this.areaLeft, this.areaTop, this.areaWidth, this.areaHeight, OPAQUE_WHITE);
        this.drawFog(canvas);
        this.drawUnits(canvas);
        this.drawPings(canvas);

        float zoom = this.camera.getZoom();
        float shownTop = this.camera.getY() + this.topInset / zoom;
        float shownHeight = Math.max(this.camera.getViewHeight() - (this.topInset + this.bottomInset) / zoom, 0);

        canvas.setClip((int) this.getX(), (int) this.getY(), (int) Math.ceil(this.getWidth()), (int) Math.ceil(this.getHeight()));
        canvas.drawRectangle(this.areaLeft + (this.camera.getX() - this.camera.getBoundsLeft()) * scaleX,
                             this.areaTop + (shownTop - this.camera.getBoundsTop()) * scaleY,
                             this.camera.getViewWidth() * scaleX, shownHeight * scaleY, LINE_THICKNESS, this.style.getCameraColor());
        canvas.setClip(0, 0, -1, -1);
        skin.getMinimapBorder().draw(canvas, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    private float toProjectedX(float pointerX) {
        this.updateMapArea();

        return this.camera.getBoundsLeft() +
               (pointerX - this.areaLeft) * (this.camera.getBoundsRight() - this.camera.getBoundsLeft()) / this.areaWidth;
    }

    private float toProjectedY(float pointerY) {
        this.updateMapArea();

        return this.camera.getBoundsTop() +
               (pointerY - this.areaTop) * (this.camera.getBoundsBottom() - this.camera.getBoundsTop()) / this.areaHeight;
    }

    /**
     * Fits the map's projected bounds into the rectangle at their own aspect, centred: the map area.
     */
    private void updateMapArea() {
        float boundsWidth = this.camera.getBoundsRight() - this.camera.getBoundsLeft();
        float boundsHeight = this.camera.getBoundsBottom() - this.camera.getBoundsTop();
        float scale = Math.min(this.getWidth() / boundsWidth, this.getHeight() / boundsHeight);

        this.areaWidth  = boundsWidth * scale;
        this.areaHeight = boundsHeight * scale;
        this.areaLeft   = this.getX() + (this.getWidth() - this.areaWidth) / HALF_DIVISOR;
        this.areaTop    = this.getY() + (this.getHeight() - this.areaHeight) / HALF_DIVISOR;
    }

    /**
     * Writes the minimap outline of a world rectangle: its projected outline moved and scaled from the map's projected bounds into the
     * minimap rectangle.
     */
    private void outlineOnMinimap(float left, float top, float right, float bottom) {
        float scaleX = this.areaWidth / (this.camera.getBoundsRight() - this.camera.getBoundsLeft());
        float scaleY = this.areaHeight / (this.camera.getBoundsBottom() - this.camera.getBoundsTop());

        this.camera.getProjection().outlineFootprint(left, top, right, bottom, this.corners);

        for (int index = 0; index < WorldProjection.OUTLINE_LENGTH; index += COORDINATES_PER_CORNER) {
            this.corners[index]     = this.areaLeft + (this.corners[index] - this.camera.getBoundsLeft()) * scaleX;
            this.corners[index + 1] = this.areaTop + (this.corners[index + 1] - this.camera.getBoundsTop()) * scaleY;
        }
    }

    /**
     * Draws the local player's fog: finds the fog colour of every tile (none where the player sees the tile's centre), uploads the fog image
     * again when a tile's colour changed since the last draw, and draws it over the terrain image.
     */
    private void drawFog(@NotNull Canvas canvas) {
        Player player = this.view.getLocalPlayer();

        if (player == null) {
            return;
        }

        GridTopology tiles = this.view.getTerrain().getTileGrid();
        int tileCount = tiles.getColumnCount() * tiles.getRowCount();
        int[] fog = this.nextTileFog.length == tileCount ? this.nextTileFog : new int[tileCount];
        boolean changed = this.fogImage == null || this.tileFog.length != tileCount;

        for (int tileY = 0; tileY < tiles.getRowCount(); tileY++) {
            for (int tileX = 0; tileX < tiles.getColumnCount(); tileX++) {
                int x = tiles.getCenterX(tileX, tileY);
                int y = tiles.getCenterY(tileX, tileY);
                int index = tileY * tiles.getColumnCount() + tileX;
                int color = this.view.isVisible(player, x, y) ? 0 : this.style.getFogColor(this.view.isExplored(player, x, y));

                fog[index] = color >>> Byte.SIZE | color << ALPHA_SHIFT;
                changed    = changed || fog[index] != this.tileFog[index];
            }
        }

        this.nextTileFog = this.tileFog;
        this.tileFog     = fog;

        Texture texture = this.fogImage;

        if (changed) {
            int[] pixels = new int[this.pixelTiles.length];

            for (int index = 0; index < pixels.length; index++) {
                int tile = this.pixelTiles[index];

                pixels[index] = tile >= 0 ? fog[tile] : 0;
            }

            if (texture != null) {
                texture.close();
            }

            texture       = canvas.uploadTexture(this.imageWidth, this.imageHeight, pixels);
            this.fogImage = texture;
        }

        if (texture != null) {
            canvas.drawTexture(texture, this.areaLeft, this.areaTop, this.areaWidth, this.areaHeight, OPAQUE_WHITE);
        }
    }

    private void drawUnits(@NotNull Canvas canvas) {
        Player player = this.view.getLocalPlayer();
        List<Unit> units = this.view.getUnits();

        for (int index = 0; index < units.size(); index++) {
            Unit unit = units.get(index);
            UnitView unitView = this.view.getUnitView(unit);
            Player owner = unitView.getOwner();

            if (owner == null || !unitView.isPlaced() || player != null && !this.view.isUnitVisible(player, unit)) {
                continue;
            }

            this.outlineOnMinimap(unitView.getLeft(), unitView.getTop(), unitView.getRight() + 1, unitView.getBottom() + 1);

            float width = Math.max(QuadrilateralUtils.getWidth(this.corners), MINIMUM_DOT_SIZE);
            float height = Math.max(QuadrilateralUtils.getHeight(this.corners), MINIMUM_DOT_SIZE);

            canvas.fillRectangle(QuadrilateralUtils.getLeft(this.corners), QuadrilateralUtils.getTop(this.corners), width, height,
                                 this.style.getUnitColor(this.view, unit, owner));
        }
    }

    private void drawPings(@NotNull Canvas canvas) {
        for (int slot = 0; slot < this.pings.getSlotCount(); slot++) {
            if (!this.pings.isActive(slot)) {
                continue;
            }

            int color = this.pings.getColor(slot);

            if (this.pings.isPlayerColor(slot)) {
                Player player = this.findPlayer(color);

                if (player == null) {
                    continue;
                }

                color = this.view.getTeamColor(player) << Byte.SIZE | OPAQUE_ALPHA;
            }

            float size = this.style.getPingSize(this.pings.getFrame(slot), this.pings.getFrameCount());

            this.outlineOnMinimap(this.pings.getX(slot), this.pings.getY(slot), this.pings.getX(slot), this.pings.getY(slot));
            canvas.drawRectangle(this.corners[0] - size / HALF_DIVISOR, this.corners[1] - size / HALF_DIVISOR, size, size, LINE_THICKNESS, color);
        }
    }

    private @Nullable Player findPlayer(int index) {
        List<Player> players = this.view.getPlayers();

        for (int position = 0; position < players.size(); position++) {
            if (players.get(position).getIndex() == index) {
                return players.get(position);
            }
        }

        return null;
    }

    /**
     * Handles an input event: a left press inside moves the camera there and starts a drag that follows the pointer until the release.
     *
     * @param event the event
     * @return {@code true} if the minimap took the event, {@code false} otherwise
     */
    @Override
    public boolean handleInput(@NotNull InputEvent event) {
        if (event.getType() == InputEvent.Type.BUTTON_PRESSED && event.getButton() == PointerButton.LEFT &&
            this.contains(event.getX(), event.getY())) {
            this.dragging = true;
            this.camera.centerOn(this.toWorldX(event.getX(), event.getY()), this.toWorldY(event.getX(), event.getY()));

            return true;
        }

        if (this.dragging) {
            if (event.getType() == InputEvent.Type.POINTER_MOVED) {
                this.camera.centerOn(this.toWorldX(event.getX(), event.getY()), this.toWorldY(event.getX(), event.getY()));

                return true;
            }

            if (event.getType() == InputEvent.Type.BUTTON_RELEASED && event.getButton() == PointerButton.LEFT) {
                this.dragging = false;

                return true;
            }
        }

        return super.handleInput(event);
    }

    /**
     * Disposes resources: releases the uploaded terrain and fog images.
     */
    @Override
    public void close() {
        this.closeImages();
    }

    private void closeImages() {
        Texture texture = this.image;
        Texture fog = this.fogImage;

        this.image    = null;
        this.fogImage = null;

        if (fog != null) {
            fog.close();
        }

        if (texture != null) {
            texture.close();
        }
    }
}

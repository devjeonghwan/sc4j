package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.WorldLayer;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.ProjectileView;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelFormat;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteFrame;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Draws the projectiles in flight the local player sees as frames of their sprite sheets, and the impact effect where a flight ended: the
 * engine default projectile {@link WorldLayer} for a mod with sprite pictures, drawn over the units. Each projectile's motion profile names its
 * {@link ProjectileSprite}; the frame's row is the projectile's heading turned to the screen through the projection, and the sheet's anchor is
 * placed on the screen position of the projectile's ground position. A flight that is gone at a later game loop (its pool slot is free or
 * carries another flight) plays its sprite's impact frames once at its last position. Frames are composed and uploaded like the unit sprites
 * ({@link SpriteFrameTextures}, without team colour) and drawn from the detail sheets like them; the layer keeps the textures until it is
 * closed. An observer without local player sees every projectile.
 * <p>
 * The layer draws RGBA sheets; a projectile without sprite or with an indexed sheet is not drawn.
 */
public final class ProjectileSpriteLayer implements WorldLayer, AutoCloseable {
    private static final int OPAQUE_WHITE = 0xFFFFFFFF;
    private static final int NO_FRAME = -1;

    /**
     * Holds one flight as the layer saw it last, or an impact effect that plays.
     */
    private static final class Flight {
        private final @NotNull ProjectileView view;
        private final int flight;
        private final @NotNull ProjectileSprite sprite;

        private int x;
        private int y;
        private int impactFrame;

        private Flight(@NotNull ProjectileView view, @NotNull ProjectileSprite sprite) {
            this.view   = view;
            this.flight = view.getFlight();
            this.sprite = sprite;

            this.x           = view.getX();
            this.y           = view.getY();
            this.impactFrame = NO_FRAME;
        }

        private boolean isFlying() {
            return this.view.getFlight() == this.flight && this.impactFrame == NO_FRAME;
        }
    }

    private final @NotNull Canvas canvas;
    private final @NotNull GameView view;
    private final @NotNull Function<@NotNull MotionProfile, @Nullable ProjectileSprite> sprites;

    private final @NotNull SpriteFrameTextures textures;
    private final @NotNull List<@NotNull Flight> flights;
    private int seenFrame;

    /**
     * Creates a {@link ProjectileSpriteLayer} instance; it uploads nothing until it draws.
     *
     * @param canvas  the canvas the layer draws on
     * @param view    the game view
     * @param sprites the sprite of each projectile motion profile, or {@code null} for a type that is not drawn
     */
    public ProjectileSpriteLayer(@NotNull Canvas canvas, @NotNull GameView view,
                                 @NotNull Function<@NotNull MotionProfile, @Nullable ProjectileSprite> sprites) {
        this.canvas  = canvas;
        this.view    = view;
        this.sprites = sprites;

        this.textures  = new SpriteFrameTextures(canvas);
        this.flights   = new ArrayList<>();
        this.seenFrame = NO_FRAME;
    }

    /**
     * @return the number of impact effects that play now
     */
    public int getImpactCount() {
        int count = 0;

        for (Flight flight : this.flights) {
            count += flight.impactFrame != NO_FRAME ? 1 : 0;
        }

        return count;
    }


    /**
     * Draws the layer for a frame: the picture of every projectile the local player sees and the impact effects that play.
     *
     * @param viewport the frame's viewport
     */
    @Override
    public void draw(@NotNull WorldViewport viewport) {
        int frame = this.view.getFrame();

        if (frame != this.seenFrame) {
            this.track(frame);
        }

        Player player = this.view.getLocalPlayer();
        WorldProjection projection = viewport.getProjection();

        for (Flight flight : this.flights) {
            if (player != null && !this.view.isVisible(player, flight.x, flight.y)) {
                continue;
            }

            if (flight.impactFrame == NO_FRAME) {
                int screenDirection = projection.toScreenDirection(flight.view.getHeading());

                this.drawFrame(viewport, flight.sprite.getSheets(), flight.sprite.selectFrame(screenDirection, frame), flight.x, flight.y);
            } else {
                DetailSet<SpriteSheet> impact = flight.sprite.getImpactSheets();
                int impactFrame = flight.sprite.selectImpactFrame(frame - flight.impactFrame);

                if (impact != null && impactFrame != NO_FRAME) {
                    this.drawFrame(viewport, impact, impactFrame, flight.x, flight.y);
                }
            }
        }
    }

    /**
     * Takes the projectiles of a new game loop: a flight that is gone starts its impact at this loop, an impact that played is dropped, and a
     * new flight is added.
     */
    private void track(int frame) {
        List<ProjectileView> projectiles = this.view.getProjectiles();

        this.seenFrame = frame;
        this.flights.removeIf(flight -> flight.impactFrame != NO_FRAME && flight.sprite.selectImpactFrame(frame - flight.impactFrame) == NO_FRAME);

        for (Flight flight : this.flights) {
            if (flight.impactFrame != NO_FRAME) {
                continue;
            }

            if (flight.isFlying() && projectiles.contains(flight.view)) {
                flight.x = flight.view.getX();
                flight.y = flight.view.getY();
            } else {
                flight.impactFrame = frame;
            }
        }

        this.flights.removeIf(flight -> flight.impactFrame == frame && flight.sprite.getImpactSheet() == null);

        for (ProjectileView projectile : projectiles) {
            MotionProfile motionProfile = projectile.getMotionProfile();
            ProjectileSprite sprite = motionProfile == null ? null : this.sprites.apply(motionProfile);

            if (sprite != null && this.findFlying(projectile) == null) {
                this.flights.add(new Flight(projectile, sprite));
            }
        }
    }

    private @Nullable Flight findFlying(@NotNull ProjectileView projectile) {
        for (Flight flight : this.flights) {
            if (flight.view == projectile && flight.isFlying()) {
                return flight;
            }
        }

        return null;
    }

    private void drawFrame(@NotNull WorldViewport viewport, @NotNull DetailSet<SpriteSheet> sheets, int frameIndex, int x, int y) {
        SpriteSheet sheet = sheets.getResource();

        if (sheet.getColor().getFormat() != PixelFormat.RGBA_8888) {
            return;
        }

        WorldProjection projection = viewport.getProjection();
        SpriteFrame frame = sheet.getFrame(frameIndex);
        float zoom = viewport.getZoom();
        float left = viewport.toScreenX(projection.toProjectedX(x, y) - sheet.getAnchorX() + frame.getX());
        float top = viewport.toScreenY(projection.toProjectedY(x, y) - sheet.getAnchorY() + frame.getY());
        float width = frame.getWidth() * zoom;
        float height = frame.getHeight() * zoom;

        if (frame.getWidth() == 0 || frame.getHeight() == 0 || left + width < 0 || top + height < 0 || left > viewport.getWidth() ||
            top > viewport.getHeight()) {
            return;
        }

        SpriteSheet drawn = sheets.find(this.canvas.getPixelScale() * zoom);

        this.canvas.drawTexture(this.textures.getTexture(drawn, frameIndex, drawn.getFrame(frameIndex), SpriteFrameTextures.NO_TEAM_COLOR),
                                left, top, width, height, OPAQUE_WHITE);
    }


    /**
     * Disposes resources: releases the uploaded frame textures.
     */
    @Override
    public void close() {
        this.textures.close();
    }
}

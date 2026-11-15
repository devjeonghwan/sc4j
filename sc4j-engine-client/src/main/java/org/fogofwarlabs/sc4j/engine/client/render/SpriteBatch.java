package org.fogofwarlabs.sc4j.engine.client.render;

import org.fogofwarlabs.sc4j.engine.resource.graphics.Palette;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.jetbrains.annotations.NotNull;

/**
 * Draws frames of {@link SpriteSheet}s of the resource model into a {@link WorldViewport}: the sheet's anchor is placed on the
 * screen position of the world position through the viewport's projection (the frame itself is screen art, not projected), an indexed
 * colour layer is coloured through the current {@link Palette}, the team colour mask mixes in the
 * owner's team colour, and the shadow layer is drawn as black before the sprite. Drawing happens between {@link #begin} and
 * {@link #end}; sprites are drawn in call order.
 */
public interface SpriteBatch extends AutoCloseable {
    /**
     * Begins drawing a frame into the window.
     *
     * @param viewport the frame's viewport
     */
    void begin(@NotNull WorldViewport viewport);

    /**
     * Sets the palette of indexed colour layers; it applies to the following draws.
     *
     * @param palette the palette, at most 256 colours
     */
    void setPalette(@NotNull Palette palette);

    /**
     * Draws one frame of a sprite sheet.
     *
     * @param sheet      the sprite sheet
     * @param frameIndex the frame of the sheet
     * @param x          the world x the anchor is placed on
     * @param y          the world y the anchor is placed on
     * @param flipped    whether the frame is mirrored horizontally around the anchor
     * @param teamColor  the owner's team colour, packed {@code 0xRRGGBB}
     */
    void draw(@NotNull SpriteSheet sheet, int frameIndex, int x, int y, boolean flipped, int teamColor);

    /**
     * Submits the pending drawing.
     */
    void end();

    /**
     * Disposes resources: releases the uploaded frames and the drawing objects.
     */
    @Override
    void close();
}

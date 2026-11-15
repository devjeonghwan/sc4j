package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.TeamColorUtils;
import org.fogofwarlabs.sc4j.engine.client.render.Texture;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteFrame;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Holds the canvas textures of sprite sheet frames for the world layers that draw RGBA sheets: a frame is composed once per team colour like
 * the engine sprite shader (the shadow layer as black under the picture, the team colour mixed into the colour layer by the mask) and
 * uploaded on first use; the textures stay until {@link #close()}.
 */
final class SpriteFrameTextures implements AutoCloseable {
    /**
     * The team colour of a picture without team: the mask, if any, mixes in black.
     */
    static final int NO_TEAM_COLOR = 0;

    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int ALPHA_SHIFT = 24;
    private static final int CHANNEL_MASK = 0xFF;
    private static final int RGB_MASK = 0xFFFFFF;
    private static final long FRAME_MASK = 0xFFFFFFFFL;

    /**
     * Composes one frame of an RGBA sheet in a team colour into straight-alpha {@code 0xAARRGGBB} pixels of the frame's rectangle.
     */
    private static int @NotNull [] compose(@NotNull SpriteSheet sheet, @NotNull SpriteFrame frame, int teamColor) {
        PixelLayer color = sheet.getColor();
        PixelLayer mask = sheet.getTeamMask();
        PixelLayer shadow = sheet.getShadow();
        int offset = frame.getPixelOffset();
        int[] pixels = new int[frame.getWidth() * frame.getHeight()];

        for (int index = 0; index < pixels.length; index++) {
            int pixel = color.getValue(offset + index);
            int alpha = pixel >>> ALPHA_SHIFT;
            int shadowAlpha = shadow != null ? shadow.getValue(offset + index) : 0;

            if (alpha == 0) {
                pixels[index] = shadowAlpha << ALPHA_SHIFT;

                continue;
            }

            int maskValue = mask != null ? mask.getValue(offset + index) : 0;
            int rgb = TeamColorUtils.mix(pixel, teamColor, maskValue);
            // The picture is drawn over its shadow: the shadow shows through a translucent pixel as black.
            int covered = alpha + shadowAlpha * (CHANNEL_MASK - alpha) / CHANNEL_MASK;
            int red = (rgb >>> RED_SHIFT & CHANNEL_MASK) * alpha / covered;
            int green = (rgb >>> GREEN_SHIFT & CHANNEL_MASK) * alpha / covered;
            int blue = (rgb & CHANNEL_MASK) * alpha / covered;

            pixels[index] = covered << ALPHA_SHIFT | red << RED_SHIFT | green << GREEN_SHIFT | blue;
        }

        return pixels;
    }


    private final @NotNull Canvas canvas;
    private final @NotNull Map<@NotNull SpriteSheet, @NotNull Map<@NotNull Long, @NotNull Texture>> textures;

    /**
     * Creates a {@link SpriteFrameTextures} instance; it uploads nothing until a texture is asked for.
     *
     * @param canvas the canvas the textures belong to
     */
    SpriteFrameTextures(@NotNull Canvas canvas) {
        this.canvas   = canvas;
        this.textures = new IdentityHashMap<>();
    }

    /**
     * Returns the texture of a frame in a team colour, composing and uploading it on first use.
     *
     * @param sheet      the RGBA sheet
     * @param frameIndex the frame index in the sheet
     * @param frame      the frame
     * @param teamColor  the team colour, packed {@code 0xRRGGBB}
     * @return the texture
     * @throws IndexOutOfBoundsException if the sheet has no such frame
     */
    @NotNull Texture getTexture(@NotNull SpriteSheet sheet, int frameIndex, @NotNull SpriteFrame frame, int teamColor) {
        Map<Long, Texture> sheetTextures = this.textures.computeIfAbsent(sheet, key -> new HashMap<>());
        long key = (long) (teamColor & RGB_MASK) << Integer.SIZE | frameIndex & FRAME_MASK;
        Texture texture = sheetTextures.get(key);

        if (texture == null) {
            if (frameIndex < 0 || frameIndex >= sheet.getFrameCount()) {
                throw Exceptions.newIndexOutOfBoundsException("The sheet has no frame {0}.", frameIndex);
            }

            texture = this.canvas.uploadTexture(frame.getWidth(), frame.getHeight(), compose(sheet, frame, teamColor));
            sheetTextures.put(key, texture);
        }

        return texture;
    }

    /**
     * Disposes resources: releases the uploaded frame textures.
     */
    @Override
    public void close() {
        for (Map<Long, Texture> sheetTextures : this.textures.values()) {
            for (Texture texture : sheetTextures.values()) {
                IgnoreException.execute(
                        texture::close
                );
            }
        }

        this.textures.clear();
    }
}

package org.fogofwarlabs.sc4j.engine.client.render;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.client.display.DetailUtils;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.fogofwarlabs.sc4j.engine.resource.ResourceNotFoundException;
import org.fogofwarlabs.sc4j.engine.resource.ResourceProvider;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Loads the {@link Picture}s of a resource provider once and draws them on one {@link Canvas}: scaled into a rectangle ({@link PictureScaling})
 * or sliced (nine-slice by the picture's borders, the edges and the centre stretched or repeated, {@link SliceFill}). A picture with a team
 * colour mask is composed per team colour by the engine mix rule ({@link TeamColorUtils}); each composed picture is uploaded at its first
 * drawing and kept until {@link #close()}. A picture without mask ignores the team colour.
 * <p>
 * A picture may have detail pictures: the same picture drawn at a whole multiple of its size, which the provider names {@code <key>-2x},
 * {@code <key>-3x} and {@code <key>-4x} ({@link DetailUtils#loadPictures}), each with its own team colour mask and borders. A picture is laid
 * out at its own size and drawn from the smallest detail whose multiple is at least the physical pixels per picture pixel (the canvas's
 * {@link Canvas#getPixelScale} times the canvas units per picture pixel of the drawing), or from the largest one when none is that large; a
 * picture without details is scaled.
 */
public final class PictureRenderer implements AutoCloseable {
    /**
     * The team colour a picture is drawn in when it shows no player: its mask, if any, mixes in black.
     */
    public static final int NO_TEAM_COLOR = 0;
    /**
     * The colour that draws a picture as it is.
     */
    public static final int OPAQUE_WHITE = 0xFFFFFFFF;

    private static final int RGB_MASK = 0xFFFFFF;
    private static final float HALF_DIVISOR = 2;

    private static int @NotNull [] compose(@NotNull Picture picture, int teamColor) {
        PixelLayer color = picture.getColor();
        PixelLayer mask = picture.getTeamMask();
        int[] pixels = new int[color.getLength()];

        color.copyValues(0, pixels, 0, pixels.length);

        if (mask == null) {
            return pixels;
        }

        for (int index = 0; index < pixels.length; index++) {
            int pixel = pixels[index];

            pixels[index] = pixel & ~RGB_MASK | TeamColorUtils.mix(pixel, teamColor, mask.getValue(index));
        }

        return pixels;
    }


    private final @NotNull Canvas canvas;
    private final @NotNull ResourceProvider resources;

    private final @NotNull Map<@NotNull ResourceKey, @NotNull DetailSet<Picture>> pictures;
    private final @NotNull Map<@NotNull Picture, @NotNull DetailSet<Picture>> details;
    private final @NotNull Map<@NotNull Picture, @NotNull Map<@NotNull Integer, @NotNull Texture>> textures;

    /**
     * Creates a {@link PictureRenderer} instance; it loads and uploads nothing until a picture is asked for.
     *
     * @param canvas    the canvas the pictures are drawn on
     * @param resources the provider the pictures are loaded from
     */
    public PictureRenderer(@NotNull Canvas canvas, @NotNull ResourceProvider resources) {
        this.canvas    = canvas;
        this.resources = resources;

        this.pictures = new HashMap<>();
        this.details  = new IdentityHashMap<>();
        this.textures = new IdentityHashMap<>();
    }

    /**
     * @return the canvas the pictures are drawn on
     */
    public @NotNull Canvas getCanvas() {
        return this.canvas;
    }

    /**
     * Returns a picture of the provider, loading it and its detail pictures at the first call.
     *
     * @param key the key of the picture
     * @return the picture
     * @throws ResourceNotFoundException if the provider has no picture under the key
     * @throws IOException               if the picture or a detail picture cannot be read
     * @throws IllegalArgumentException  if a detail picture does not place its size, anchor or borders at its multiple of the picture's
     */
    public @NotNull Picture getPicture(@NotNull ResourceKey key) throws IOException {
        return this.getDetails(key).getResource();
    }

    /**
     * Returns a picture of the provider with its detail pictures, loading them at the first call; every call for a key returns the same set,
     * for example for a cursor that is shown from its details.
     *
     * @param key the key of the picture
     * @return the picture with its details
     * @throws ResourceNotFoundException if the provider has no picture under the key
     * @throws IOException               if the picture or a detail picture cannot be read
     * @throws IllegalArgumentException  if a detail picture does not place its size, anchor or borders at its multiple of the picture's
     */
    public @NotNull DetailSet<Picture> getDetails(@NotNull ResourceKey key) throws IOException {
        DetailSet<Picture> pictureDetails = this.pictures.get(key);

        if (pictureDetails == null) {
            pictureDetails = DetailUtils.loadPictures(this.resources, key);
            this.pictures.put(key, pictureDetails);
            this.details.put(pictureDetails.getResource(), pictureDetails);
        }

        return pictureDetails;
    }

    /**
     * Returns a picture of the provider, loading it at the first call, or nothing when the provider has none under the key, for a look that
     * falls back to the engine default where the mod has no picture.
     *
     * @param key the key of the picture
     * @return the picture, or {@code null} if the provider has no picture under the key
     * @throws IOException if the picture exists but cannot be read
     */
    public @Nullable Picture findPicture(@NotNull ResourceKey key) throws IOException {
        try {
            return this.getPicture(key);
        } catch (ResourceNotFoundException exception) {
            return null;
        }
    }

    /**
     * Returns the multiple of the picture a picture is drawn from at a size on the canvas of this renderer.
     *
     * @param picture the picture
     * @param scale   the canvas units per picture pixel of the drawing, {@code 1} at the picture's own size
     * @return {@code 1} for the picture itself, or the multiple of the picked detail picture
     */
    public int findDrawnMultiple(@NotNull Picture picture, float scale) {
        DetailSet<Picture> pictureDetails = this.details.get(picture);

        return pictureDetails != null ? pictureDetails.findMultiple(this.canvas.getPixelScale() * scale) : 1;
    }

    private @NotNull Picture findDrawnPicture(@NotNull Picture picture, float scale) {
        DetailSet<Picture> pictureDetails = this.details.get(picture);

        return pictureDetails != null ? pictureDetails.find(this.canvas.getPixelScale() * scale) : picture;
    }

    /**
     * Returns the texture of a picture in a team colour, composing and uploading it at the first call.
     *
     * @param picture   the picture
     * @param teamColor the team colour, packed {@code 0xRRGGBB}; ignored for a picture without team colour mask
     * @return the texture, owned by this renderer
     */
    public @NotNull Texture getTexture(@NotNull Picture picture, int teamColor) {
        Map<Integer, Texture> pictureTextures = this.textures.computeIfAbsent(picture, key -> new HashMap<>());
        int key = picture.getTeamMask() != null ? teamColor & RGB_MASK : NO_TEAM_COLOR;
        Texture texture = pictureTextures.get(key);

        if (texture == null) {
            texture = this.canvas.uploadTexture(picture.getWidth(), picture.getHeight(), compose(picture, key));
            pictureTextures.put(key, texture);
        }

        return texture;
    }


    /**
     * Draws a picture scaled into a rectangle, from the detail picture of the drawn size.
     *
     * @param canvas    the canvas of this renderer, between its begin and end
     * @param picture   the picture
     * @param x         the left coordinate of the rectangle
     * @param y         the top coordinate of the rectangle
     * @param width     the width of the rectangle
     * @param height    the height of the rectangle
     * @param scaling   how the picture is scaled into the rectangle
     * @param teamColor the team colour, packed {@code 0xRRGGBB}, or {@link #NO_TEAM_COLOR}
     * @param color     the packed {@code 0xRRGGBBAA} colour multiplied with the picture, {@link #OPAQUE_WHITE} for none
     * @throws IllegalArgumentException if the canvas is not the canvas of this renderer
     */
    public void draw(@NotNull Canvas canvas, @NotNull Picture picture,
                     float x, float y, float width, float height,
                     @NotNull PictureScaling scaling, int teamColor, int color) {
        this.checkCanvas(canvas);

        float scaleX = width / picture.getWidth();
        float scaleY = height / picture.getHeight();
        float scale = scaling == PictureScaling.FIT ? Math.min(scaleX, scaleY) : Math.max(scaleX, scaleY);
        Texture texture = this.getTexture(this.findDrawnPicture(picture, scale), teamColor);

        switch (scaling) {
            case STRETCH -> canvas.drawTexture(texture, x, y, width, height, color);
            case FIT -> {
                float drawnWidth = picture.getWidth() * scale;
                float drawnHeight = picture.getHeight() * scale;

                canvas.drawTexture(texture, x + (width - drawnWidth) / HALF_DIVISOR, y + (height - drawnHeight) / HALF_DIVISOR,
                                   drawnWidth, drawnHeight, color);
            }
            case FILL -> {
                float shownWidth = width / scale / picture.getWidth();
                float shownHeight = height / scale / picture.getHeight();
                float left = (1 - shownWidth) / HALF_DIVISOR;
                float top = (1 - shownHeight) / HALF_DIVISOR;

                canvas.drawTexture(texture, x, y, width, height, left, top, left + shownWidth, top + shownHeight, color);
            }
        }
    }

    /**
     * Draws a picture sliced into a rectangle by its borders (nine-slice) with stretched edges and centre ({@link SliceFill#STRETCH}).
     *
     * @param canvas  the canvas of this renderer, between its begin and end
     * @param picture the picture
     * @param x       the left coordinate of the rectangle
     * @param y       the top coordinate of the rectangle
     * @param width   the width of the rectangle
     * @param height  the height of the rectangle
     * @param color   the packed {@code 0xRRGGBBAA} colour multiplied with the picture, {@link #OPAQUE_WHITE} for none
     * @throws IllegalArgumentException if the canvas is not the canvas of this renderer
     */
    public void drawSliced(@NotNull Canvas canvas, @NotNull Picture picture, float x, float y, float width, float height, int color) {
        this.drawSliced(canvas, picture, x, y, width, height, SliceFill.STRETCH, color);
    }

    /**
     * Draws a picture sliced into a rectangle by its borders (nine-slice): the corners keep their size, the edges run along their side and the
     * centre fills the rest, stretched or repeated at the picture's own size by the fill. A rectangle narrower or lower than the borders of an
     * axis shrinks those borders in proportion. The pieces come from the detail picture of the picture's own size, cut by that detail's
     * borders. A picture without borders is all centre: {@link SliceFill#TILE} repeats the whole picture over the rectangle from its top left
     * corner.
     *
     * @param canvas  the canvas of this renderer, between its begin and end
     * @param picture the picture
     * @param x       the left coordinate of the rectangle
     * @param y       the top coordinate of the rectangle
     * @param width   the width of the rectangle
     * @param height  the height of the rectangle
     * @param fill    how the edges and the centre fill their parts
     * @param color   the packed {@code 0xRRGGBBAA} colour multiplied with the picture, {@link #OPAQUE_WHITE} for none
     * @throws IllegalArgumentException if the canvas is not the canvas of this renderer
     */
    public void drawSliced(@NotNull Canvas canvas, @NotNull Picture picture,
                           float x, float y, float width, float height,
                           @NotNull SliceFill fill, int color) {
        this.checkCanvas(canvas);

        Picture drawn = this.findDrawnPicture(picture, 1);
        Texture texture = this.getTexture(drawn, NO_TEAM_COLOR);
        float drawnWidth = drawn.getWidth();
        float drawnHeight = drawn.getHeight();
        int horizontalBorders = picture.getBorderLeft() + picture.getBorderRight();
        int verticalBorders = picture.getBorderTop() + picture.getBorderBottom();
        float horizontalScale = horizontalBorders > width ? width / horizontalBorders : 1;
        float verticalScale = verticalBorders > height ? height / verticalBorders : 1;
        float[] columns = {x, x + picture.getBorderLeft() * horizontalScale, x + width - picture.getBorderRight() * horizontalScale, x + width};
        float[] rows = {y, y + picture.getBorderTop() * verticalScale, y + height - picture.getBorderBottom() * verticalScale, y + height};
        float[] textureColumns = {0, drawn.getBorderLeft() / drawnWidth, 1 - drawn.getBorderRight() / drawnWidth, 1};
        float[] textureRows = {0, drawn.getBorderTop() / drawnHeight, 1 - drawn.getBorderBottom() / drawnHeight, 1};
        float[] pieceWidths = {picture.getBorderLeft() * horizontalScale, picture.getWidth() - horizontalBorders,
                               picture.getBorderRight() * horizontalScale};
        float[] pieceHeights = {picture.getBorderTop() * verticalScale, picture.getHeight() - verticalBorders,
                                picture.getBorderBottom() * verticalScale};

        for (int row = 0; row < rows.length - 1; row++) {
            for (int column = 0; column < columns.length - 1; column++) {
                float partWidth = columns[column + 1] - columns[column];
                float partHeight = rows[row + 1] - rows[row];

                if (partWidth <= 0 || partHeight <= 0) {
                    continue;
                }

                if (fill == SliceFill.TILE && pieceWidths[column] > 0 && pieceHeights[row] > 0) {
                    this.drawRepeated(canvas, texture, columns[column], rows[row], partWidth, partHeight, pieceWidths[column], pieceHeights[row],
                                      textureColumns[column], textureRows[row], textureColumns[column + 1], textureRows[row + 1], color);
                } else {
                    canvas.drawTexture(texture, columns[column], rows[row], partWidth, partHeight,
                                       textureColumns[column], textureRows[row], textureColumns[column + 1], textureRows[row + 1],
                                       color);
                }
            }
        }
    }

    /**
     * Repeats a whole picture at its own size over a rectangle in a grid of tiles through an origin, so rectangles drawn with the same origin
     * show one continuous texture; the tiles at the rectangle's edges are cut to it. The tiles come from the detail picture of the picture's own
     * size.
     *
     * @param canvas  the canvas of this renderer, between its begin and end
     * @param picture the picture, a seamless texture
     * @param x       the left coordinate of the rectangle
     * @param y       the top coordinate of the rectangle
     * @param width   the width of the rectangle
     * @param height  the height of the rectangle
     * @param originX the left coordinate of one tile of the grid, for example {@code 0} for the canvas origin
     * @param originY the top coordinate of one tile of the grid
     * @param color   the packed {@code 0xRRGGBBAA} colour multiplied with the picture, {@link #OPAQUE_WHITE} for none
     * @throws IllegalArgumentException if the canvas is not the canvas of this renderer
     */
    public void drawTiled(@NotNull Canvas canvas, @NotNull Picture picture,
                          float x, float y, float width, float height,
                          float originX, float originY, int color) {
        this.checkCanvas(canvas);

        float tileWidth = picture.getWidth();
        float tileHeight = picture.getHeight();

        if (width <= 0 || height <= 0 || tileWidth <= 0 || tileHeight <= 0) {
            return;
        }

        Texture texture = this.getTexture(this.findDrawnPicture(picture, 1), NO_TEAM_COLOR);
        float firstX = originX + (float) Math.floor((x - originX) / tileWidth) * tileWidth;
        float firstY = originY + (float) Math.floor((y - originY) / tileHeight) * tileHeight;

        for (float tileY = firstY; tileY < y + height; tileY += tileHeight) {
            float top = Math.max(y, tileY);
            float bottom = Math.min(y + height, tileY + tileHeight);

            for (float tileX = firstX; tileX < x + width; tileX += tileWidth) {
                float left = Math.max(x, tileX);
                float right = Math.min(x + width, tileX + tileWidth);

                canvas.drawTexture(texture, left, top, right - left, bottom - top, (left - tileX) / tileWidth, (top - tileY) / tileHeight,
                                   (right - tileX) / tileWidth, (bottom - tileY) / tileHeight, color);
            }
        }
    }

    /**
     * Repeats a piece of a texture over a part at the piece's size, from the part's top left corner; the last piece of each row and column is
     * cut to the part.
     */
    private void drawRepeated(@NotNull Canvas canvas, @NotNull Texture texture,
                              float x, float y, float width, float height,
                              float pieceWidth, float pieceHeight,
                              float left, float top, float right, float bottom,
                              int color) {
        for (float pieceY = 0; pieceY < height; pieceY += pieceHeight) {
            float shownHeight = Math.min(pieceHeight, height - pieceY);
            float pieceBottom = top + (bottom - top) * shownHeight / pieceHeight;

            for (float pieceX = 0; pieceX < width; pieceX += pieceWidth) {
                float shownWidth = Math.min(pieceWidth, width - pieceX);
                float pieceRight = left + (right - left) * shownWidth / pieceWidth;

                canvas.drawTexture(texture, x + pieceX, y + pieceY, shownWidth, shownHeight, left, top, pieceRight, pieceBottom, color);
            }
        }
    }

    private void checkCanvas(@NotNull Canvas canvas) {
        if (canvas != this.canvas) {
            throw Exceptions.newIllegalArgumentException("The picture renderer draws on another canvas.");
        }
    }

    /**
     * Disposes resources: releases the uploaded textures and forgets the loaded pictures and their details.
     */
    @Override
    public void close() {
        for (Map<Integer, Texture> pictureTextures : this.textures.values()) {
            for (Texture texture : pictureTextures.values()) {
                IgnoreException.execute(
                        texture::close
                );
            }
        }

        this.textures.clear();
        this.details.clear();
        this.pictures.clear();
    }
}

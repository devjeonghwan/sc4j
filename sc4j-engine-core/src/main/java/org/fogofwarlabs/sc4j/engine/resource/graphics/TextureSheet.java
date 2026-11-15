package org.fogofwarlabs.sc4j.engine.resource.graphics;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Holds the frames of one animated picture whose pixels are block-compressed GPU textures: a frame box, the frames
 * ({@link TextureFrame}) as rectangles of one texture atlas, and named layers that share that atlas layout, for example a
 * colour layer, a team colour mask and lighting layers. The layer names are the content's.
 * <p>
 * Frame positions and sizes are in sheet units. A texture holds the atlas at {@code 1 / unitsPerTexel} of that resolution:
 * a texel covers {@code unitsPerTexel} by {@code unitsPerTexel} sheet units. A layer is absent when the content has none.
 */
public final class TextureSheet {
    private final int width;
    private final int height;
    private final int unitsPerTexel;

    private final @NotNull List<@NotNull TextureFrame> frames;
    private final @NotNull Map<@NotNull String, @NotNull CompressedTexture> layers;

    /**
     * Creates a {@link TextureSheet} instance.
     *
     * @param width         the width of the frame box in sheet units, at least {@code 0}
     * @param height        the height of the frame box in sheet units, at least {@code 0}
     * @param unitsPerTexel the sheet units one texel covers in each direction, at least {@code 1}
     * @param frames        the frames, copied
     * @param layers        the layers by name in the content's order, copied
     * @throws IllegalArgumentException if the box size is negative or {@code unitsPerTexel} is below {@code 1}
     */
    public TextureSheet(int width, int height, int unitsPerTexel,
                        @NotNull List<@NotNull TextureFrame> frames, @NotNull Map<@NotNull String, @NotNull CompressedTexture> layers) {
        if (width < 0 || height < 0 || unitsPerTexel < 1) {
            throw Exceptions.newIllegalArgumentException("Invalid texture sheet: {0}x{1}, {2} units per texel", width, height, unitsPerTexel);
        }

        this.width         = width;
        this.height        = height;
        this.unitsPerTexel = unitsPerTexel;

        this.frames = List.copyOf(frames);
        this.layers = Collections.unmodifiableMap(new LinkedHashMap<>(layers));
    }

    /**
     * @return the width of the frame box in sheet units
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the height of the frame box in sheet units
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the sheet units one texel covers in each direction
     */
    public int getUnitsPerTexel() {
        return this.unitsPerTexel;
    }

    /**
     * @return the number of frames
     */
    public int getFrameCount() {
        return this.frames.size();
    }

    /**
     * Returns one frame.
     *
     * @param frameIndex the frame index, {@code 0} up to {@link #getFrameCount()} minus one
     * @return the frame
     * @throws IndexOutOfBoundsException if the index names no frame
     */
    public @NotNull TextureFrame getFrame(int frameIndex) {
        return this.frames.get(frameIndex);
    }

    /**
     * @return the names of the layers in the content's order, unmodifiable
     */
    public @NotNull Set<@NotNull String> getLayerNames() {
        return this.layers.keySet();
    }

    /**
     * Finds a layer by name.
     *
     * @param name the layer name
     * @return the layer, or {@code null} if the sheet has no layer of the name
     */
    public @Nullable CompressedTexture findLayer(@NotNull String name) {
        return this.layers.get(name);
    }
}

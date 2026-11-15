package org.fogofwarlabs.sc4j.engine.resource.graphics;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds a block-compressed picture and its mip levels as the GPU stores them ({@link CompressionFormat}). Level {@code 0}
 * is the full picture; level {@code n} has the size of level {@code 0} halved {@code n} times, at least one pixel. The
 * blocks are copied in and out, so a texture never changes; a renderer uploads the blocks without decoding them.
 */
public final class CompressedTexture {
    private final @NotNull CompressionFormat format;
    private final int width;
    private final int height;

    private final @NotNull List<byte @NotNull []> levels;

    /**
     * Creates a {@link CompressedTexture} instance.
     *
     * @param format the block compression
     * @param width  the width of level {@code 0} in pixels, at least {@code 1}
     * @param height the height of level {@code 0} in pixels, at least {@code 1}
     * @param levels the blocks of each mip level from level {@code 0}, at least one, copied
     * @throws IllegalArgumentException if the size is empty, there is no level or a level has another size than its blocks
     */
    public CompressedTexture(@NotNull CompressionFormat format, int width, int height, @NotNull List<byte @NotNull []> levels) {
        if (width < 1 || height < 1) {
            throw Exceptions.newIllegalArgumentException("A texture must not be empty: {0}x{1}", width, height);
        }

        if (levels.isEmpty()) {
            throw Exceptions.newIllegalArgumentException("A texture has at least one level.");
        }

        List<byte[]> copies = new ArrayList<>(levels.size());

        for (int level = 0; level < levels.size(); level++) {
            long expected = format.getStoredSize(Math.max(1, width >> level), Math.max(1, height >> level));

            if (levels.get(level).length != expected) {
                throw Exceptions.newIllegalArgumentException("The level {0} has {1} bytes, its {2} blocks {3}.",
                                                             level, levels.get(level).length, format, expected);
            }

            copies.add(levels.get(level).clone());
        }

        this.format = format;
        this.width  = width;
        this.height = height;

        this.levels = List.copyOf(copies);
    }

    /**
     * @return the block compression
     */
    public @NotNull CompressionFormat getFormat() {
        return this.format;
    }

    /**
     * @return the width of level {@code 0} in pixels
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the height of level {@code 0} in pixels
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the number of mip levels, at least {@code 1}
     */
    public int getLevelCount() {
        return this.levels.size();
    }

    /**
     * Returns the stored size of one level.
     *
     * @param level the mip level
     * @return the size of the level's blocks in bytes
     * @throws IndexOutOfBoundsException if the texture has no such level
     */
    public int getLevelSize(int level) {
        return this.levels.get(level).length;
    }

    /**
     * Copies the blocks of one level into a buffer, for example for a compressed GPU upload.
     *
     * @param level       the mip level
     * @param destination receives {@link #getLevelSize(int)} bytes at its position, which advances
     * @throws IndexOutOfBoundsException if the texture has no such level
     * @throws BufferOverflowException   if the buffer has less room than the level
     */
    public void copyLevel(int level, @NotNull ByteBuffer destination) {
        destination.put(this.levels.get(level));
    }
}

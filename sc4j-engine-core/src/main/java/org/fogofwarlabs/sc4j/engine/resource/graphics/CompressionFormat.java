package org.fogofwarlabs.sc4j.engine.resource.graphics;

/**
 * Names the block compression of a {@link CompressedTexture}: every format stores the pixels in blocks of
 * {@value #BLOCK_DIMENSION} by {@value #BLOCK_DIMENSION} pixels of a fixed byte size, so a renderer uploads them as they are.
 */
public enum CompressionFormat {
    /**
     * BC1 (S3TC DXT1): colour with optional one-bit alpha, eight bytes per block.
     */
    BC1(8),
    /**
     * BC2 (S3TC DXT3): colour with explicit four-bit alpha, sixteen bytes per block.
     */
    BC2(16),
    /**
     * BC3 (S3TC DXT5): colour with interpolated alpha, sixteen bytes per block.
     */
    BC3(16),
    /**
     * BC7: high quality colour and alpha, sixteen bytes per block.
     */
    BC7(16);

    /**
     * The width and height of one block in pixels.
     */
    public static final int BLOCK_DIMENSION = 4;

    private final int blockSize;

    CompressionFormat(int blockSize) {
        this.blockSize = blockSize;
    }

    /**
     * @return the size of one block in bytes
     */
    public int getBlockSize() {
        return this.blockSize;
    }

    /**
     * Returns the stored size of a picture in this format.
     *
     * @param width  the width in pixels, at least {@code 1}
     * @param height the height in pixels, at least {@code 1}
     * @return the size in bytes of the blocks that cover the picture
     */
    public long getStoredSize(int width, int height) {
        long columns = (width + BLOCK_DIMENSION - 1) / BLOCK_DIMENSION;
        long rows = (height + BLOCK_DIMENSION - 1) / BLOCK_DIMENSION;

        return columns * rows * this.blockSize;
    }
}

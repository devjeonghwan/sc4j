package org.fogofwarlabs.sc4j.engine.resource.graphics;

/**
 * Names how a {@link PixelLayer} stores one pixel.
 */
public enum PixelFormat {
    /**
     * One unsigned byte per pixel: an index into a {@link Palette} that the content chooses when it draws.
     */
    INDEXED_8(true),
    /**
     * One unsigned byte per pixel: an alpha or intensity value, {@code 0} (none) up to {@code 255} (full).
     */
    ALPHA_8(true),
    /**
     * One {@code int} per pixel: a colour packed as {@code 0xAARRGGBB} with straight (not premultiplied) alpha.
     */
    RGBA_8888(false);

    private final boolean byteStored;

    PixelFormat(boolean byteStored) {
        this.byteStored = byteStored;
    }

    /**
     * @return {@code true} if a pixel is one unsigned byte, {@code false} if it is one packed {@code int}
     */
    public boolean isByteStored() {
        return this.byteStored;
    }
}

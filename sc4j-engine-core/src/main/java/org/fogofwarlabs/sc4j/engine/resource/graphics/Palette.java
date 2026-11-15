package org.fogofwarlabs.sc4j.engine.resource.graphics;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the colours an {@link PixelFormat#INDEXED_8} layer is drawn with: up to {@value #MAXIMUM_SIZE} colours packed as
 * {@code 0xAARRGGBB} with straight alpha.
 */
public final class Palette {
    /**
     * The largest number of colours: every value of an indexed pixel.
     */
    public static final int MAXIMUM_SIZE = 256;

    private final int @NotNull [] colors;

    /**
     * Creates a {@link Palette} instance.
     *
     * @param colors the colours as {@code 0xAARRGGBB}, {@code 1} up to {@value #MAXIMUM_SIZE} of them, copied
     * @throws IllegalArgumentException if there is no colour or more than {@value #MAXIMUM_SIZE}
     */
    public Palette(int @NotNull [] colors) {
        if (colors.length < 1 || colors.length > MAXIMUM_SIZE) {
            throw Exceptions.newIllegalArgumentException("A palette has 1..{0} colours: {1}", MAXIMUM_SIZE, colors.length);
        }

        this.colors = colors.clone();
    }

    /**
     * @return the number of colours
     */
    public int getSize() {
        return this.colors.length;
    }

    /**
     * Returns one colour.
     *
     * @param index the palette index
     * @return the colour as {@code 0xAARRGGBB}
     * @throws IndexOutOfBoundsException if the index names no colour
     */
    public int getColor(int index) {
        return this.colors[index];
    }

    /**
     * @return a copy of every colour, as {@code 0xAARRGGBB} in index order
     */
    public int @NotNull [] copyColors() {
        return this.colors.clone();
    }
}

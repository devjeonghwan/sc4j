package org.fogofwarlabs.sc4j.engine.client.render;

/**
 * Mixes a player's team colour into a pixel by a team colour mask value, the engine mix rule {@code rgb = mix(rgb, team, mask / 255)} of the
 * sprite shader, for the textures that compose team colours on the processor (sprite frames, pictures).
 */
public final class TeamColorUtils {
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int BLUE_SHIFT = 0;
    private static final int CHANNEL_MASK = 0xFF;
    private static final int RGB_MASK = 0xFFFFFF;
    private static final int HALF = 2;

    /**
     * Mixes a team colour into the colour channels of a pixel.
     *
     * @param color     the pixel, packed {@code 0xAARRGGBB} or {@code 0xRRGGBB}; its alpha is ignored
     * @param teamColor the team colour, packed {@code 0xRRGGBB}
     * @param mask      the mask value, {@code 0} (the pixel's colour) up to {@code 255} (the team colour)
     * @return the mixed colour, packed {@code 0xRRGGBB}
     */
    public static int mix(int color, int teamColor, int mask) {
        if (mask == 0) {
            return color & RGB_MASK;
        }

        return mixChannel(color, teamColor, RED_SHIFT, mask) << RED_SHIFT |
               mixChannel(color, teamColor, GREEN_SHIFT, mask) << GREEN_SHIFT |
               mixChannel(color, teamColor, BLUE_SHIFT, mask);
    }

    private static int mixChannel(int color, int teamColor, int shift, int mask) {
        int value = color >>> shift & CHANNEL_MASK;
        int team = teamColor >>> shift & CHANNEL_MASK;

        return (value * (CHANNEL_MASK - mask) + team * mask + CHANNEL_MASK / HALF) / CHANNEL_MASK;
    }

    private TeamColorUtils() {
    }
}

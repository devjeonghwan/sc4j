package org.fogofwarlabs.sc4j.engine.client.render;

import org.fogofwarlabs.sc4j.engine.client.display.DisplayScale;
import org.fogofwarlabs.sc4j.engine.client.display.UiSurface;
import org.fogofwarlabs.sc4j.engine.client.render.image.ImageCanvas;
import org.fogofwarlabs.sc4j.engine.pack.StandardPack;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.fogofwarlabs.sc4j.engine.resource.ResourceNotFoundException;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests drawing pictures on an offscreen canvas: scaled to fit, fill and stretch, mixed with a team colour by the mask, and sliced by their borders
 * with stretched or repeated edges and centre; loading them once from the resource provider; and drawing them from the detail picture of the
 * display's pixels per picture pixel.
 */
final class PictureRendererTest {
    private static final int RED = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;
    private static final int BLUE = 0xFF0000FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private static final int TEAM_COLOR = 0x2040E0;
    private static final int HALF_MASK = 128;
    private static final int FULL_MASK = 255;
    private static final int STRIPE_WIDTH = 4;
    private static final int STRIPE_HEIGHT = 2;
    private static final int CANVAS_SIZE = 8;
    private static final int SLICED_WIDTH = 7;
    private static final int SLICED_HEIGHT = 5;
    private static final int DETAIL_MULTIPLE = 2;
    private static final int WRONG_DETAIL_SIZE = 3;
    private static final float HALF_SIZE = 0.5f;
    private static final int @NotNull [] SLICE_COLORS = {0xFF010101, 0xFF020202, 0xFF030303,
                                                         0xFF040404, 0xFF050505, 0xFF060606,
                                                         0xFF070707, 0xFF080808, 0xFF090909};
    /**
     * A four by four frame of one pixel borders whose edges and centre have two different pixels along each axis: corners {@code K}, top edge
     * {@code T}, left edge {@code L}, right edge {@code R}, bottom edge {@code B} and centre {@code A B / C D}, in rows.
     */
    private static final int @NotNull [] TILED_COLORS = {0xFF101010, 0xFF111111, 0xFF121212, 0xFF131313,
                                                         0xFF141414, 0xFF151515, 0xFF161616, 0xFF171717,
                                                         0xFF181818, 0xFF191919, 0xFF1A1A1A, 0xFF1B1B1B,
                                                         0xFF1C1C1C, 0xFF1D1D1D, 0xFF1E1E1E, 0xFF1F1F1F};
    private static final int TILED_SIZE = 4;
    private static final int TILED_WIDTH = 7;

    /**
     * Creates a {@link Picture} instance of four columns, red, green, blue and white, two rows high.
     *
     * @return the created {@link Picture} instance
     */
    private static @NotNull Picture createStripes() {
        int[] colors = {RED, GREEN, BLUE, WHITE, RED, GREEN, BLUE, WHITE};

        return new Picture(STRIPE_WIDTH, STRIPE_HEIGHT, PixelLayer.ofRgba(colors));
    }

    private static @NotNull StandardPack openPack(@NotNull Path folder) throws IOException {
        BufferedImage icon = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        icon.setRGB(0, 0, RED);
        ImageIO.write(icon, "png", folder.resolve("icon.png").toFile());
        Files.writeString(folder.resolve("pack.json"), "{\"format\": 1, \"pictures\": {\"icons/red\": \"icon.png\"}}", StandardCharsets.UTF_8);

        return StandardPack.open(folder);
    }

    private static void writePicture(@NotNull Path folder, @NotNull String name, int size, int color) throws IOException {
        BufferedImage picture = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                picture.setRGB(x, y, color);
            }
        }

        ImageIO.write(picture, "png", folder.resolve(name).toFile());
    }

    private static @NotNull BufferedImage draw(@NotNull Path folder, @NotNull Picture picture, float width, float height,
                                               @NotNull PictureScaling scaling) throws IOException {
        ImageCanvas canvas = new ImageCanvas(CANVAS_SIZE, CANVAS_SIZE);

        try (StandardPack pack = openPack(folder);
             PictureRenderer renderer = new PictureRenderer(canvas, pack)) {
            canvas.begin(CANVAS_SIZE, CANVAS_SIZE);
            renderer.draw(canvas, picture, 0, 0, width, height, scaling, PictureRenderer.NO_TEAM_COLOR, PictureRenderer.OPAQUE_WHITE);
            canvas.end();
        }

        return canvas.getImage();
    }

    @Test
    void draw_should_stretch_fit_and_fill_the_rectangle(@TempDir @NotNull Path folder) throws IOException {
        Picture stripes = createStripes();
        BufferedImage stretched = draw(folder, stripes, CANVAS_SIZE, STRIPE_HEIGHT, PictureScaling.STRETCH);
        BufferedImage fitted = draw(folder, stripes, CANVAS_SIZE, CANVAS_SIZE, PictureScaling.FIT);
        BufferedImage filled = draw(folder, stripes, STRIPE_WIDTH, STRIPE_WIDTH, PictureScaling.FILL);

        Assertions.assertEquals(RED, stretched.getRGB(1, 1));
        Assertions.assertEquals(GREEN, stretched.getRGB(2, 0));
        Assertions.assertEquals(WHITE, stretched.getRGB(CANVAS_SIZE - 1, 1));
        Assertions.assertEquals(BLACK, stretched.getRGB(0, STRIPE_HEIGHT), "nothing below the rectangle");

        Assertions.assertEquals(BLACK, fitted.getRGB(0, 1), "the fitted picture is centred vertically");
        Assertions.assertEquals(RED, fitted.getRGB(0, 2));
        Assertions.assertEquals(WHITE, fitted.getRGB(CANVAS_SIZE - 1, CANVAS_SIZE - 3));
        Assertions.assertEquals(BLACK, fitted.getRGB(0, CANVAS_SIZE - 2));

        Assertions.assertEquals(GREEN, filled.getRGB(0, 0), "the filled picture is cut at both sides");
        Assertions.assertEquals(GREEN, filled.getRGB(1, STRIPE_WIDTH - 1));
        Assertions.assertEquals(BLUE, filled.getRGB(STRIPE_WIDTH - 1, 0));
        Assertions.assertEquals(BLACK, filled.getRGB(STRIPE_WIDTH, 0), "nothing beside the rectangle");
    }

    @Test
    void draw_should_mix_the_team_colour_by_the_mask(@TempDir @NotNull Path folder) throws IOException {
        int[] colors = {BLACK, WHITE, RED};
        byte[] mask = {(byte) FULL_MASK, (byte) HALF_MASK, 0};
        Picture portrait = new Picture(colors.length, 1, PixelLayer.ofRgba(colors), PixelLayer.ofAlpha(mask), 0, 0, 0, 0, 0, 0);
        ImageCanvas canvas = new ImageCanvas(CANVAS_SIZE, CANVAS_SIZE);

        try (StandardPack pack = openPack(folder);
             PictureRenderer renderer = new PictureRenderer(canvas, pack)) {
            canvas.begin(CANVAS_SIZE, CANVAS_SIZE);
            renderer.draw(canvas, portrait, 0, 0, colors.length, 1, PictureScaling.STRETCH, TEAM_COLOR, PictureRenderer.OPAQUE_WHITE);
            renderer.draw(canvas, createStripes(), 0, 1, STRIPE_WIDTH, STRIPE_HEIGHT, PictureScaling.STRETCH, TEAM_COLOR,
                          PictureRenderer.OPAQUE_WHITE);
            canvas.end();

            Assertions.assertSame(renderer.getTexture(portrait, TEAM_COLOR), renderer.getTexture(portrait, TEAM_COLOR), "composed once");
        }

        BufferedImage image = canvas.getImage();

        Assertions.assertEquals(BLACK | TEAM_COLOR, image.getRGB(0, 0), "a full mask shows the team colour");
        Assertions.assertEquals(BLACK | TeamColorUtils.mix(WHITE, TEAM_COLOR, HALF_MASK), image.getRGB(1, 0));
        Assertions.assertEquals(RED, image.getRGB(2, 0), "no mask keeps the colour");
        Assertions.assertEquals(RED, image.getRGB(0, 1), "a picture without mask ignores the team colour");
    }

    @Test
    void draw_sliced_should_keep_the_corners_and_stretch_the_edges_and_centre(@TempDir @NotNull Path folder) throws IOException {
        Picture frame = new Picture(3, 3, PixelLayer.ofRgba(SLICE_COLORS), null, 0, 0, 1, 1, 1, 1);
        ImageCanvas canvas = new ImageCanvas(CANVAS_SIZE, CANVAS_SIZE);

        try (StandardPack pack = openPack(folder);
             PictureRenderer renderer = new PictureRenderer(canvas, pack)) {
            canvas.begin(CANVAS_SIZE, CANVAS_SIZE);
            renderer.drawSliced(canvas, frame, 0, 0, SLICED_WIDTH, SLICED_HEIGHT, PictureRenderer.OPAQUE_WHITE);
            renderer.drawSliced(canvas, frame, SLICED_WIDTH, SLICED_HEIGHT, 1, 1, PictureRenderer.OPAQUE_WHITE);
            canvas.end();
        }

        BufferedImage image = canvas.getImage();

        Assertions.assertEquals(SLICE_COLORS[0], image.getRGB(0, 0));
        Assertions.assertEquals(SLICE_COLORS[1], image.getRGB(1, 0));
        Assertions.assertEquals(SLICE_COLORS[1], image.getRGB(SLICED_WIDTH - 2, 0));
        Assertions.assertEquals(SLICE_COLORS[2], image.getRGB(SLICED_WIDTH - 1, 0));
        Assertions.assertEquals(SLICE_COLORS[3], image.getRGB(0, SLICED_HEIGHT / 2));
        Assertions.assertEquals(SLICE_COLORS[4], image.getRGB(SLICED_WIDTH / 2, SLICED_HEIGHT / 2));
        Assertions.assertEquals(SLICE_COLORS[5], image.getRGB(SLICED_WIDTH - 1, 1));
        Assertions.assertEquals(SLICE_COLORS[6], image.getRGB(0, SLICED_HEIGHT - 1));
        Assertions.assertEquals(SLICE_COLORS[7], image.getRGB(SLICED_WIDTH / 2, SLICED_HEIGHT - 1));
        Assertions.assertEquals(SLICE_COLORS[8], image.getRGB(SLICED_WIDTH - 1, SLICED_HEIGHT - 1));
        Assertions.assertEquals(BLACK, image.getRGB(SLICED_WIDTH, 0), "nothing beside the rectangle");
    }

    @Test
    void draw_sliced_should_repeat_the_edges_and_centre_at_their_own_size_when_tiled(@TempDir @NotNull Path folder) throws IOException {
        Picture frame = new Picture(TILED_SIZE, TILED_SIZE, PixelLayer.ofRgba(TILED_COLORS), null, 0, 0, 1, 1, 1, 1);
        ImageCanvas canvas = new ImageCanvas(CANVAS_SIZE, CANVAS_SIZE);

        try (StandardPack pack = openPack(folder);
             PictureRenderer renderer = new PictureRenderer(canvas, pack)) {
            canvas.begin(CANVAS_SIZE, CANVAS_SIZE);
            renderer.drawSliced(canvas, frame, 0, 0, TILED_WIDTH, TILED_WIDTH, SliceFill.TILE, PictureRenderer.OPAQUE_WHITE);
            canvas.end();
        }

        BufferedImage image = canvas.getImage();

        Assertions.assertEquals(TILED_COLORS[0], image.getRGB(0, 0), "the corner keeps its size");
        Assertions.assertEquals(TILED_COLORS[1], image.getRGB(1, 0), "the top edge repeats at its own size");
        Assertions.assertEquals(TILED_COLORS[2], image.getRGB(2, 0));
        Assertions.assertEquals(TILED_COLORS[1], image.getRGB(3, 0));
        Assertions.assertEquals(TILED_COLORS[1], image.getRGB(TILED_WIDTH - 2, 0), "the last repeat is cut");
        Assertions.assertEquals(TILED_COLORS[3], image.getRGB(TILED_WIDTH - 1, 0));
        Assertions.assertEquals(TILED_COLORS[4], image.getRGB(0, 1), "the left edge repeats down its side");
        Assertions.assertEquals(TILED_COLORS[8], image.getRGB(0, 2));
        Assertions.assertEquals(TILED_COLORS[4], image.getRGB(0, 3));
        Assertions.assertEquals(TILED_COLORS[5], image.getRGB(1, 1), "the centre repeats over the rest");
        Assertions.assertEquals(TILED_COLORS[6], image.getRGB(2, 1));
        Assertions.assertEquals(TILED_COLORS[5], image.getRGB(3, 1));
        Assertions.assertEquals(TILED_COLORS[9], image.getRGB(1, 2));
        Assertions.assertEquals(TILED_COLORS[10], image.getRGB(4, 4));
        Assertions.assertEquals(TILED_COLORS[5], image.getRGB(5, 5));
        Assertions.assertEquals(TILED_COLORS[7], image.getRGB(TILED_WIDTH - 1, 1), "the right edge repeats down its side");
        Assertions.assertEquals(TILED_COLORS[13], image.getRGB(1, TILED_WIDTH - 1), "the bottom edge repeats along its side");
        Assertions.assertEquals(TILED_COLORS[14], image.getRGB(2, TILED_WIDTH - 1));
        Assertions.assertEquals(TILED_COLORS[15], image.getRGB(TILED_WIDTH - 1, TILED_WIDTH - 1));
        Assertions.assertEquals(BLACK, image.getRGB(TILED_WIDTH, 0), "nothing beside the rectangle");
    }

    @Test
    void get_picture_should_load_once_and_find_picture_should_skip_missing_pictures(@TempDir @NotNull Path folder) throws IOException {
        ImageCanvas canvas = new ImageCanvas(CANVAS_SIZE, CANVAS_SIZE);

        try (StandardPack pack = openPack(folder);
             PictureRenderer renderer = new PictureRenderer(canvas, pack)) {
            ResourceKey key = ResourceKey.of("icons/red");
            ResourceKey missing = ResourceKey.of("icons/missing");

            Assertions.assertSame(renderer.getPicture(key), renderer.getPicture(key));
            Assertions.assertNull(renderer.findPicture(missing));
            Assertions.assertThrows(ResourceNotFoundException.class, () -> renderer.getPicture(missing));
            Assertions.assertThrows(IllegalArgumentException.class,
                                    () -> renderer.draw(new ImageCanvas(1, 1), renderer.getPicture(key), 0, 0, 1, 1, PictureScaling.FIT,
                                                        PictureRenderer.NO_TEAM_COLOR, PictureRenderer.OPAQUE_WHITE),
                                    "another canvas");
        }
    }

    @Test
    void draw_should_draw_from_the_detail_picture_of_the_pixels_per_picture_pixel(@TempDir @NotNull Path folder) throws IOException {
        ImageCanvas canvas = new ImageCanvas(CANVAS_SIZE * DETAIL_MULTIPLE, CANVAS_SIZE * DETAIL_MULTIPLE);

        writePicture(folder, "icon.png", 1, RED);
        writePicture(folder, "icon-2x.png", DETAIL_MULTIPLE, GREEN);
        writePicture(folder, "wrong-2x.png", WRONG_DETAIL_SIZE, BLUE);
        Files.writeString(folder.resolve("pack.json"), """
                                                       {"format": 1, "pictures": {"icon": "icon.png", "icon-2x": "icon-2x.png", "wrong": "icon.png", "wrong-2x": "wrong-2x.png"}}
                                                       """, StandardCharsets.UTF_8);

        try (StandardPack pack = StandardPack.open(folder);
             PictureRenderer renderer = new PictureRenderer(canvas, pack)) {
            Picture icon = renderer.getPicture(ResourceKey.of("icon"));

            canvas.begin(CANVAS_SIZE, CANVAS_SIZE);
            Assertions.assertEquals(1, renderer.findDrawnMultiple(icon, 1), "one pixel per picture pixel draws the picture itself");
            Assertions.assertEquals(DETAIL_MULTIPLE, renderer.findDrawnMultiple(icon, DETAIL_MULTIPLE), "a picture drawn twice as large");
            canvas.end();

            canvas.begin(UiSurface.of(CANVAS_SIZE * DETAIL_MULTIPLE, CANVAS_SIZE * DETAIL_MULTIPLE, DisplayScale.of(DETAIL_MULTIPLE, 1)));
            Assertions.assertEquals(DETAIL_MULTIPLE, renderer.findDrawnMultiple(icon, 1));
            Assertions.assertEquals(1, renderer.findDrawnMultiple(icon, HALF_SIZE), "at half size the picture itself has the pixels");
            renderer.draw(canvas, icon, 0, 0, 1, 1, PictureScaling.STRETCH, PictureRenderer.NO_TEAM_COLOR, PictureRenderer.OPAQUE_WHITE);
            canvas.end();

            Assertions.assertThrows(IllegalArgumentException.class, () -> renderer.getPicture(ResourceKey.of("wrong")),
                                    "a detail picture of another size");
        }

        Assertions.assertEquals(GREEN, canvas.getImage().getRGB(0, 0));
        Assertions.assertEquals(GREEN, canvas.getImage().getRGB(DETAIL_MULTIPLE - 1, DETAIL_MULTIPLE - 1));
        Assertions.assertEquals(BLACK, canvas.getImage().getRGB(DETAIL_MULTIPLE, 0), "the picture keeps its logical size");
    }
}

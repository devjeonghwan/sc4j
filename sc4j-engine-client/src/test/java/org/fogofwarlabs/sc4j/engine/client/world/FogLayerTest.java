package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.HiddenWindowScenario;
import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.client.projection.IsometricProjection;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.WorldView;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.client.render.gl.GlCanvas;
import org.fogofwarlabs.sc4j.engine.client.render.image.ImageCanvas;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.map.TileCodeGrid;
import org.fogofwarlabs.sc4j.engine.map.TilesetLegend;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.fogofwarlabs.sc4j.engine.resource.graphics.TileGraphics;
import org.fogofwarlabs.sc4j.engine.resource.graphics.TileShape;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;

/**
 * Tests the engine default fog over diamond terrain pictures in the isometric projection: at the map's unexplored corner neither the map nor
 * the void beside it shows any terrain pixel, on the image canvas and on the OpenGL canvas of a hidden window.
 */
final class FogLayerTest {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;
    private static final int TILE_WIDTH = 64;
    private static final int TILE_HEIGHT = 32;
    private static final int GROUND = 0xFF40FF40;
    private static final int BLACK = 0x000000;
    private static final int OPAQUE_BLACK = 0x000000FF;
    private static final int RGB_MASK = 0xFFFFFF;
    private static final int FIELD_PAGE_SIZE = 64;
    private static final int FRAMES = 8;
    private static final float HALF = 2;
    private static final char GROUND_CODE = 'g';

    /**
     * Creates one diamond tile picture of opaque ground, transparent outside the diamond.
     */
    private static @NotNull TileGraphics createDiamondTile() {
        int[] colors = new int[TILE_WIDTH * TILE_HEIGHT];

        for (int y = 0; y < TILE_HEIGHT; y++) {
            for (int x = 0; x < TILE_WIDTH; x++) {
                float across = Math.abs(x + 0.5f - TILE_WIDTH / HALF) / (TILE_WIDTH / HALF);
                float down = Math.abs(y + 0.5f - TILE_HEIGHT / HALF) / (TILE_HEIGHT / HALF);

                colors[y * TILE_WIDTH + x] = across + down <= 1 ? GROUND : 0;
            }
        }

        return new TileGraphics(TILE_WIDTH, TILE_HEIGHT, TileShape.DIAMOND, PixelLayer.ofRgba(colors));
    }

    /**
     * Draws the map's left corner in the middle of the screen with the diamond tiles and the fog, and counts the pixels that are not black.
     */
    private static int countShownPixels(@NotNull Canvas canvas, @NotNull GameView view, @NotNull IntBinaryOperator colors) throws IOException {
        WorldProjection projection = new IsometricProjection(view.getTerrain().getTileSize(), TILE_WIDTH, TILE_HEIGHT);
        List<String> rows = new ArrayList<>();
        WorldViewport viewport = new WorldViewport(projection);
        int shown = 0;

        for (int row = 0; row < view.getTerrain().getHeight(); row++) {
            rows.add(String.valueOf(GROUND_CODE).repeat(view.getTerrain().getWidth()));
        }

        TilesetLegend legend = new TilesetLegend(Map.of(GROUND_CODE, new int[]{0}));

        try (TilesetLayer tiles = new TilesetLayer(canvas, view, DetailSet.of(createDiamondTile()), legend, new TileCodeGrid(rows))) {
            WorldView world = new WorldView(List.of(tiles, new FogLayer(canvas, view)));

            viewport.set(WIDTH, HEIGHT, projection.toProjectedX(0, view.getMapHeight()) - WIDTH / HALF,
                         projection.toProjectedY(0, view.getMapHeight()) - HEIGHT / HALF, 1);
            canvas.begin(WIDTH, HEIGHT);
            canvas.fillRectangle(0, 0, WIDTH, HEIGHT, OPAQUE_BLACK);
            world.draw(viewport);
            canvas.end();
        }

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                shown += (colors.applyAsInt(x, y) & RGB_MASK) == BLACK ? 0 : 1;
            }
        }

        return shown;
    }

    @Test
    void fog_covers_the_terrain_pictures_at_the_unexplored_map_edge_on_the_image_canvas() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        ImageCanvas canvas = new ImageCanvas(WIDTH, HEIGHT);

        skirmish.runFrames(FRAMES);

        Assertions.assertEquals(0, countShownPixels(canvas, skirmish.getSession().getView(), canvas.getImage()::getRGB),
                                "the unexplored corner and the void beside it show no terrain");
    }

    @Test
    void fog_covers_the_terrain_pictures_at_the_unexplored_map_edge_on_the_gpu_canvas() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();

        skirmish.runFrames(FRAMES);

        HiddenWindowScenario.run(WIDTH, HEIGHT, () -> {
            try (GlCanvas canvas = new GlCanvas(FIELD_PAGE_SIZE)) {
                Assertions.assertEquals(0, countShownPixels(canvas, skirmish.getSession().getView(),
                                                            (x, y) -> HiddenWindowScenario.readColor(x, y, HEIGHT)),
                                        "the unexplored corner and the void beside it show no terrain");
            } catch (IOException exception) {
                Assertions.fail("A world layer cannot read a resource.", exception);
            }
        });
    }
}

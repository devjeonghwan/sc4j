package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.client.command.CommandTarget;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.fogofwarlabs.sc4j.engine.client.projection.IsometricProjection;
import org.fogofwarlabs.sc4j.engine.client.projection.SquareProjection;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.image.ImageCanvas;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.world.Camera;
import org.fogofwarlabs.sc4j.engine.client.world.TerrainColors;
import org.fogofwarlabs.sc4j.engine.map.grid.GridTopology;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests the minimap mapping between its rectangle and the map in both engine projections: the rectangle shows the map's projected bounds at their own
 * aspect, centred (a square from above, a diamond twice as wide as high in the isometric projection), a press moves the camera there, the terrain
 * image of the minimap follows the projection, the camera outline shows the world between the HUD's rows cut at the minimap's edges, the local
 * player's fog lies on the terrain tile by tile, and pings move on with the time.
 */
final class MinimapTest {
    private static final float MINIMAP_LEFT = 10;
    private static final float MINIMAP_TOP = 20;
    private static final float MINIMAP_SIZE = 128;
    private static final int ISOMETRIC_TILE_WIDTH = 64;
    private static final int ISOMETRIC_TILE_HEIGHT = 32;
    private static final int SCREEN_SIZE = 400;
    private static final float DELTA = 1;
    private static final int PING_X = 96;
    private static final int PING_Y = 160;
    private static final int PING_COLOR = 0x20C020FF;
    private static final int LARGE_SCREEN_SIZE = 4000;
    private static final int CANVAS_SIZE = 200;
    private static final int WIDE_CANVAS_WIDTH = 300;
    private static final float TOP_INSET = 24;
    private static final float BOTTOM_INSET = 160;
    private static final int RGB_MASK = 0xFFFFFF;
    private static final int CHANNEL_MASK = 0xFF;
    private static final int OPAQUE = 0xFF;
    private static final int CHANNEL_TOLERANCE = 3;
    private static final int RUN_FRAMES = 240;
    private static final float PIXEL_CENTER = 0.5f;
    private static final int SEEN = 0;
    private static final int EXPLORED = 1;
    private static final int UNEXPLORED = 2;
    private static final int FOG_STATES = 3;
    /**
     * The smallest share of the pixels inside the tiles of a fog state, outside the unit dots, that show the tile's look.
     */
    private static final float SMALLEST_MATCHING_SHARE = 0.9f;
    /**
     * The largest difference between the aspect of the drawn map and that of the projected map, from the rounding of its edge pixels.
     */
    private static final float ASPECT_TOLERANCE = 0.1f;
    /**
     * The largest offset in pixels of the drawn diamond's middle row from the rectangle's middle row.
     */
    private static final float DIAMOND_TOLERANCE = 2;
    /**
     * The colour of the canvas under the minimap, which no map pixel has.
     */
    private static final int BACKGROUND_COLOR = 0x336699FF;

    private static @NotNull Minimap createMinimap(@NotNull GameView view, @NotNull Camera camera) {
        Minimap minimap = new Minimap(view, camera, new TerrainColors());

        camera.setScreenSize(SCREEN_SIZE, SCREEN_SIZE);
        minimap.setBounds(MINIMAP_LEFT, MINIMAP_TOP, MINIMAP_SIZE, MINIMAP_SIZE);

        return minimap;
    }

    /**
     * Reads the tile under a pixel's centre of the minimap's map area in a canvas of one pixel per unit, or {@code -1} outside the map.
     */
    private static int findTile(@NotNull GameView view, @NotNull Camera camera, @NotNull Minimap minimap, int x, int y) {
        float scaleX = (camera.getBoundsRight() - camera.getBoundsLeft()) / minimap.getMapAreaWidth();
        float scaleY = (camera.getBoundsBottom() - camera.getBoundsTop()) / minimap.getMapAreaHeight();
        float projectedX = camera.getBoundsLeft() + (x + PIXEL_CENTER - minimap.getMapAreaLeft()) * scaleX;
        float projectedY = camera.getBoundsTop() + (y + PIXEL_CENTER - minimap.getMapAreaTop()) * scaleY;
        int worldX = (int) Math.floor(camera.getProjection().toWorldX(projectedX, projectedY));
        int worldY = (int) Math.floor(camera.getProjection().toWorldY(projectedX, projectedY));
        GridTopology tiles = view.getTerrain().getTileGrid();
        int column = tiles.findColumn(worldX, worldY);
        int row = tiles.findRow(worldX, worldY);

        return worldX >= 0 && worldY >= 0 && tiles.contains(column, row) ? row * tiles.getColumnCount() + column : -1;
    }

    private static boolean isNear(int rgb, int expected) {
        for (int shift = 0; shift <= Short.SIZE; shift += Byte.SIZE) {
            if (Math.abs((rgb >> shift & CHANNEL_MASK) - (expected >> shift & CHANNEL_MASK)) > CHANNEL_TOLERANCE) {
                return false;
            }
        }

        return true;
    }

    private static int dim(int rgb, int fogColor) {
        int alpha = fogColor & OPAQUE;
        int result = 0;

        for (int shift = 0; shift <= Short.SIZE; shift += Byte.SIZE) {
            int channel = rgb >> shift & CHANNEL_MASK;
            int fog = fogColor >>> Byte.SIZE >> shift & CHANNEL_MASK;

            result |= Math.round((channel * (OPAQUE - alpha) + fog * alpha) / (float) OPAQUE) << shift;
        }

        return result;
    }

    @Test
    void minimap_maps_its_rectangle_to_the_map_from_above() throws IOException {
        GameView view = new SkirmishSessionScenario().getSession().getView();
        Camera camera = new Camera(new SquareProjection(), view.getMapWidth(), view.getMapHeight());
        Minimap minimap = createMinimap(view, camera);
        CommandTarget centre = minimap.pick(MINIMAP_LEFT + MINIMAP_SIZE / 2, MINIMAP_TOP + MINIMAP_SIZE / 2);

        Assertions.assertNotNull(centre);
        Assertions.assertEquals(view.getMapWidth() / 2f, centre.getX(), DELTA);
        Assertions.assertEquals(view.getMapHeight() / 2f, centre.getY(), DELTA);
        Assertions.assertEquals(0, minimap.toWorldX(MINIMAP_LEFT, MINIMAP_TOP));
        Assertions.assertEquals(view.getMapWidth() - 1, minimap.toWorldX(MINIMAP_LEFT + MINIMAP_SIZE, MINIMAP_TOP));
        Assertions.assertEquals(view.getTerrain().getWidth(), new TerrainColors().getImageWidth(view.getTerrain(), camera.getProjection()),
                                "one pixel per tile from above");
    }

    @Test
    void minimap_shows_the_isometric_diamond_and_moves_the_camera_there() throws IOException {
        GameView view = new SkirmishSessionScenario().getSession().getView();
        WorldProjection projection = new IsometricProjection(view.getTerrain().getTileSize(), ISOMETRIC_TILE_WIDTH, ISOMETRIC_TILE_HEIGHT);
        Camera camera = new Camera(projection, view.getMapWidth(), view.getMapHeight());
        Minimap minimap = createMinimap(view, camera);
        TerrainColors colors = new TerrainColors();
        int imageWidth = colors.getImageWidth(view.getTerrain(), projection);
        int imageHeight = colors.getImageHeight(view.getTerrain(), projection);
        int[] image = colors.createImage(view.getTerrain(), projection);
        float top = minimap.getMapAreaTop();

        Assertions.assertEquals(0, minimap.toWorldX(MINIMAP_LEFT + MINIMAP_SIZE / 2, top), "the top corner is the world origin");
        Assertions.assertEquals(0, minimap.toWorldY(MINIMAP_LEFT + MINIMAP_SIZE / 2, top));
        Assertions.assertEquals(view.getMapWidth() / 2f, minimap.toWorldX(MINIMAP_LEFT + MINIMAP_SIZE / 2, MINIMAP_TOP + MINIMAP_SIZE / 2), DELTA);
        Assertions.assertEquals(view.getMapHeight() / 2f, minimap.toWorldY(MINIMAP_LEFT + MINIMAP_SIZE / 2, MINIMAP_TOP + MINIMAP_SIZE / 2),
                                DELTA);
        Assertions.assertEquals(0, image[0] >>> Integer.SIZE - Byte.SIZE, "the corner outside the diamond is transparent");
        Assertions.assertNotEquals(0, image[imageHeight / 2 * imageWidth + imageWidth / 2] >>> Integer.SIZE - Byte.SIZE, "the centre is ground");

        minimap.handleInput(InputEvent.createButtonPressed(PointerButton.LEFT, 0,
                                                           MINIMAP_LEFT + MINIMAP_SIZE / 2, MINIMAP_TOP + MINIMAP_SIZE / 2, false));

        Assertions.assertEquals(view.getMapWidth() / 2f, camera.toWorldX(SCREEN_SIZE / 2f, SCREEN_SIZE / 2f), DELTA);
        Assertions.assertEquals(view.getMapHeight() / 2f, camera.toWorldY(SCREEN_SIZE / 2f, SCREEN_SIZE / 2f), DELTA);
    }

    @Test
    void minimap_fills_its_map_area_at_the_aspect_of_the_projected_map_centred_in_its_rectangle() throws IOException {
        GameView view = new SkirmishSessionScenario().getSession().getView();
        WorldProjection projection = new IsometricProjection(view.getTerrain().getTileSize(), ISOMETRIC_TILE_WIDTH, ISOMETRIC_TILE_HEIGHT);
        Camera camera = new Camera(projection, view.getMapWidth(), view.getMapHeight());
        Minimap minimap = createMinimap(view, camera);
        ImageCanvas canvas = new ImageCanvas(CANVAS_SIZE, CANVAS_SIZE);
        float aspect = (camera.getBoundsRight() - camera.getBoundsLeft()) / (camera.getBoundsBottom() - camera.getBoundsTop());
        int cameraColor = minimap.getStyle().getCameraColor() >>> Byte.SIZE;
        int left = CANVAS_SIZE;
        int right = -1;
        int top = CANVAS_SIZE;
        int bottom = -1;

        canvas.begin(CANVAS_SIZE, CANVAS_SIZE);
        canvas.fillRectangle(0, 0, CANVAS_SIZE, CANVAS_SIZE, BACKGROUND_COLOR);
        minimap.draw(canvas);
        canvas.end();

        // The map's pixels are those the minimap drew inside its border other than the camera outline.
        for (int y = (int) MINIMAP_TOP + 1; y < MINIMAP_TOP + MINIMAP_SIZE - 1; y++) {
            for (int x = (int) MINIMAP_LEFT + 1; x < MINIMAP_LEFT + MINIMAP_SIZE - 1; x++) {
                int shown = canvas.getImage().getRGB(x, y) & RGB_MASK;

                if (shown != BACKGROUND_COLOR >>> Byte.SIZE && shown != cameraColor) {
                    left   = Math.min(left, x);
                    right  = Math.max(right, x);
                    top    = Math.min(top, y);
                    bottom = Math.max(bottom, y);
                }
            }
        }

        float shownAspect = (right - left + 1) / (float) (bottom - top + 1);

        Assertions.assertTrue(aspect > 1, "the isometric map is wider than high");
        Assertions.assertEquals(aspect, shownAspect, ASPECT_TOLERANCE, "the drawn map keeps the aspect of the projected map");
        Assertions.assertEquals(MINIMAP_SIZE, minimap.getMapAreaWidth(), DELTA, "the map area takes the rectangle's width");
        Assertions.assertEquals(MINIMAP_SIZE / aspect, minimap.getMapAreaHeight(), DELTA);
        Assertions.assertEquals(MINIMAP_TOP + (MINIMAP_SIZE - minimap.getMapAreaHeight()) / 2, minimap.getMapAreaTop(), DELTA, "the area is centred");
        Assertions.assertEquals(MINIMAP_TOP + MINIMAP_SIZE / 2, (top + bottom + 1) / 2f, DIAMOND_TOLERANCE, "the diamond is centred");
    }

    @Test
    void minimap_lays_the_fog_on_the_terrain_tile_by_tile() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        GameView view = skirmish.getSession().getView();
        WorldProjection projection = new IsometricProjection(view.getTerrain().getTileSize(), ISOMETRIC_TILE_WIDTH, ISOMETRIC_TILE_HEIGHT);
        Camera camera = new Camera(projection, view.getMapWidth(), view.getMapHeight());
        Minimap minimap = createMinimap(view, camera);
        TerrainColors colors = new TerrainColors();
        GridTopology tiles = view.getTerrain().getTileGrid();
        ImageCanvas canvas = new ImageCanvas(WIDE_CANVAS_WIDTH, CANVAS_SIZE);
        Player player = view.getLocalPlayer();
        int[] counts = new int[FOG_STATES];
        int[] matches = new int[FOG_STATES];
        Set<Integer> teamColors = new HashSet<>();

        Assertions.assertNotNull(player);
        skirmish.runFrames(RUN_FRAMES);

        for (Player owner : view.getPlayers()) {
            teamColors.add(view.getTeamColor(owner));
        }

        // A rectangle twice as wide as high gives the diamond's tiles two pixel rows each.
        minimap.setBounds(MINIMAP_LEFT, MINIMAP_TOP, 2 * MINIMAP_SIZE, MINIMAP_SIZE);
        camera.setScreenSize(LARGE_SCREEN_SIZE, LARGE_SCREEN_SIZE);
        canvas.begin(WIDE_CANVAS_WIDTH, CANVAS_SIZE);
        minimap.draw(canvas);
        canvas.end();

        for (int y = (int) MINIMAP_TOP + 1; y < MINIMAP_TOP + MINIMAP_SIZE - 1; y++) {
            for (int x = (int) MINIMAP_LEFT + 1; x < MINIMAP_LEFT + 2 * MINIMAP_SIZE - 1; x++) {
                int tile = findTile(view, camera, minimap, x, y);

                if (tile < 0 || findTile(view, camera, minimap, x - 1, y) != tile || findTile(view, camera, minimap, x + 1, y) != tile ||
                    findTile(view, camera, minimap, x, y - 1) != tile || findTile(view, camera, minimap, x, y + 1) != tile) {
                    continue;
                }

                int shown = canvas.getImage().getRGB(x, y) & RGB_MASK;

                if (teamColors.contains(shown)) {
                    continue;
                }

                int column = tile % tiles.getColumnCount();
                int row = tile / tiles.getColumnCount();
                int centerX = tiles.getCenterX(column, row);
                int centerY = tiles.getCenterY(column, row);
                int ground = colors.getColor(view.getTerrain(), column, row) & RGB_MASK;
                int state = view.isVisible(player, centerX, centerY) ? SEEN : view.isExplored(player, centerX, centerY) ? EXPLORED : UNEXPLORED;
                int expected = state == SEEN ? ground : dim(ground, minimap.getStyle().getFogColor(state == EXPLORED));

                counts[state]++;
                matches[state] += isNear(shown, expected) ? 1 : 0;
            }
        }

        Assertions.assertTrue(counts[SEEN] > 0 && counts[UNEXPLORED] > 0, "the player sees some tiles and has not explored others");

        for (int state = 0; state < counts.length; state++) {
            Assertions.assertTrue(matches[state] >= counts[state] * SMALLEST_MATCHING_SHARE,
                                  "fog state " + state + ": " + matches[state] + " of " + counts[state] + " pixels show the tile's look");
        }
    }

    @Test
    void minimap_outlines_the_world_between_the_hud_rows_cut_at_its_edges() throws IOException {
        GameView view = new SkirmishSessionScenario().getSession().getView();
        WorldProjection projection = new IsometricProjection(view.getTerrain().getTileSize(), ISOMETRIC_TILE_WIDTH, ISOMETRIC_TILE_HEIGHT);
        Camera camera = new Camera(projection, view.getMapWidth(), view.getMapHeight());
        Minimap minimap = createMinimap(view, camera);
        ImageCanvas canvas = new ImageCanvas(CANVAS_SIZE, CANVAS_SIZE);
        int cameraColor = minimap.getStyle().getCameraColor() >>> Byte.SIZE;
        float scaleY = minimap.getMapAreaHeight() / (camera.getBoundsBottom() - camera.getBoundsTop());
        int outsideCount = 0;

        camera.setScreenSize(LARGE_SCREEN_SIZE, LARGE_SCREEN_SIZE);
        canvas.begin(CANVAS_SIZE, CANVAS_SIZE);
        minimap.draw(canvas);
        canvas.end();

        for (int y = 0; y < CANVAS_SIZE; y++) {
            for (int x = 0; x < CANVAS_SIZE; x++) {
                boolean inside = x >= MINIMAP_LEFT && x < MINIMAP_LEFT + MINIMAP_SIZE && y >= MINIMAP_TOP && y < MINIMAP_TOP + MINIMAP_SIZE;

                outsideCount += !inside && (canvas.getImage().getRGB(x, y) & RGB_MASK) == cameraColor ? 1 : 0;
            }
        }

        Assertions.assertEquals(0, outsideCount, "the outline of a screen larger than the map is cut at the minimap's edges");

        camera.setScreenSize(SCREEN_SIZE, SCREEN_SIZE);
        camera.centerOn(view.getMapWidth() / 2f, view.getMapHeight() / 2f);
        minimap.setViewInsets(TOP_INSET, BOTTOM_INSET);
        canvas.begin(CANVAS_SIZE, CANVAS_SIZE);
        minimap.draw(canvas);
        canvas.end();

        int outlineTop = (int) Math.floor(minimap.getMapAreaTop() + (camera.getY() + TOP_INSET / camera.getZoom() - camera.getBoundsTop()) * scaleY);
        int screenTop = (int) Math.floor(minimap.getMapAreaTop() + (camera.getY() - camera.getBoundsTop()) * scaleY);

        Assertions.assertEquals(cameraColor, canvas.getImage().getRGB((int) (MINIMAP_LEFT + MINIMAP_SIZE / 2), outlineTop) & RGB_MASK,
                                "the outline's top edge lies below the resource bar's rows");
        Assertions.assertNotEquals(cameraColor, canvas.getImage().getRGB((int) (MINIMAP_LEFT + MINIMAP_SIZE / 2), screenTop) & RGB_MASK,
                                   "the outline leaves out the resource bar");
    }

    @Test
    void update_should_move_pings_on_by_the_frame_interval() throws IOException {
        GameView view = new SkirmishSessionScenario().getSession().getView();
        Minimap minimap = createMinimap(view, new Camera(new SquareProjection(), view.getMapWidth(), view.getMapHeight()));

        minimap.addPing(PING_X, PING_Y, PING_COLOR);
        minimap.update(Minimap.PING_FRAME_INTERVAL - 1);

        Assertions.assertEquals(0, minimap.getPings().getFrame(0));
        Assertions.assertEquals(PING_X, minimap.getPings().getX(0));

        minimap.update(1);

        Assertions.assertEquals(1, minimap.getPings().getFrame(0));

        minimap.addPing(PING_X, PING_Y, new Player(1));

        Assertions.assertTrue(minimap.getPings().isPlayerColor(1), "a player's ping shows the player's colour");
        Assertions.assertEquals(1, minimap.getPings().getColor(1));
        Assertions.assertFalse(minimap.getPings().isPlayerColor(0));

        minimap.update(Minimap.PING_FRAME_INTERVAL * Minimap.PING_FRAMES);

        Assertions.assertFalse(minimap.getPings().isAnyActive(), "the ping ends after its frames");
        Assertions.assertInstanceOf(StandardMinimapStyle.class, minimap.getStyle());
    }
}

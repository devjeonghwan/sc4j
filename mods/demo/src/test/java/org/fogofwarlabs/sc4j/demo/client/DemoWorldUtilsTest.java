package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.demo.game.DemoGameScenario;
import org.fogofwarlabs.sc4j.demo.game.DemoGames;
import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.client.display.DisplayScale;
import org.fogofwarlabs.sc4j.engine.client.display.UiSurface;
import org.fogofwarlabs.sc4j.engine.client.projection.IsometricProjection;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.WorldView;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.client.render.image.ImageCanvas;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.StandardGameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.client.world.*;
import org.fogofwarlabs.sc4j.engine.combat.StandardCombatSystem;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.map.MapModel;
import org.fogofwarlabs.sc4j.engine.resource.graphics.TileGraphics;
import org.fogofwarlabs.sc4j.engine.resource.graphics.TileShape;
import org.fogofwarlabs.sc4j.engine.spatial.TableUnitBounds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the demo's world view: Ashen Ford in the isometric projection with its diamond tileset and a few units, drawn offscreen through the
 * engine world layers, a building under construction drawn with the demo's sprite art, and the tiles and sprites drawn from their detail
 * pictures on a scaled display; the frames are saved as sample pictures under the build folder.
 */
final class DemoWorldUtilsTest {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private static final int TILE_WIDTH = 64;
    private static final int TILE_HEIGHT = 32;
    /**
     * How many tiles right of and below the view's centre the ground check looks.
     */
    private static final int GROUND_TILE_OFFSET = 3;
    private static final int FRAMES = 1200;
    private static final long SEED = 7;
    private static final int @NotNull [] TEAM_COLORS = {0x3C78D8, 0xD8503C, 0xC8C8C8};
    private static final int CHANNEL_MASK = 0xFF;
    private static final int OPAQUE_BLACK = 0x000000FF;
    private static final int RED_SHIFT = 16;
    private static final int ALPHA_SHIFT = 24;
    private static final @NotNull String SAMPLE = "build/demo-frames/ashen-ford-isometric.png";
    private static final @NotNull String CONSTRUCTION_SAMPLE = "build/demo-frames/construction.png";
    private static final @NotNull String SCALED_SAMPLE = "build/demo-frames/world-scale-%d.png";
    private static final int PERCENT_SCALE = 100;
    private static final @NotNull String SIZE_SEPARATOR = "x";
    private static final int CONSTRUCTION_FRAME_LIMIT = 12000;
    private static final int SHOWN_CONSTRUCTION_PROGRESS = UnitView.COMPLETE * 2 / 3;
    private static final int WALL_HEIGHT = 12;
    private static final @NotNull List<@NotNull String> TOWN_HALLS = List.of("hearthstead", "ember-lodge");

    private static @NotNull Unit findTownHall(@NotNull Game game) {
        for (String typeId : TOWN_HALLS) {
            Unit unit = DemoGameScenario.findUnit(game, game.getPlayer(DemoGames.FIRST_SLOT), typeId);

            if (unit != null) {
                return unit;
            }
        }

        return Assertions.fail("The first player has no town hall.");
    }

    private static @Nullable UnitView findLateConstruction(@NotNull Game game, @NotNull GameView view) {
        List<Unit> units = view.getUnits();

        for (Unit shownUnit : units) {
            UnitView unit = view.getUnitView(shownUnit);
            UnitType type = unit.getType();

            if (type != null && game.getContent().getUnitTypes().hasTrait(type, UnitTrait.BUILDING) &&
                unit.getConstructionProgress() >= SHOWN_CONSTRUCTION_PROGRESS && unit.getConstructionProgress() < UnitView.COMPLETE) {
                return unit;
            }
        }

        return null;
    }

    private static @NotNull BufferedImage drawWorld(@NotNull WorldViewport viewport, @NotNull ImageCanvas canvas,
                                                    @NotNull WorldView world) throws IOException {
        canvas.begin(WIDTH, HEIGHT);
        canvas.fillRectangle(0, 0, WIDTH, HEIGHT, OPAQUE_BLACK);
        world.draw(viewport);
        canvas.end();

        BufferedImage image = canvas.getImage();
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        copy.setData(image.getData());

        return copy;
    }

    @Test
    void ashen_ford_draws_in_the_isometric_projection_with_its_diamond_tiles_and_units() throws IOException {
        try (DemoGameScenario scenario = new DemoGameScenario()) {
            GameSetup setup = scenario.createSetup(SEED);
            Game game = scenario.getGames().createGame(setup);
            MapModel map = scenario.getMaps().require(setup);
            DetailSet<TileGraphics> tiles = DemoWorldUtils.loadTileGraphics(scenario.getContent(), map);
            WorldProjection projection = DemoWorldUtils.createProjection(map, tiles);

            Assertions.assertEquals(TileShape.DIAMOND, tiles.getResource().getShape());
            Assertions.assertEquals(TILE_WIDTH, tiles.getResource().getTileWidth());
            Assertions.assertEquals(TILE_HEIGHT, tiles.getResource().getTileHeight());
            Assertions.assertInstanceOf(IsometricProjection.class, projection);

            for (int frame = 0; frame < FRAMES && !DemoGames.isOver(game); frame++) {
                game.getFrameRunner().runFrame();
            }

            GameView view = new StandardGameView(game, null, map.getTerrain(), game.getModSystem(TableUnitBounds.KEY),
                                                 StandardCombatSystem.FRACTION_BITS, scenario.getContent().getResourceKinds(), TEAM_COLORS);
            Unit townHall = findTownHall(game);
            UnitView townHallView = view.getUnitView(townHall);
            ImageCanvas canvas = new ImageCanvas(WIDTH, HEIGHT);
            Camera camera = new Camera(projection, view.getMapWidth(), view.getMapHeight());
            WorldViewport viewport = new WorldViewport(projection);

            camera.setScreenSize(WIDTH, HEIGHT);
            camera.centerOn(townHallView.getX(), townHallView.getY());
            camera.applyTo(viewport);

            try (TilesetLayer terrain = DemoWorldUtils.createTerrainLayer(canvas, view, scenario.getContent(), map, tiles)) {
                WorldView world = new WorldView(List.of(terrain, new UnitLayer(canvas, view)));

                canvas.begin(WIDTH, HEIGHT);
                canvas.fillRectangle(0, 0, WIDTH, HEIGHT, OPAQUE_BLACK);
                world.draw(viewport);
                canvas.end();
            }

            BufferedImage frame = canvas.getImage();
            int centerColor = frame.getRGB((int) viewport.worldToScreenX(townHallView.getX(), townHallView.getY()),
                                           (int) viewport.worldToScreenY(townHallView.getX(), townHallView.getY()));
            int groundColor = frame.getRGB(WIDTH / 2 + TILE_WIDTH * GROUND_TILE_OFFSET, HEIGHT / 2 + TILE_HEIGHT * GROUND_TILE_OFFSET);

            Assertions.assertEquals(CHANNEL_MASK, centerColor >>> ALPHA_SHIFT, "the town hall's footprint is drawn");
            Assertions.assertTrue((centerColor & CHANNEL_MASK) > (centerColor >> RED_SHIFT & CHANNEL_MASK), "in the first player's blue");
            Assertions.assertEquals(CHANNEL_MASK, groundColor >>> ALPHA_SHIFT, "the tiles cover the ground");

            Path sample = Path.of(SAMPLE);

            Files.createDirectories(sample.getParent());
            Assertions.assertTrue(ImageIO.write(frame, "png", sample.toFile()));
        }
    }

    @ParameterizedTest
    @ValueSource(floats = {1.0f, 2.0f})
    void the_world_draws_its_tiles_and_sprites_from_their_detail_pictures_at_a_display_scale(float scale) throws IOException {
        try (DemoGameScenario scenario = new DemoGameScenario()) {
            GameSetup setup = scenario.createSetup(SEED);
            Game game = scenario.getGames().createGame(setup);
            MapModel map = scenario.getMaps().require(setup);
            DetailSet<TileGraphics> tiles = DemoWorldUtils.loadTileGraphics(scenario.getContent(), map);
            WorldProjection projection = DemoWorldUtils.createProjection(map, tiles);
            GameView view = new StandardGameView(game, null, map.getTerrain(), game.getModSystem(TableUnitBounds.KEY),
                                                 StandardCombatSystem.FRACTION_BITS, scenario.getContent().getResourceKinds(), TEAM_COLORS);
            DemoSprites sprites = DemoSprites.load(scenario.getContent(), game.getDefinition().getContent());
            UnitView townHall = view.getUnitView(findTownHall(game));
            int multiple = Math.round(scale);
            ImageCanvas image = new ImageCanvas(WIDTH * multiple, HEIGHT * multiple);
            List<String> uploads = new ArrayList<>();
            Canvas canvas = (Canvas) Proxy.newProxyInstance(Canvas.class.getClassLoader(), new Class<?>[]{Canvas.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("uploadTexture")) {
                    uploads.add(arguments[0] + SIZE_SEPARATOR + arguments[1]);
                }

                return method.invoke(image, arguments);
            });
            Camera camera = new Camera(projection, view.getMapWidth(), view.getMapHeight());
            WorldViewport viewport = new WorldViewport(projection);
            UnitType townHallType = townHall.getType();

            Assertions.assertNotNull(townHallType);

            UnitSprite townHallSprite = sprites.findSprite(townHallType);

            Assertions.assertNotNull(townHallSprite);
            camera.setScreenSize(WIDTH, HEIGHT);
            camera.centerOn(townHall.getX(), townHall.getY());
            camera.applyTo(viewport);

            try (TilesetLayer terrain = DemoWorldUtils.createTerrainLayer(canvas, view, scenario.getContent(), map, tiles);
                 UnitSpriteLayer units = new UnitSpriteLayer(canvas, view, sprites::findSprite)) {
                canvas.begin(UiSurface.of(WIDTH * multiple, HEIGHT * multiple, DisplayScale.of(scale, 1)));
                canvas.fillRectangle(0, 0, WIDTH, HEIGHT, OPAQUE_BLACK);
                new WorldView(List.of(terrain, units)).draw(viewport);
                canvas.end();
            }

            String tile = TILE_WIDTH * multiple + SIZE_SEPARATOR + TILE_HEIGHT * multiple;
            String townHallFrame = townHallSprite.getSheet().getWidth() * multiple + SIZE_SEPARATOR +
                                   townHallSprite.getSheet().getHeight() * multiple;

            Assertions.assertTrue(uploads.contains(tile), "the tiles come from the pictures of the scale: " + uploads);
            Assertions.assertTrue(uploads.contains(townHallFrame), "the town hall comes from the sheet of the scale: " + uploads);
            Assertions.assertEquals(scale > 1, !uploads.contains(TILE_WIDTH + SIZE_SEPARATOR + TILE_HEIGHT), "no 1x tile on a 2x display");
            Assertions.assertEquals(1, tiles.getDetailCount());
            Assertions.assertEquals(1, townHallSprite.getSheets().getDetailCount());

            Path sample = Path.of(SCALED_SAMPLE.formatted(Math.round(scale * PERCENT_SCALE)));

            Files.createDirectories(sample.getParent());
            Assertions.assertTrue(ImageIO.write(image.getImage(), "png", sample.toFile()));
        }
    }

    @Test
    void a_building_under_construction_draws_its_isometric_art_on_its_footprint() throws IOException {
        try (DemoGameScenario scenario = new DemoGameScenario()) {
            GameSetup setup = scenario.createSetup(SEED);
            Game game = scenario.getGames().createGame(setup);
            MapModel map = scenario.getMaps().require(setup);
            DetailSet<TileGraphics> tiles = DemoWorldUtils.loadTileGraphics(scenario.getContent(), map);
            WorldProjection projection = DemoWorldUtils.createProjection(map, tiles);
            GameView view = new StandardGameView(game, null, map.getTerrain(), game.getModSystem(TableUnitBounds.KEY),
                                                 StandardCombatSystem.FRACTION_BITS, scenario.getContent().getResourceKinds(), TEAM_COLORS);
            DemoSprites sprites = DemoSprites.load(scenario.getContent(), game.getDefinition().getContent());
            UnitView building = null;

            for (int frame = 0; frame < CONSTRUCTION_FRAME_LIMIT && building == null && !DemoGames.isOver(game); frame++) {
                game.getFrameRunner().runFrame();
                building = findLateConstruction(game, view);
            }

            Assertions.assertNotNull(building, "a computer player builds");

            ImageCanvas canvas = new ImageCanvas(WIDTH, HEIGHT);
            Camera camera = new Camera(projection, view.getMapWidth(), view.getMapHeight());
            WorldViewport viewport = new WorldViewport(projection);

            camera.setScreenSize(WIDTH, HEIGHT);
            camera.centerOn(building.getX(), building.getY());
            camera.applyTo(viewport);

            try (TilesetLayer terrain = DemoWorldUtils.createTerrainLayer(canvas, view, scenario.getContent(), map, tiles);
                 UnitSpriteLayer units = new UnitSpriteLayer(canvas, view, sprites::findSprite)) {
                BufferedImage ground = drawWorld(viewport, canvas, new WorldView(List.of(terrain)));
                BufferedImage frame = drawWorld(viewport, canvas, new WorldView(List.of(terrain, units)));
                int wallX = (int) viewport.worldToScreenX(building.getX(), building.getY());
                int wallY = (int) viewport.worldToScreenY(building.getX(), building.getY()) - WALL_HEIGHT;

                Assertions.assertNotEquals(ground.getRGB(wallX, wallY), frame.getRGB(wallX, wallY), "the building stands on its footprint");

                Path sample = Path.of(CONSTRUCTION_SAMPLE);

                Files.createDirectories(sample.getParent());
                Assertions.assertTrue(ImageIO.write(frame, "png", sample.toFile()));
            }
        }
    }
}

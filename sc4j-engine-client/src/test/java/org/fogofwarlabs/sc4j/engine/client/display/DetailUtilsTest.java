package org.fogofwarlabs.sc4j.engine.client.display;

import org.fogofwarlabs.sc4j.engine.pack.StandardPack;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.fogofwarlabs.sc4j.engine.resource.ResourceNotFoundException;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.fogofwarlabs.sc4j.engine.resource.graphics.TileGraphics;
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
import java.util.List;

/**
 * Tests loading the detail versions of pictures, sprite sheets and tilesets from a standard pack: found under {@code <id>-<n>x}, checked to
 * place everything at their multiple, and detail sprite sheets kept out of the pack's animation sets.
 */
final class DetailUtilsTest {
    private static final int OPAQUE_RED = 0xFFFF0000;
    private static final int FRAME_WIDTH = 4;
    private static final int FRAME_HEIGHT = 3;
    private static final int COLUMNS = 2;
    private static final int TILE_WIDTH = 4;
    private static final int TILE_HEIGHT = 2;
    private static final int TILE_COUNT = 2;
    private static final int CURSOR_SIZE = 3;
    private static final int MULTIPLE = 2;
    private static final @NotNull String MANIFEST = """
                                                    {
                                                      "format": 1,
                                                      "spriteSheets": {"walker": "walker.json", "plain": "walker.json", "skewed": "walker.json"},
                                                      "spriteSheetDetails": {"walker-2x": "walker-2x.json", "skewed-2x": "skewed-2x.json"},
                                                      "tilesets": {"meadow": "meadow.json", "meadow-2x": "meadow-2x.json"},
                                                      "pictures": {"cursor": "cursor.json", "cursor-2x": "cursor-2x.json", "moved": "cursor.json", "moved-2x": "moved-2x.json"}
                                                    }
                                                    """;

    private static void writeImage(@NotNull Path folder, @NotNull String name, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, OPAQUE_RED);
            }
        }

        ImageIO.write(image, "png", folder.resolve(name).toFile());
    }

    private static void writeSheet(@NotNull Path folder, @NotNull String name, int multiple, int anchorX) throws IOException {
        writeImage(folder, name + ".png", FRAME_WIDTH * multiple * COLUMNS, FRAME_HEIGHT * multiple);
        Files.writeString(folder.resolve(name + ".json"), """
                                                          {"image": "%s.png", "frameWidth": %d, "frameHeight": %d, "columns": %d, "rows": 1, "anchor": {"x": %d, "y": %d}}
                                                          """.formatted(name, FRAME_WIDTH * multiple, FRAME_HEIGHT * multiple, COLUMNS, anchorX, multiple), StandardCharsets.UTF_8);
    }

    private static @NotNull StandardPack openPack(@NotNull Path folder) throws IOException {
        writeSheet(folder, "walker", 1, 1);
        writeSheet(folder, "walker-2x", MULTIPLE, MULTIPLE);
        writeSheet(folder, "skewed-2x", MULTIPLE, 1);
        writeImage(folder, "meadow.png", TILE_WIDTH * TILE_COUNT, TILE_HEIGHT);
        writeImage(folder, "meadow-2x.png", TILE_WIDTH * MULTIPLE * TILE_COUNT, TILE_HEIGHT * MULTIPLE);
        Files.writeString(folder.resolve("meadow.json"), """
                                                         {"image": "meadow.png", "tileWidth": %d, "tileHeight": %d, "shape": "diamond", "tiles": {".": [0, 1]}}
                                                         """.formatted(TILE_WIDTH, TILE_HEIGHT), StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("meadow-2x.json"), """
                                                            {"image": "meadow-2x.png", "tileWidth": %d, "tileHeight": %d, "shape": "diamond"}
                                                            """.formatted(TILE_WIDTH * MULTIPLE, TILE_HEIGHT * MULTIPLE), StandardCharsets.UTF_8);
        writeImage(folder, "cursor.png", CURSOR_SIZE, CURSOR_SIZE);
        writeImage(folder, "cursor-2x.png", CURSOR_SIZE * MULTIPLE, CURSOR_SIZE * MULTIPLE);
        Files.writeString(folder.resolve("cursor.json"), "{\"image\": \"cursor.png\", \"anchor\": {\"x\": 1, \"y\": 1}}", StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("cursor-2x.json"), "{\"image\": \"cursor-2x.png\", \"anchor\": {\"x\": 2, \"y\": 2}}", StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("moved-2x.json"), "{\"image\": \"cursor-2x.png\", \"anchor\": {\"x\": 1, \"y\": 2}}", StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("pack.json"), MANIFEST, StandardCharsets.UTF_8);

        return StandardPack.open(folder);
    }

    @Test
    void load_sprite_sheets_should_find_the_detail_sheets_outside_the_animation_sets(@TempDir @NotNull Path folder) throws IOException {
        try (StandardPack pack = openPack(folder)) {
            DetailSet<SpriteSheet> walker = DetailUtils.loadSpriteSheets(pack, ResourceKey.of("walker"));
            DetailSet<SpriteSheet> plain = DetailUtils.loadSpriteSheets(pack, ResourceKey.of("plain"));
            SpriteSheet detail = walker.getDetail(0);

            Assertions.assertEquals(List.of("walker", "plain", "skewed"), pack.getSpriteSheetIds(), "a detail sheet has no animation set");
            Assertions.assertEquals(1, walker.getDetailCount());
            Assertions.assertEquals(MULTIPLE, walker.getMultiple(0));
            Assertions.assertEquals(FRAME_WIDTH * MULTIPLE, detail.getFrame(1).getWidth());
            Assertions.assertEquals(MULTIPLE, detail.getAnchorX());
            Assertions.assertSame(detail, walker.find(MULTIPLE));
            Assertions.assertEquals(0, plain.getDetailCount(), "a sheet without detail sheets");
            Assertions.assertThrows(IllegalArgumentException.class, () -> DetailUtils.loadSpriteSheets(pack, ResourceKey.of("skewed")),
                                    "a detail sheet whose anchor is not twice the sheet's");
            Assertions.assertThrows(ResourceNotFoundException.class, () -> DetailUtils.loadSpriteSheets(pack, ResourceKey.of("missing")));
        }
    }

    @Test
    void load_tile_graphics_and_load_pictures_should_find_the_detail_versions(@TempDir @NotNull Path folder) throws IOException {
        try (StandardPack pack = openPack(folder)) {
            DetailSet<TileGraphics> meadow = DetailUtils.loadTileGraphics(pack, ResourceKey.of("meadow"));
            DetailSet<Picture> cursor = DetailUtils.loadPictures(pack, ResourceKey.of("cursor"));

            Assertions.assertEquals(TILE_WIDTH * MULTIPLE, meadow.getDetail(0).getTileWidth());
            Assertions.assertEquals(TILE_COUNT, meadow.getDetail(0).getTileCount());
            Assertions.assertEquals(MULTIPLE, cursor.getDetail(0).getAnchorX());
            Assertions.assertSame(cursor.getResource(), cursor.find(1));
            Assertions.assertThrows(IllegalArgumentException.class, () -> DetailUtils.loadPictures(pack, ResourceKey.of("moved")),
                                    "a detail picture whose hot spot is not twice the picture's");
        }
    }
}

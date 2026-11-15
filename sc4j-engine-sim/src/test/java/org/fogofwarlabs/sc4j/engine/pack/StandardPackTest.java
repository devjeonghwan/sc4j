package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.engine.animation.*;
import org.fogofwarlabs.sc4j.engine.animation.instruction.*;
import org.fogofwarlabs.sc4j.engine.map.TilesetLegend;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.fogofwarlabs.sc4j.engine.resource.ResourceNotFoundException;
import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.fogofwarlabs.sc4j.engine.resource.dialog.ControlKind;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogDefinition;
import org.fogofwarlabs.sc4j.engine.resource.dialog.TextAlignment;
import org.fogofwarlabs.sc4j.engine.resource.graphics.*;
import org.fogofwarlabs.sc4j.engine.resource.text.FontFace;
import org.fogofwarlabs.sc4j.engine.resource.text.FontGlyph;
import org.fogofwarlabs.sc4j.engine.resource.text.StringTable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Tests reading the engine standard data pack: sprite sheet layers, the JSON animations with their labels and keywords,
 * tilesets with their tile shape and tile codes, fonts from glyph sheets, pictures with their mask, anchor and borders, the other resources, zip
 * packs, mod instruction types and the failures of broken packs.
 */
final class StandardPackTest {
    private static final @NotNull String MANIFEST = """
                                                    {"format": 1, "animationKinds": ["idle", "death"],
                                                     "spriteSheets": {"knight": "units/knight.json", "smoke": "fx/smoke.json"},
                                                     "sounds": {"click": "sound/click.wav"}, "palettes": {"gray": "palette.json"}, "stringTables": {"en": "text/en.json"}}
                                                    """;
    private static final @NotNull String KNIGHT_ANIMATIONS = """
                                                             "idle": [{"frame": 0}, {"waitRandom": [2, 4]}, {"randomJump": "fidget", "chance": 32}, {"jump": "idle"}],
                                                             "fidget": [{"call": "flourish"}, {"jump": "idle"}],
                                                             "death": [{"label": "fall"}, {"frame": 1}, {"sound": "click"}, {"spawnOverlay": "smoke", "y": -3}, {"attack": "secondary"}, {"end": true}],
                                                             "flourish": [{"frame": 1}, {"return": true}]
                                                             """;
    private static final @NotNull String SMOKE_DEFINITION = """
                                                            {"image": "smoke.png", "frameWidth": 2, "frameHeight": 2, "columns": 2, "rows": 1, "anchor": {"x": 1, "y": 1}}
                                                            """;
    private static final int FRAME_SIZE = 2;
    private static final int COLUMNS = 2;
    private static final int OPAQUE = 0xFF000000;
    private static final int MASK_VALUE = 200;
    private static final int SHADOW_ALPHA = 115;
    private static final int SAMPLE_RATE = 8000;
    private static final int TILE_SIZE = 4;
    private static final int TILE_COLUMNS = 2;
    private static final int TILE_ROWS = 2;
    private static final int GLYPH_CELL = 4;
    private static final int GLYPH_ALPHA = 0xC0;
    private static final int SPACE_ADVANCE = 3;
    private static final short @NotNull [] SAMPLES = {0, 1000, -1000, Short.MAX_VALUE};

    /**
     * A mod instruction that ends the program; the pack reads it from {@code {"vanish": true}}.
     */
    private static final class Vanish extends AnimationInstruction {
        @Override
        public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
            return AnimationStep.END;
        }
    }

    /**
     * Reads {@link Vanish}.
     */
    private static final class VanishType implements PackInstructionType {
        private final @NotNull String keyword;

        private VanishType(@NotNull String keyword) {
            this.keyword = keyword;
        }

        @Override
        public @NotNull String getKeyword() {
            return this.keyword;
        }

        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            reader.requireTrueValue();

            return new Vanish();
        }
    }

    @Test
    void load_sprite_sheet_should_read_colour_mask_and_shadow_frames(@TempDir @NotNull Path folder) throws IOException {
        writePack(folder, KNIGHT_ANIMATIONS);

        try (StandardPack pack = StandardPack.open(folder)) {
            SpriteSheet sheet = pack.loadSpriteSheet(ResourceKey.of("knight"));
            PixelLayer teamMask = sheet.getTeamMask();
            PixelLayer shadow = sheet.getShadow();
            int secondFrame = sheet.getFrame(1).getPixelOffset();

            Assertions.assertNotNull(teamMask);
            Assertions.assertNotNull(shadow);
            Assertions.assertEquals(COLUMNS, sheet.getFrameCount());
            Assertions.assertEquals(1, sheet.getAnchorX());
            Assertions.assertEquals(FRAME_SIZE * FRAME_SIZE, secondFrame);
            Assertions.assertEquals(OPAQUE | 1, sheet.getColor().getValue(secondFrame));
            Assertions.assertEquals(MASK_VALUE, teamMask.getValue(secondFrame));
            Assertions.assertEquals(0, teamMask.getValue(0));
            Assertions.assertEquals(SHADOW_ALPHA, shadow.getValue(secondFrame + 1));
            Assertions.assertNull(pack.loadSpriteSheet(ResourceKey.of("smoke")).getTeamMask());
        }
    }

    @Test
    void load_animations_should_resolve_animation_names_inline_labels_and_ids(@TempDir @NotNull Path folder) throws IOException {
        writePack(folder, KNIGHT_ANIMATIONS);

        try (StandardPack pack = StandardPack.open(folder)) {
            AnimationLibrary library = pack.loadAnimations();
            AnimationSet knight = library.getSet(0);
            AnimationKind death = library.findKind("death");
            AnimationProgram program = knight.getProgram(0);

            Assertions.assertNotNull(death);
            Assertions.assertNotNull(program);
            Assertions.assertSame(library, pack.loadAnimations());
            Assertions.assertEquals(List.of("knight", "smoke"), List.of(knight.getName(), library.getSet(1).getName()));
            Assertions.assertNull(library.getSet(1).getProgram(0));
            Assertions.assertEquals(0, knight.getStartPosition(0));
            Assertions.assertEquals(6, knight.getStartPosition(death));
            Assertions.assertEquals(0, ((Jump) program.get(5)).getTargetPosition());
            Assertions.assertInstanceOf(WaitRandom.class, program.get(1));
            Assertions.assertEquals(4, ((RandomJump) program.get(2)).getTargetPosition());
            Assertions.assertEquals(32, ((RandomJump) program.get(2)).getChance());
            Assertions.assertEquals(11, ((Call) program.get(4)).getTargetPosition());
            Assertions.assertEquals(6, program.findLabel("fall").getPosition());
            Assertions.assertSame(pack.getSoundTypes().get(0), ((PlaySound) program.get(7)).getSound());
            Assertions.assertSame(pack.getImageTypes().get(1), ((SpawnOverlay) program.get(8)).getImageType());
            Assertions.assertEquals(-3, ((SpawnOverlay) program.get(8)).getOffsetY());
            Assertions.assertEquals(1, ((FireWeapon) program.get(9)).getWeaponSlot());
            Assertions.assertSame(End.INSTANCE, program.get(10));
            Assertions.assertEquals(1, ((ShowFrame) program.get(11)).getFrame());
            Assertions.assertSame(Return.INSTANCE, program.get(12));
        }
    }

    @Test
    void open_should_read_a_zip_pack_like_its_folder(@TempDir @NotNull Path folder) throws IOException {
        Path packFolder = folder.resolve("pack");
        Path archive = folder.resolve("pack.zip");

        writePack(packFolder, KNIGHT_ANIMATIONS);

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive)); Stream<Path> files = Files.walk(packFolder)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                zip.putNextEntry(new ZipEntry(packFolder.relativize(file).toString().replace('\\', '/')));
                zip.write(Files.readAllBytes(file));
                zip.closeEntry();
            }
        }

        try (StandardPack folderPack = StandardPack.open(packFolder); StandardPack zipPack = StandardPack.open(archive)) {
            SpriteSheet expected = folderPack.loadSpriteSheet(ResourceKey.of("knight"));
            SpriteSheet actual = zipPack.loadSpriteSheet(ResourceKey.of("knight"));
            int[] expectedPixels = new int[expected.getColor().getLength()];
            int[] actualPixels = new int[actual.getColor().getLength()];

            expected.getColor().copyValues(0, expectedPixels, 0, expectedPixels.length);
            actual.getColor().copyValues(0, actualPixels, 0, actualPixels.length);

            Assertions.assertArrayEquals(expectedPixels, actualPixels);
            Assertions.assertEquals(folderPack.loadAnimations().getPrograms().getFirst().getLength(),
                                    zipPack.loadAnimations().getPrograms().getFirst().getLength());
        }
    }

    @Test
    void load_should_read_palettes_string_tables_and_sounds(@TempDir @NotNull Path folder) throws IOException {
        writePack(folder, KNIGHT_ANIMATIONS);

        try (StandardPack pack = StandardPack.open(folder)) {
            Palette palette = pack.loadPalette(ResourceKey.of("gray"));
            StringTable strings = pack.loadStringTable(ResourceKey.of("en"));
            SoundClip sound = pack.loadSound(ResourceKey.of("click"));

            Assertions.assertEquals(2, palette.getSize());
            Assertions.assertEquals(0xFF102030, palette.getColor(0));
            Assertions.assertEquals(0x80FFFFFF, palette.getColor(1));
            Assertions.assertEquals(List.of("Start", "Quit"), strings.getStrings());
            Assertions.assertEquals(SAMPLE_RATE, sound.getSampleRate());
            Assertions.assertEquals(SAMPLES.length, sound.getFrameCount());
            Assertions.assertEquals(SAMPLES[2], sound.getSample(2));
            Assertions.assertEquals(List.of("click"), pack.getSoundIds());
        }
    }

    @Test
    void load_tile_graphics_should_read_tiles_shape_and_tile_codes(@TempDir @NotNull Path folder) throws IOException {
        writeTilesetPack(folder, "diamond", "[0, 2]");

        try (StandardPack pack = StandardPack.open(folder)) {
            TileGraphics tiles = pack.loadTileGraphics(ResourceKey.of("meadow"));
            TilesetLegend legend = pack.loadTilesetLegend(ResourceKey.of("meadow"));

            Assertions.assertEquals(TILE_SIZE, tiles.getTileWidth());
            Assertions.assertEquals(TILE_SIZE, tiles.getTileHeight());
            Assertions.assertEquals(TileShape.DIAMOND, tiles.getShape());
            Assertions.assertEquals(TILE_COLUMNS * TILE_ROWS, tiles.getTileCount());

            for (int tileIndex = 0; tileIndex < tiles.getTileCount(); tileIndex++) {
                Assertions.assertEquals(OPAQUE | tileIndex, tiles.getPixels().getValue(tiles.getTileOffset(tileIndex) + TILE_SIZE + 1));
            }

            Assertions.assertEquals("#.", legend.getCodes());
            Assertions.assertEquals(2, legend.getTile('.', 1));
            Assertions.assertEquals(3, legend.getTile('#', 0));
        }
    }

    @Test
    void load_tile_graphics_should_reject_unknown_shapes_and_missing_tiles(@TempDir @NotNull Path folder) throws IOException {
        writeTilesetPack(folder.resolve("shape"), "hexagon", "[0]");
        writeTilesetPack(folder.resolve("tile"), "square", "[4]");

        try (StandardPack pack = StandardPack.open(folder.resolve("shape"))) {
            Assertions.assertThrows(IOException.class, () -> pack.loadTileGraphics(ResourceKey.of("meadow")));
        }

        try (StandardPack pack = StandardPack.open(folder.resolve("tile"))) {
            Assertions.assertEquals(TileShape.SQUARE, pack.loadTileGraphics(ResourceKey.of("meadow")).getShape());
            Assertions.assertThrows(IOException.class, () -> pack.loadTilesetLegend(ResourceKey.of("meadow")));
        }
    }

    @Test
    void load_font_should_read_trimmed_glyphs_from_the_glyph_sheet(@TempDir @NotNull Path folder) throws IOException {
        writeTilesetPack(folder, "square", "[0]");

        try (StandardPack pack = StandardPack.open(folder)) {
            FontFace font = pack.loadFont(ResourceKey.of("body"));
            FontGlyph letter = font.findGlyph('A');
            FontGlyph space = font.findGlyph(' ');

            Assertions.assertNotNull(letter);
            Assertions.assertNotNull(space);
            Assertions.assertNull(font.findGlyph('B'));
            Assertions.assertEquals(GLYPH_CELL + 1, font.getLineHeight());
            Assertions.assertEquals(GLYPH_CELL * 2, font.getImageWidth());
            Assertions.assertEquals(2, letter.getWidth());
            Assertions.assertEquals(3, letter.getHeight());
            Assertions.assertEquals(1, letter.getOffsetY());
            Assertions.assertEquals(GLYPH_CELL + 1, letter.getImageX());
            Assertions.assertEquals(1, letter.getImageY());
            Assertions.assertEquals(3, letter.getAdvance());
            Assertions.assertEquals(0, space.getWidth());
            Assertions.assertEquals(SPACE_ADVANCE, space.getAdvance());
            Assertions.assertEquals(GLYPH_ALPHA, font.getPixels().getValue(letter.getImageY() * font.getImageWidth() + letter.getImageX()));
        }

        Files.writeString(folder.resolve("fonts/body.json"), "{\"image\": \"body.png\", \"cellWidth\": 4, \"cellHeight\": 4, \"characters\": \"AA\"}",
                          StandardCharsets.UTF_8);

        try (StandardPack pack = StandardPack.open(folder)) {
            Assertions.assertThrows(IOException.class, () -> pack.loadFont(ResourceKey.of("body")));
        }
    }

    @Test
    void load_dialog_should_read_controls_with_kinds_alignments_attributes_and_children(@TempDir @NotNull Path folder) throws IOException {
        Files.writeString(folder.resolve(PackManifest.FILE_NAME), "{\"format\": 1, \"dialogs\": {\"menu\": \"dialogs/menu.json\"}}",
                          StandardCharsets.UTF_8);
        Files.createDirectories(folder.resolve("dialogs"));
        Files.writeString(folder.resolve("dialogs/menu.json"),
                          "{\"x\": 4, \"width\": 320, \"height\": 200, \"controls\": [" +
                          "{\"id\": 1, \"kind\": \"panel\", \"x\": 0, \"y\": 0, \"width\": 320, \"height\": 200, \"children\": [" +
                          "{\"id\": 2, \"kind\": \"button\", \"x\": 20, \"y\": 30, \"width\": 280, \"height\": 32, \"text\": \"Start\"," +
                          " \"align\": \"center\", \"attributes\": {\"hotkey\": \"s\", \"default\": \"true\"}}," +
                          "{\"id\": 3, \"kind\": \"list-box\", \"x\": 20, \"y\": 70, \"width\": 280, \"height\": 100}]}]}",
                          StandardCharsets.UTF_8);

        try (StandardPack pack = StandardPack.open(folder)) {
            DialogDefinition dialog = pack.loadDialog(ResourceKey.of("menu"));
            DialogControl start = dialog.findControl(2);
            DialogControl list = dialog.findControl(3);

            Assertions.assertEquals("menu", dialog.getName());
            Assertions.assertEquals(4, dialog.getX());
            Assertions.assertEquals(0, dialog.getY());
            Assertions.assertEquals(1, dialog.getControls().size());
            Assertions.assertEquals(ControlKind.PANEL, dialog.getControls().getFirst().getKind());
            Assertions.assertNotNull(start);
            Assertions.assertNotNull(list);
            Assertions.assertEquals(ControlKind.BUTTON, start.getKind());
            Assertions.assertEquals("Start", start.getText());
            Assertions.assertEquals(TextAlignment.CENTER, start.getTextAlignment());
            Assertions.assertEquals(Map.of("hotkey", "s", "default", "true"), start.getAttributes());
            Assertions.assertEquals(ControlKind.LIST_BOX, list.getKind());
            Assertions.assertEquals(TextAlignment.LEFT, list.getTextAlignment());
            Assertions.assertNull(list.getText());
        }

        Files.writeString(folder.resolve("dialogs/menu.json"),
                          "{\"width\": 10, \"height\": 10, \"controls\": [{\"id\": 1, \"kind\": \"slider\", \"x\": 0, \"y\": 0," +
                          " \"width\": 1, \"height\": 1}]}", StandardCharsets.UTF_8);

        try (StandardPack pack = StandardPack.open(folder)) {
            Assertions.assertThrows(IOException.class, () -> pack.loadDialog(ResourceKey.of("menu")));
        }

        Files.writeString(folder.resolve("dialogs/menu.json"),
                          "{\"width\": 10, \"height\": 10, \"colour\": \"red\"}", StandardCharsets.UTF_8);

        try (StandardPack pack = StandardPack.open(folder)) {
            Assertions.assertThrows(IOException.class, () -> pack.loadDialog(ResourceKey.of("menu")));
        }
    }

    @Test
    void load_should_report_unlisted_and_unsupported_resources_as_not_found(@TempDir @NotNull Path folder) throws IOException {
        writePack(folder, KNIGHT_ANIMATIONS);

        try (StandardPack pack = StandardPack.open(folder)) {
            ResourceKey missing = ResourceKey.of("missing");

            Assertions.assertThrows(ResourceNotFoundException.class, () -> pack.loadSpriteSheet(missing));
            Assertions.assertThrows(ResourceNotFoundException.class, () -> pack.loadSound(missing));
            Assertions.assertThrows(ResourceNotFoundException.class, () -> pack.loadPalette(missing));
            Assertions.assertThrows(ResourceNotFoundException.class, () -> pack.loadStringTable(missing));
            Assertions.assertThrows(ResourceNotFoundException.class, () -> pack.loadFont(missing));
            Assertions.assertThrows(ResourceNotFoundException.class, () -> pack.loadDialog(missing));
            Assertions.assertThrows(ResourceNotFoundException.class, () -> pack.loadTileGraphics(missing));
            Assertions.assertThrows(ResourceNotFoundException.class, () -> pack.loadPicture(missing));
        }
    }

    @Test
    void load_picture_should_read_plain_pictures_and_definitions_with_mask_anchor_and_borders(@TempDir @NotNull Path folder) throws IOException {
        Files.createDirectories(folder.resolve("hud"));
        Files.writeString(folder.resolve(PackManifest.FILE_NAME), """
                                                                  {"format": 1, "pictures": {"screens/menu": "menu.png", "hud/panel": "hud/panel.json", "hud/broken": "hud/broken.json",
                                                                                             "hud/text": "hud/panel.txt"}}
                                                                  """, StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("hud/panel.json"), """
                                                            {"image": "panel.png", "teamMask": "panel_team.png", "anchor": {"x": 1, "y": 0}, "borders": {"left": 1, "right": 2}}
                                                            """, StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("hud/broken.json"), """
                                                             {"image": "panel.png", "borders": {"left": 3, "right": 2}}
                                                             """, StandardCharsets.UTF_8);
        writePicture(folder.resolve("menu.png"), FRAME_SIZE * COLUMNS, 0);
        writePicture(folder.resolve("hud/panel.png"), FRAME_SIZE * COLUMNS, 0);
        writeMask(folder.resolve("hud/panel_team.png"));

        try (StandardPack pack = StandardPack.open(folder)) {
            Picture menu = pack.loadPicture(ResourceKey.of("screens/menu"));
            Picture panel = pack.loadPicture(ResourceKey.of("hud/panel"));
            PixelLayer teamMask = panel.getTeamMask();

            Assertions.assertEquals(FRAME_SIZE * COLUMNS, menu.getWidth());
            Assertions.assertEquals(FRAME_SIZE, menu.getHeight());
            Assertions.assertNull(menu.getTeamMask());
            Assertions.assertEquals(0, menu.getBorderLeft() + menu.getBorderRight() + menu.getAnchorX());
            Assertions.assertEquals(OPAQUE | 1, menu.getColor().getValue(FRAME_SIZE));
            Assertions.assertNotNull(teamMask);
            Assertions.assertEquals(0, teamMask.getValue(0));
            Assertions.assertEquals(MASK_VALUE, teamMask.getValue(FRAME_SIZE));
            Assertions.assertEquals(1, panel.getAnchorX());
            Assertions.assertEquals(1, panel.getBorderLeft());
            Assertions.assertEquals(0, panel.getBorderTop());
            Assertions.assertEquals(2, panel.getBorderRight());
            Assertions.assertThrows(IOException.class, () -> pack.loadPicture(ResourceKey.of("hud/broken")), "the borders are wider than the picture");
            Assertions.assertThrows(IOException.class, () -> pack.loadPicture(ResourceKey.of("hud/text")), "neither a PNG nor a definition");
        }
    }

    @Test
    void open_should_reject_a_missing_or_unsupported_manifest(@TempDir @NotNull Path folder) throws IOException {
        Assertions.assertThrows(IOException.class, () -> StandardPack.open(folder));

        Files.writeString(folder.resolve(PackManifest.FILE_NAME), "{\"format\": 2}", StandardCharsets.UTF_8);
        Assertions.assertThrows(IOException.class, () -> StandardPack.open(folder));

        Files.writeString(folder.resolve(PackManifest.FILE_NAME), "{\"format\": 1, \"spriteSheets\": {\"a\": \"../a.json\"}}", StandardCharsets.UTF_8);
        Assertions.assertThrows(IOException.class, () -> StandardPack.open(folder));

        Files.writeString(folder.resolve(PackManifest.FILE_NAME), "{\"format\": 1, \"animationKinds\": [\"idle\", \"idle\"]}", StandardCharsets.UTF_8);
        Assertions.assertThrows(IOException.class, () -> StandardPack.open(folder));
    }

    @Test
    void load_animations_should_reject_invalid_instructions(@TempDir @NotNull Path folder) throws IOException {
        Map<String, String> cases = Map.of("unknown keyword", "\"idle\": [{\"fly\": 1}]",
                                           "two keywords", "\"idle\": [{\"frame\": 0, \"wait\": 1}]",
                                           "unknown label", "\"idle\": [{\"jump\": \"nowhere\"}]",
                                           "frame outside the sheet", "\"idle\": [{\"frame\": 2}, {\"end\": true}]",
                                           "empty animation", "\"idle\": []",
                                           "zero wait", "\"idle\": [{\"wait\": 0}, {\"end\": true}]",
                                           "label defined twice", "\"idle\": [{\"label\": \"idle\"}, {\"end\": true}]",
                                           "unknown sound", "\"idle\": [{\"sound\": \"bang\"}, {\"end\": true}]",
                                           "label after the end", "\"idle\": [{\"end\": true}, {\"label\": \"late\"}]",
                                           "chance out of range", "\"idle\": [{\"randomJump\": \"idle\", \"chance\": 300}]");

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            Path packFolder = folder.resolve(entry.getKey().replace(' ', '_'));

            writePack(packFolder, entry.getValue());

            try (StandardPack pack = StandardPack.open(packFolder)) {
                Assertions.assertThrows(IOException.class, pack::loadAnimations, entry.getKey());
            }
        }
    }

    @Test
    void load_sprite_sheet_should_reject_a_picture_of_another_size(@TempDir @NotNull Path folder) throws IOException {
        writePack(folder, KNIGHT_ANIMATIONS);
        writePicture(folder.resolve("fx/smoke.png"), 1, 0);

        try (StandardPack pack = StandardPack.open(folder)) {
            Assertions.assertThrows(IOException.class, () -> pack.loadSpriteSheet(ResourceKey.of("smoke")));
        }
    }

    @Test
    void open_should_add_mod_instruction_types_and_reject_used_keywords(@TempDir @NotNull Path folder) throws IOException {
        writePack(folder, "\"idle\": [{\"frame\": 0}, {\"vanish\": true}]");

        try (StandardPack pack = StandardPack.open(folder, List.of(new VanishType("vanish")))) {
            AnimationProgram program = pack.loadAnimations().getPrograms().getFirst();

            Assertions.assertInstanceOf(Vanish.class, program.get(1));
        }

        try (StandardPack pack = StandardPack.open(folder)) {
            Assertions.assertThrows(IOException.class, pack::loadAnimations);
        }

        Assertions.assertThrows(IllegalArgumentException.class, () -> StandardPack.open(folder, List.of(new VanishType("wait"))));
        Assertions.assertThrows(IllegalArgumentException.class, () -> StandardPack.open(folder, List.of(new VanishType("label"))));
    }

    private static void writePack(@NotNull Path folder, @NotNull String knightAnimations) throws IOException {
        Files.createDirectories(folder.resolve("units"));
        Files.createDirectories(folder.resolve("fx"));
        Files.createDirectories(folder.resolve("sound"));
        Files.createDirectories(folder.resolve("text"));
        Files.writeString(folder.resolve(PackManifest.FILE_NAME), MANIFEST, StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("units/knight.json"), """
                                                               {"image": "knight.png", "teamMask": "knight_team.png", "shadow": "knight_shadow.png",
                                                                "frameWidth": 2, "frameHeight": 2, "columns": 2, "rows": 1, "anchor": {"x": 1, "y": 1}, "kind": "unit",
                                                                "animations": {%s}}
                                                               """.formatted(knightAnimations), StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("fx/smoke.json"), SMOKE_DEFINITION, StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("palette.json"), "[\"#102030\", \"#80FFFFFF\"]", StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("text/en.json"), "[\"Start\", \"Quit\"]", StandardCharsets.UTF_8);
        writePicture(folder.resolve("units/knight.png"), FRAME_SIZE * COLUMNS, 0);
        writePicture(folder.resolve("fx/smoke.png"), FRAME_SIZE * COLUMNS, 0);
        writePicture(folder.resolve("units/knight_shadow.png"), FRAME_SIZE * COLUMNS, SHADOW_ALPHA);
        writeMask(folder.resolve("units/knight_team.png"));
        writeSound(folder.resolve("sound/click.wav"));
    }

    private static void writeTilesetPack(@NotNull Path folder, @NotNull String shape, @NotNull String grassTiles) throws IOException {
        Files.createDirectories(folder.resolve("tiles"));
        Files.createDirectories(folder.resolve("fonts"));
        Files.writeString(folder.resolve(PackManifest.FILE_NAME), """
                                                                  {"format": 1, "tilesets": {"meadow": "tiles/meadow.json"}, "fonts": {"body": "fonts/body.json"}}
                                                                  """, StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("tiles/meadow.json"), """
                                                               {"image": "meadow.png", "tileWidth": %d, "tileHeight": %d, "shape": "%s", "tiles": {".": %s, "#": [3]}}
                                                               """.formatted(TILE_SIZE, TILE_SIZE, shape, grassTiles), StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("fonts/body.json"), """
                                                             {"image": "body.png", "cellWidth": %d, "cellHeight": %d, "characters": " A", "lineHeight": %d, "spaceAdvance": %d}
                                                             """.formatted(GLYPH_CELL, GLYPH_CELL, GLYPH_CELL + 1, SPACE_ADVANCE), StandardCharsets.UTF_8);

        BufferedImage tiles = new BufferedImage(TILE_SIZE * TILE_COLUMNS, TILE_SIZE * TILE_ROWS, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < tiles.getHeight(); y++) {
            for (int x = 0; x < tiles.getWidth(); x++) {
                tiles.setRGB(x, y, OPAQUE | y / TILE_SIZE * TILE_COLUMNS + x / TILE_SIZE);
            }
        }

        ImageIO.write(tiles, "png", folder.resolve("tiles/meadow.png").toFile());

        BufferedImage glyphs = new BufferedImage(GLYPH_CELL * 2, GLYPH_CELL, BufferedImage.TYPE_INT_ARGB);

        for (int y = 1; y <= 3; y++) {
            glyphs.setRGB(GLYPH_CELL + 1, y, GLYPH_ALPHA << Integer.SIZE - Byte.SIZE);
            glyphs.setRGB(GLYPH_CELL + 2, y, GLYPH_ALPHA << Integer.SIZE - Byte.SIZE);
        }

        ImageIO.write(glyphs, "png", folder.resolve("fonts/body.png").toFile());
    }

    private static void writePicture(@NotNull Path file, int width, int shadowAlpha) throws IOException {
        BufferedImage picture = new BufferedImage(width, FRAME_SIZE, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < FRAME_SIZE; y++) {
                picture.setRGB(x, y, shadowAlpha != 0 ? shadowAlpha << Integer.SIZE - Byte.SIZE : OPAQUE | x / FRAME_SIZE);
            }
        }

        ImageIO.write(picture, "png", file.toFile());
    }

    private static void writeMask(@NotNull Path file) throws IOException {
        BufferedImage mask = new BufferedImage(FRAME_SIZE * COLUMNS, FRAME_SIZE, BufferedImage.TYPE_BYTE_GRAY);

        for (int x = FRAME_SIZE; x < mask.getWidth(); x++) {
            for (int y = 0; y < FRAME_SIZE; y++) {
                mask.getRaster().setSample(x, y, 0, MASK_VALUE);
            }
        }

        ImageIO.write(mask, "png", file.toFile());
    }

    private static void writeSound(@NotNull Path file) throws IOException {
        byte[] bytes = new byte[SAMPLES.length * 2];

        for (int index = 0; index < SAMPLES.length; index++) {
            bytes[index * 2]     = (byte) SAMPLES[index];
            bytes[index * 2 + 1] = (byte) (SAMPLES[index] >> Byte.SIZE);
        }

        AudioFormat format = new AudioFormat(SAMPLE_RATE, Short.SIZE, 1, true, false);

        try (AudioInputStream input = new AudioInputStream(new ByteArrayInputStream(bytes), format, SAMPLES.length);
             OutputStream output = Files.newOutputStream(file)) {
            AudioSystem.write(input, AudioFileFormat.Type.WAVE, output);
        }
    }
}

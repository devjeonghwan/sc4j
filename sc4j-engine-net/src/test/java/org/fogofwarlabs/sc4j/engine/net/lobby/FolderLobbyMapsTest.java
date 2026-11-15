package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Verifies the engine's default lobby maps over map folders: a map is found by the digest of its contents under any file name in the read
 * folders, read for a member that misses it, and a received map is saved in the save folder under its name and the file extension.
 */
final class FolderLobbyMapsTest {
    private static final int MAP_SIZE = 1000;

    private static final long MAP_SEED = 5;
    private static final long OTHER_MAP_SEED = 6;
    /**
     * How much later a changed file is marked as modified.
     */
    private static final long CHANGE_MILLIS = 2000L;

    private static final @NotNull String SAVE_FOLDER = "downloads";
    private static final @NotNull String EXTENSION = ".map";

    private static byte @NotNull [] createMap(long seed) {
        byte[] contents = new byte[MAP_SIZE];

        new Random(seed).nextBytes(contents);

        return contents;
    }


    @Test
    void has_map_should_find_the_contents_under_any_file_name(@TempDir @NotNull Path folder) throws IOException {
        byte[] contents = createMap(MAP_SEED);
        FolderLobbyMaps maps = new FolderLobbyMaps(MapFolders.of(List.of(folder), SAVE_FOLDER), EXTENSION);
        MapReference duel = MapReference.create("Duel", contents);

        Assertions.assertFalse(maps.hasMap(duel));

        Files.write(folder.resolve("other.map"), createMap(OTHER_MAP_SEED));
        Files.write(Files.createDirectories(folder.resolve("old")).resolve("renamed copy.bin"), contents);

        Assertions.assertTrue(maps.hasMap(duel));
        Assertions.assertArrayEquals(contents, maps.readMap(duel));
        Assertions.assertNull(maps.readMap(MapReference.create("Duel", new byte[MAP_SIZE])));
    }

    @Test
    void has_map_should_see_a_file_that_changed_since_the_last_check(@TempDir @NotNull Path folder) throws IOException {
        byte[] contents = createMap(MAP_SEED);
        byte[] other = createMap(OTHER_MAP_SEED);
        FolderLobbyMaps maps = new FolderLobbyMaps(MapFolders.of(List.of(folder), SAVE_FOLDER), EXTENSION);
        Path file = Files.write(folder.resolve("duel.map"), contents);

        Assertions.assertTrue(maps.hasMap(MapReference.create("Duel", contents)));

        FileTime checked = Files.getLastModifiedTime(file);

        Files.write(file, other);
        Files.setLastModifiedTime(file, FileTime.fromMillis(checked.toMillis() + CHANGE_MILLIS));

        Assertions.assertFalse(maps.hasMap(MapReference.create("Duel", contents)), "the old contents are gone");
        Assertions.assertTrue(maps.hasMap(MapReference.create("Duel", other)));
    }

    @Test
    void add_map_should_save_the_map_under_its_name_in_the_save_folder(@TempDir @NotNull Path folder) throws IOException {
        byte[] contents = createMap(MAP_SEED);
        FolderLobbyMaps maps = new FolderLobbyMaps(MapFolders.of(List.of(folder), SAVE_FOLDER), EXTENSION);
        MapReference duel = MapReference.create("Duel: Two/Three", contents);

        maps.addMap(duel, contents);

        Path saved = folder.resolve(SAVE_FOLDER).resolve("Duel_ Two_Three.map");

        Assertions.assertArrayEquals(contents, Files.readAllBytes(saved));
        Assertions.assertTrue(maps.hasMap(duel));
        Assertions.assertTrue(maps.hasMap(MapReference.create("Another name", contents)), "the digest names the contents");

        maps.addMap(duel, contents);

        try (Stream<Path> files = Files.list(folder.resolve(SAVE_FOLDER))) {
            Assertions.assertEquals(1L, files.count(), "the same map is not saved twice");
        }
    }

    @Test
    void add_map_should_fail_without_a_save_folder() {
        byte[] contents = createMap(MAP_SEED);
        FolderLobbyMaps maps = new FolderLobbyMaps(MapFolders.of(List.of(), SAVE_FOLDER), EXTENSION);

        Assertions.assertThrows(IOException.class, () -> maps.addMap(MapReference.create("Duel", contents), contents));
    }
}

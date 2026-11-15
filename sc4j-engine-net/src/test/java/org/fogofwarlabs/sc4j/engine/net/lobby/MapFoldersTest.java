package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Verifies the map folders of a mod: the read folders searched in order with a folder's files before its subfolders and any file name, and the
 * save folder that keeps a received map under a free name, reuses a file of the same contents and never leaves the folder.
 */
final class MapFoldersTest {
    private static final int MAP_SIZE = 1000;

    private static final long MAP_SEED = 5;
    private static final long OTHER_MAP_SEED = 6;

    private static final @NotNull String SAVE_FOLDER = "downloads";

    private static byte @NotNull [] createMap(long seed) {
        byte[] contents = new byte[MAP_SIZE];

        new Random(seed).nextBytes(contents);

        return contents;
    }

    private static @NotNull Path writeMap(@NotNull Path file, byte @NotNull [] contents) throws IOException {
        Files.createDirectories(file.getParent());

        return Files.write(file, contents);
    }

    private static @NotNull MapFileMatch matchContents(byte @NotNull [] contents) {
        return file -> Arrays.equals(contents, Files.readAllBytes(file));
    }

    private static @NotNull List<@NotNull String> listNames(@NotNull Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }


    @Test
    void find_file_should_search_the_read_folders_in_order(@TempDir @NotNull Path folder) throws IOException {
        byte[] contents = createMap(MAP_SEED);
        Path firstMaps = folder.resolve("first");
        Path secondMaps = folder.resolve("second");
        MapFolders folders = MapFolders.of(List.of(firstMaps, secondMaps), SAVE_FOLDER);
        Path secondCopy = writeMap(secondMaps.resolve("duel.map"), contents);

        Assertions.assertEquals(secondCopy, folders.findFile(matchContents(contents)), "a missing first folder is skipped");

        Path firstCopy = writeMap(firstMaps.resolve(SAVE_FOLDER).resolve("duel.map"), contents);

        Assertions.assertEquals(firstCopy, folders.findFile(matchContents(contents)), "the first folder holding a copy wins");
    }

    @Test
    void find_file_should_take_any_file_name_whose_contents_match(@TempDir @NotNull Path folder) throws IOException {
        byte[] contents = createMap(MAP_SEED);
        MapFolders folders = MapFolders.of(List.of(folder), SAVE_FOLDER);

        writeMap(folder.resolve("a same size.map"), createMap(OTHER_MAP_SEED));

        Path renamed = writeMap(folder.resolve("renamed.other"), contents);

        Assertions.assertEquals(renamed, folders.findFile(matchContents(contents)));
        Assertions.assertNull(folders.findFile(matchContents(new byte[MAP_SIZE])));
    }

    @Test
    void find_file_should_take_the_files_of_a_folder_before_its_subfolders(@TempDir @NotNull Path folder) throws IOException {
        byte[] contents = createMap(MAP_SEED);
        MapFolders folders = MapFolders.of(List.of(folder), SAVE_FOLDER);

        writeMap(folder.resolve("a").resolve("b").resolve("deepest.map"), contents);

        Path middle = writeMap(folder.resolve("a").resolve("z middle.map"), contents);
        Path top = writeMap(folder.resolve("z top.map"), contents);

        Assertions.assertEquals(top, folders.findFile(matchContents(contents)));

        Files.delete(top);

        Assertions.assertEquals(middle, folders.findFile(matchContents(contents)));
    }

    @Test
    void of_should_save_into_the_subfolder_of_the_first_read_folder() {
        MapFolders folders = MapFolders.of(List.of(Path.of("first"), Path.of("second")), SAVE_FOLDER);

        Assertions.assertEquals(List.of(Path.of("first"), Path.of("second")), folders.getReadFolders());
        Assertions.assertEquals(Path.of("first", SAVE_FOLDER), folders.getSaveFolder());
        Assertions.assertNull(MapFolders.of(List.of(), SAVE_FOLDER).getSaveFolder());
    }

    @Test
    void save_file_should_take_a_free_name_instead_of_replacing_another_file(@TempDir @NotNull Path folder) throws IOException {
        byte[] contents = createMap(MAP_SEED);
        byte[] other = createMap(OTHER_MAP_SEED);
        Path save = Files.createDirectories(folder.resolve(SAVE_FOLDER));
        Path existing = Files.write(save.resolve("held.map"), other);
        Path twin = Files.write(save.resolve("twin.map"), contents);
        MapFolders folders = new MapFolders(List.of(), save);

        Path saved = folders.saveFile("held.map", contents);

        Assertions.assertArrayEquals(other, Files.readAllBytes(existing), "the existing file keeps its bytes");
        Assertions.assertEquals(save.resolve("held (2).map"), saved);
        Assertions.assertArrayEquals(contents, Files.readAllBytes(saved));

        long modified = Files.getLastModifiedTime(twin).toMillis();

        Assertions.assertEquals(twin, folders.saveFile("twin.map", contents));
        Assertions.assertEquals(modified, Files.getLastModifiedTime(twin).toMillis(), "the same file is not written again");
        Assertions.assertEquals(save.resolve("held (2).map"), folders.saveFile("held.map", contents), "the free name with the contents is reused");

        Files.write(save.resolve("plain"), other);

        Assertions.assertEquals(save.resolve("plain (2)"), folders.saveFile("plain", contents), "a name without extension");
        Assertions.assertEquals(List.of("held (2).map", "held.map", "plain", "plain (2)", "twin.map"), listNames(save), "no partial file stays");
    }

    @Test
    void save_file_should_create_the_save_folder_and_refuse_a_name_outside_it(@TempDir @NotNull Path folder) throws IOException {
        byte[] contents = createMap(MAP_SEED);
        Path save = folder.resolve("maps").resolve(SAVE_FOLDER);
        MapFolders folders = new MapFolders(List.of(folder.resolve("maps")), save);

        for (String name : new String[]{"..", "../x.map", "a/b.map", "/abs.map", ""}) {
            Assertions.assertThrows(IOException.class, () -> folders.saveFile(name, contents), name);
        }

        Assertions.assertFalse(Files.exists(folder.resolve("x.map")));
        Assertions.assertEquals(save.toAbsolutePath().normalize().resolve("duel.map"), folders.saveFile("duel.map", contents));
        Assertions.assertThrows(IOException.class, () -> new MapFolders(List.of(folder), null).saveFile("duel.map", contents), "no save folder");
    }
}

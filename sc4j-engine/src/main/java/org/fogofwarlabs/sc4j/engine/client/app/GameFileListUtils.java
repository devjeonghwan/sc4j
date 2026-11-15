package org.fogofwarlabs.sc4j.engine.client.app;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.engine.replay.ReplayFormat;
import org.fogofwarlabs.sc4j.engine.replay.ReplaySummary;
import org.fogofwarlabs.sc4j.engine.save.SaveFormat;
import org.fogofwarlabs.sc4j.engine.save.SaveSummary;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lists the stored games of a folder through a mod's registered formats: the replays its replay formats read, or the saved games its save
 * formats read. A file that no format recognizes or whose header cannot be read is left out (and logged); a missing folder lists nothing.
 * The entries are sorted newest first (by the time the file was last written), files of the same time by name.
 */
public final class GameFileListUtils {
    private static final @NotNull Logger LOGGER = Logger.of(GameFileListUtils.class);
    /**
     * Orders entries newest first, entries of the same time by file name.
     */
    private static final @NotNull Comparator<GameFileEntry> NEWEST_FIRST = Comparator.comparing(GameFileEntry::getModifiedTime).reversed()
                                                                                     .thenComparing(entry -> entry.getPath().getFileName().toString());

    /**
     * Lists the replays of a folder.
     *
     * @param folder  the folder, searched without its subfolders
     * @param formats the mod's replay formats, in registration order
     * @return the entries, newest first
     * @throws IOException if the folder cannot be listed
     */
    public static @NotNull List<@NotNull GameFileEntry> listReplays(@NotNull Path folder, @NotNull List<@NotNull ReplayFormat> formats) throws IOException {
        List<GameFileEntry> entries = new ArrayList<>();

        for (Path file : listFiles(folder)) {
            byte[] contents = Files.readAllBytes(file);
            ReplayFormat format = ReplayFormat.find(formats, contents);

            if (format == null) {
                continue;
            }

            try {
                ReplaySummary summary = format.readSummary(contents);

                entries.add(new GameFileEntry(file, format.getName(), Files.getLastModifiedTime(file).toInstant(), summary.getMapName(),
                                              summary.getFrameCount(), summary.getPlayerCount(), summary.getPlayers()));
            } catch (IOException exception) {
                LOGGER.warning("REPLAY SKIPPED: {0} ({1})", file, exception.getMessage());
            }
        }

        entries.sort(NEWEST_FIRST);

        return entries;
    }

    /**
     * Lists the saved games of a folder.
     *
     * @param folder  the folder, searched without its subfolders
     * @param formats the mod's save formats, in registration order
     * @return the entries, newest first
     * @throws IOException if the folder cannot be listed
     */
    public static @NotNull List<@NotNull GameFileEntry> listSaves(@NotNull Path folder, @NotNull List<@NotNull SaveFormat> formats) throws IOException {
        List<GameFileEntry> entries = new ArrayList<>();

        for (Path file : listFiles(folder)) {
            byte[] contents = Files.readAllBytes(file);
            SaveFormat format = SaveFormat.find(formats, contents);

            if (format == null) {
                continue;
            }

            try {
                SaveSummary summary = format.readSummary(contents);

                entries.add(new GameFileEntry(file, format.getName(), Files.getLastModifiedTime(file).toInstant(), summary.getMapName(),
                                              summary.getFrame(), summary.getPlayerCount(), List.of()));
            } catch (IOException exception) {
                LOGGER.warning("SAVE SKIPPED: {0} ({1})", file, exception.getMessage());
            }
        }

        entries.sort(NEWEST_FIRST);

        return entries;
    }

    private static @NotNull List<@NotNull Path> listFiles(@NotNull Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(Files::isRegularFile).sorted(Comparator.comparing(file -> file.getFileName().toString())).toList();
        }
    }

    private GameFileListUtils() {
    }
}

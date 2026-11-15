package org.fogofwarlabs.sc4j.base.io;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Checks that a file written through a temporary file replaces the file and leaves no other file.
 */
final class AtomicFileUtilsTest {
    private static final @NotNull String FOLDER = "replays";
    private static final @NotNull String FILE_NAME = "last-game.sc4r";
    private static final byte @NotNull [] FIRST_CONTENTS = {1, 2, 3};
    private static final byte @NotNull [] SECOND_CONTENTS = {4, 5};

    @Test
    void write_should_create_the_folder_and_replace_the_file(@TempDir @NotNull Path directory) throws IOException {
        Path folder = directory.resolve(FOLDER);
        Path file = folder.resolve(FILE_NAME);

        AtomicFileUtils.write(file, FIRST_CONTENTS);

        Assertions.assertArrayEquals(FIRST_CONTENTS, Files.readAllBytes(file));

        AtomicFileUtils.write(file, SECOND_CONTENTS);

        Assertions.assertArrayEquals(SECOND_CONTENTS, Files.readAllBytes(file));

        try (Stream<Path> files = Files.list(folder)) {
            Assertions.assertEquals(List.of(file), files.toList());
        }
    }
}

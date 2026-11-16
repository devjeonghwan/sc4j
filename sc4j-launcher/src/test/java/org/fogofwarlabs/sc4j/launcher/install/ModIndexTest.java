package org.fogofwarlabs.sc4j.launcher.install;

import org.fogofwarlabs.sc4j.launcher.release.ReleaseClient;
import org.fogofwarlabs.sc4j.launcher.release.ReleaseException;
import org.fogofwarlabs.sc4j.launcher.release.ReleaseProblem;
import org.fogofwarlabs.sc4j.launcher.release.ReleaseServerScenario;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Tests the mod catalog: its entries in file order, entries without a name or a valid repository left out, and files that are no catalog.
 */
final class ModIndexTest {
    private static final @NotNull String CATALOG = """
                                                   {
                                                     "format": 1,
                                                     "mods": [
                                                       {"name": "First Mod", "repository": "owner/first", "description": "The first."},
                                                       {"name": "No Repository"},
                                                       {"name": " ", "repository": "owner/blank"},
                                                       {"name": "Bad Repository", "repository": "not a repository"},
                                                       "not an entry",
                                                       {"name": "Second Mod", "repository": "https://github.com/owner/second"}
                                                     ]
                                                   }
                                                   """;

    @Test
    void parse_should_keep_the_valid_entries_in_file_order() throws IOException {
        List<ModIndexEntry> entries = ModIndex.parse(CATALOG.getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals(2, entries.size());
        Assertions.assertEquals("First Mod", entries.get(0).getName());
        Assertions.assertEquals("owner/first", entries.get(0).getRepository().toString());
        Assertions.assertEquals("The first.", entries.get(0).getDescription());
        Assertions.assertEquals("Second Mod", entries.get(1).getName());
        Assertions.assertEquals("owner/second", entries.get(1).getRepository().toString());
        Assertions.assertNull(entries.get(1).getDescription());
    }

    @Test
    void parse_should_refuse_a_file_that_is_no_catalog() {
        for (String text : List.of("not json", "[]", "{\"format\": 2, \"mods\": []}", "{\"format\": 1}", "{\"mods\": []}")) {
            Assertions.assertThrows(IOException.class, () -> ModIndex.parse(text.getBytes(StandardCharsets.UTF_8)), text);
        }
    }

    @Test
    void read_should_fetch_the_catalog_from_its_address() throws IOException {
        try (ReleaseServerScenario server = ReleaseServerScenario.start()) {
            ReleaseClient client = new ReleaseClient(server.getApi());
            ModIndex index = new ModIndex(client, server.addFile("mods.json", CATALOG.getBytes(StandardCharsets.UTF_8)));
            ModIndex missing = new ModIndex(client, server.getApi().resolve("missing.json"));
            ReleaseException exception = Assertions.assertThrows(ReleaseException.class, missing::read);

            Assertions.assertEquals(2, index.read().size());
            Assertions.assertEquals(ReleaseProblem.NOT_FOUND, exception.getProblem());
        }
    }
}

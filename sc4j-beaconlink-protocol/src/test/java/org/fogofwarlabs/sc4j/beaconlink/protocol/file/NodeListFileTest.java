package org.fogofwarlabs.sc4j.beaconlink.protocol.file;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Tests the install's node list file {@code userdata/beaconlink/nodes.json}: the round trip of plain and pinned entries in order, the
 * unchanged layout of a list without pinned keys, the seed list standing in until a node list is saved, and the problem reported for each
 * kind of unusable file.
 */
final class NodeListFileTest {
    private static @NotNull NodeListProblem requireProblem(@NotNull NodeListFile file) {
        NodeListProblem problem = file.read().getProblem();

        Assertions.assertNotNull(problem);

        return problem;
    }

    @Test
    void write_and_read_should_keep_plain_and_pinned_entries_in_order(@TempDir @NotNull Path directory) throws IOException {
        IdentityKey key = IdentityKeyPair.generate(new SecureRandom()).getPublicKey();
        NodeListFile file = new NodeListFile(directory.resolve("userdata").resolve("beaconlink").resolve(NodeListFile.FILE_NAME));
        List<ConfiguredNode> nodes = List.of(ConfiguredNode.parse("beacon.example.org:7000", null), ConfiguredNode.parse("[2001:db8::1]:7001", key),
                                             ConfiguredNode.of("192.0.2.5", 65535, null));

        file.write(nodes);

        NodeListReading reading = file.read();

        Assertions.assertNull(reading.getProblem());
        Assertions.assertEquals(nodes, reading.getNodes());
        Assertions.assertEquals(key, reading.getNodes().get(1).getKey());
        Assertions.assertTrue(Files.readString(file.getPath(), StandardCharsets.UTF_8)
                                   .contains("\"key\": \"" + HexFormat.of().formatHex(key.getBytes()) + "\""));
    }

    @Test
    void write_should_keep_the_layout_of_a_list_without_pinned_keys(@TempDir @NotNull Path directory) throws IOException {
        NodeListFile file = new NodeListFile(directory.resolve(NodeListFile.FILE_NAME));

        file.write(List.of(ConfiguredNode.parse("beacon.example.org:7000", null), ConfiguredNode.parse("[2001:db8::1]:7001", null)));

        Assertions.assertEquals("{\n  \"format\": 1,\n  \"nodes\": [\"beacon.example.org:7000\", \"[2001:db8::1]:7001\"]\n}\n",
                                Files.readString(file.getPath(), StandardCharsets.UTF_8));
    }

    @Test
    void read_should_return_no_node_and_no_problem_without_a_file(@TempDir @NotNull Path directory) {
        NodeListReading reading = new NodeListFile(directory.resolve(NodeListFile.FILE_NAME)).read();

        Assertions.assertEquals(List.of(), reading.getNodes());
        Assertions.assertNull(reading.getProblem());
    }

    @Test
    void select_existing_or_should_use_the_seed_list_until_a_node_list_is_saved(@TempDir @NotNull Path directory) throws IOException {
        NodeListFile userList = new NodeListFile(directory.resolve("userdata").resolve("beaconlink").resolve(NodeListFile.FILE_NAME));
        NodeListFile seedList = new NodeListFile(directory.resolve("launcher").resolve("seeds.json"));
        List<ConfiguredNode> seeds = List.of(ConfiguredNode.parse("beacon.example.org:7455", null));

        seedList.write(seeds);

        Assertions.assertSame(seedList, userList.selectExistingOr(seedList));
        Assertions.assertEquals(seeds, userList.selectExistingOr(seedList).read().getNodes());

        userList.write(List.of());

        Assertions.assertSame(userList, userList.selectExistingOr(seedList));
        Assertions.assertEquals(List.of(), userList.selectExistingOr(seedList).read().getNodes());
    }

    @Test
    void read_should_report_the_problem_of_an_unusable_file(@TempDir @NotNull Path directory) throws IOException {
        Path path = directory.resolve(NodeListFile.FILE_NAME);
        NodeListFile file = new NodeListFile(path);
        Map<String, NodeListProblemKind> cases = Map.of(
                "not json", NodeListProblemKind.NOT_JSON,
                "[".repeat(10_000), NodeListProblemKind.NOT_JSON,
                "[]", NodeListProblemKind.NOT_A_NODE_LIST,
                "{\"format\": 1}", NodeListProblemKind.NOT_A_NODE_LIST,
                "{\"format\": \"1\", \"nodes\": []}", NodeListProblemKind.NOT_A_NODE_LIST,
                "{\"format\": 2, \"nodes\": []}", NodeListProblemKind.UNSUPPORTED_FORMAT,
                "{\"format\": 1, \"nodes\": [\"no-port\"]}", NodeListProblemKind.INVALID_ENTRY,
                "{\"format\": 1, \"nodes\": [{\"address\": \"host:1\", \"key\": \"00\"}]}", NodeListProblemKind.INVALID_ENTRY,
                "{\"format\": 1, \"nodes\": [{\"address\": \"host:1\", \"key\": 5}]}", NodeListProblemKind.INVALID_ENTRY,
                "{\"format\": 1, \"nodes\": [\"host:1\", 7]}", NodeListProblemKind.INVALID_ENTRY
        );

        for (Map.Entry<String, NodeListProblemKind> entry : cases.entrySet()) {
            Files.writeString(path, entry.getKey(), StandardCharsets.UTF_8);

            NodeListReading reading = file.read();
            NodeListProblem problem = reading.getProblem();

            Assertions.assertNotNull(problem, entry.getKey());
            Assertions.assertEquals(entry.getValue(), problem.getKind(), entry.getKey());
            Assertions.assertEquals(List.of(), reading.getNodes());
        }

        Files.writeString(path, "{\"format\": 1, \"nodes\": [\"host:1\", 7]}", StandardCharsets.UTF_8);

        Assertions.assertEquals(1, requireProblem(file).getEntryIndex());

        Files.write(path, new byte[NodeListFile.MAXIMUM_FILE_LENGTH + 1]);

        Assertions.assertEquals(NodeListProblemKind.TOO_LARGE, requireProblem(file).getKind());
    }

    @Test
    void read_should_report_an_unreadable_file(@TempDir @NotNull Path directory) throws IOException {
        Path folder = Files.createDirectory(directory.resolve(NodeListFile.FILE_NAME));

        Assertions.assertEquals(NodeListProblemKind.UNREADABLE, requireProblem(new NodeListFile(folder)).getKind());
    }
}

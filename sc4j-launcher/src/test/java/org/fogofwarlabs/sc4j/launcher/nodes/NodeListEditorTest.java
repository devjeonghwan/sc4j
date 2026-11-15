package org.fogofwarlabs.sc4j.launcher.nodes;

import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.NodeListFile;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the node list editor: the field checks with their reasons, the proposed lists of add, replace, remove and move, and an edited list
 * saved and read back through the shared node list file with the pinned key of a hand-written entry kept silently.
 */
final class NodeListEditorTest {
    private static final @NotNull ConfiguredNode FIRST = ConfiguredNode.parse("first.example.org:7455", null);
    private static final @NotNull ConfiguredNode SECOND = ConfiguredNode.parse("second.example.org:7455", null);
    private static final @NotNull ConfiguredNode THIRD = ConfiguredNode.parse("[2001:db8::1]:7000", null);

    @Test
    void check_should_name_every_problem_of_the_fields() {
        NodeListEditor editor = new NodeListEditor(List.of(FIRST));

        Assertions.assertEquals(List.of(NodeEntryProblem.HOST_INVALID, NodeEntryProblem.PORT_INVALID),
                                editor.check("two words", "70000", NodeListEditor.NEW_ENTRY).getProblems());
        Assertions.assertEquals(List.of(NodeEntryProblem.HOST_INVALID), editor.check(" ", "7455", NodeListEditor.NEW_ENTRY).getProblems());
        Assertions.assertEquals(List.of(NodeEntryProblem.PORT_INVALID), editor.check("host", "+7", NodeListEditor.NEW_ENTRY).getProblems());
        Assertions.assertEquals(List.of(NodeEntryProblem.DUPLICATE),
                                editor.check("FIRST.example.org", "7455", NodeListEditor.NEW_ENTRY).getProblems());
        Assertions.assertNull(editor.check("FIRST.example.org", "7455", NodeListEditor.NEW_ENTRY).getNode());
    }

    @Test
    void check_should_make_an_entry_without_a_key_and_keep_a_pinned_key_of_the_same_address() {
        IdentityKey key = IdentityKeyPair.generate(new SecureRandom()).getPublicKey();
        ConfiguredNode pinned = ConfiguredNode.of(FIRST.getHost(), FIRST.getPort(), key);
        NodeListEditor editor = new NodeListEditor(List.of(pinned, SECOND));
        NodeEntryCheck added = editor.check(" 2001:db8::1 ", " 7000 ", NodeListEditor.NEW_ENTRY);

        Assertions.assertEquals(List.of(), added.getProblems());
        Assertions.assertEquals(ConfiguredNode.of("2001:db8::1", 7000, null), added.getNode());
        Assertions.assertEquals(pinned, editor.check("FIRST.example.org", "7455", 0).getNode());
        Assertions.assertEquals(ConfiguredNode.of("first.example.org", 7456, null), editor.check("first.example.org", "7456", 0).getNode());
        Assertions.assertEquals(ConfiguredNode.of("third.example.org", 1, null), editor.check("third.example.org", "1", 1).getNode());
    }

    @Test
    void check_should_refuse_a_new_entry_when_the_list_is_full() {
        List<ConfiguredNode> nodes = new ArrayList<>();

        for (int index = 0; index < NodeListEditor.MAXIMUM_NODES; index++) {
            nodes.add(ConfiguredNode.of("node" + index + ".example.org", ConfiguredNode.MINIMUM_PORT + index, null));
        }

        NodeListEditor editor = new NodeListEditor(nodes);

        Assertions.assertEquals(List.of(NodeEntryProblem.LIST_FULL),
                                editor.check("more.example.org", "7455", NodeListEditor.NEW_ENTRY).getProblems());
        Assertions.assertEquals(List.of(), editor.check("more.example.org", "7455", 0).getProblems());
    }

    @Test
    void edits_propose_lists_and_keep_the_saved_list_until_set() {
        NodeListEditor editor = new NodeListEditor(List.of(FIRST, SECOND, THIRD));

        Assertions.assertEquals(List.of(FIRST, SECOND, THIRD, FIRST), editor.add(FIRST));
        Assertions.assertEquals(List.of(FIRST, THIRD, THIRD), editor.replace(1, THIRD));
        Assertions.assertEquals(List.of(FIRST, THIRD), editor.remove(1));
        Assertions.assertEquals(List.of(SECOND, THIRD, FIRST), editor.move(0, 2));
        Assertions.assertEquals(List.of(FIRST, THIRD, SECOND), editor.move(2, 1));
        Assertions.assertEquals(List.of(FIRST, SECOND, THIRD), editor.getNodes());
        Assertions.assertTrue(editor.canMove(1, 0));
        Assertions.assertFalse(editor.canMove(0, -1));
        Assertions.assertFalse(editor.canMove(2, 3));
        Assertions.assertFalse(editor.canMove(1, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> editor.remove(3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> editor.move(0, 3));

        editor.setNodes(editor.remove(0));

        Assertions.assertEquals(List.of(SECOND, THIRD), editor.getNodes());
    }

    @Test
    void edited_list_keeps_the_pinned_key_of_a_hand_written_entry(@TempDir @NotNull Path directory) throws IOException {
        IdentityKey key = IdentityKeyPair.generate(new SecureRandom()).getPublicKey();
        ConfiguredNode pinned = ConfiguredNode.of("third.example.org", 7000, key);
        NodeListFile file = new NodeListFile(directory.resolve("userdata").resolve("beaconlink").resolve(NodeListFile.FILE_NAME));

        file.write(List.of(FIRST, pinned));

        NodeListEditor editor = new NodeListEditor(file.read().getNodes());
        ConfiguredNode added = editor.check("second.example.org", "7455", NodeListEditor.NEW_ENTRY).getNode();

        Assertions.assertNotNull(added);

        List<ConfiguredNode> proposal = editor.move(1, 0);

        editor.setNodes(proposal);
        proposal = editor.add(added);
        file.write(proposal);

        Assertions.assertEquals(List.of(pinned, FIRST, SECOND), file.read().getNodes());
        Assertions.assertEquals(key, file.read().getNodes().getFirst().getKey());

        editor.setNodes(file.read().getNodes());
        file.write(editor.remove(1));

        Assertions.assertEquals(List.of(pinned, SECOND), new NodeListEditor(file.read().getNodes()).getNodes());
    }
}

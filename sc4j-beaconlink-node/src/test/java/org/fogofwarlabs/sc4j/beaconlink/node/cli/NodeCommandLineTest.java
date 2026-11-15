package org.fogofwarlabs.sc4j.beaconlink.node.cli;

import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests the node command line: the state folder, an IP literal bind address and repeated seeds parse; missing, repeated, unknown and invalid
 * options are refused.
 */
final class NodeCommandLineTest {
    private static final int PORT = 7455;

    @Test
    void parse_should_read_the_state_the_bind_address_and_the_peers() {
        NodeCommandLine commandLine = NodeCommandLine.parse(new String[]{"--state", "node-state", "--bind", "127.0.0.1:" + PORT,
                                                                         "--peer", "[::1]:" + PORT, "--peer", "node.example:" + PORT});

        Assertions.assertFalse(commandLine.isHelp());
        Assertions.assertEquals(Path.of("node-state").toAbsolutePath().normalize(), commandLine.getState());
        Assertions.assertEquals(new InetSocketAddress(InetAddress.ofLiteral("127.0.0.1"), PORT), commandLine.getBind());
        Assertions.assertEquals(List.of(ConfiguredNode.of("::1", PORT, null), ConfiguredNode.of("node.example", PORT, null)), commandLine.getPeers());
        Assertions.assertTrue(NodeCommandLine.parse(new String[]{"--help"}).isHelp());
    }

    @Test
    void parse_should_refuse_missing_repeated_unknown_and_invalid_options() {
        List<String[]> invalid = List.of(new String[]{"--state", "node-state"},
                                         new String[]{"--bind", "127.0.0.1:" + PORT},
                                         new String[]{"--state", "a", "--state", "b", "--bind", "127.0.0.1:" + PORT},
                                         new String[]{"--state", "a", "--bind", "127.0.0.1:" + PORT, "--verbose"},
                                         new String[]{"--state", "a", "--bind", "node.example:" + PORT},
                                         new String[]{"--state", "a", "--bind", "127.0.0.1"},
                                         new String[]{"--state", "a", "--bind", "127.0.0.1:65536"},
                                         new String[]{"--state", "a", "--bind", "::1:" + PORT},
                                         new String[]{"--state", "a", "--bind", "127.0.0.1:" + PORT, "--peer", "127.0.0.1:0"},
                                         new String[]{"--state", "a", "--bind", "127.0.0.1:" + PORT, "--peer", "::1:" + PORT},
                                         new String[]{"--state", "a", "--bind", "127.0.0.1:" + PORT, "--peer"});

        for (String[] arguments : invalid) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> NodeCommandLine.parse(arguments), String.join(" ", arguments));
        }
    }
}

package org.fogofwarlabs.sc4j.beaconlink.node;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.node.cli.NodeCommandLine;
import org.fogofwarlabs.sc4j.beaconlink.node.cli.NodeExitCode;
import org.fogofwarlabs.sc4j.beaconlink.node.state.NodeStateFolder;
import org.fogofwarlabs.sc4j.beaconlink.peer.MeshPeer;
import org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyFileUtils;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.Envelope;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.MessageEncoder;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.PingMessage;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.PongMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests the node entry point: {@code --help} prints the usage and ends normally, an invalid command line ends with the configuration error
 * code and the reason, a node process started with a state folder and a 127.0.0.1 socket writes its key and answers a ping, and a second
 * node process joins the network through the first as its seed.
 */
final class NodeMainTest {
    private static final @NotNull InetAddress LOOPBACK = InetAddress.ofLiteral("127.0.0.1");
    private static final long RANDOM_SEED = 20261108L;
    private static final @NotNull Pattern LISTEN_LINE = Pattern.compile("listens at /127\\.0\\.0\\.1:(\\d+)");
    private static final @NotNull Pattern CONNECTED_LINE = Pattern.compile("Network state: CONNECTED");
    private static final @NotNull Pattern REACHABLE_LINE = Pattern.compile("Reachable: an unrequested hello arrived from /127\\.0\\.0\\.1:(\\d+)");
    private static final long START_SECONDS = 60L;
    private static final long PING_MILLIS = 10_000L;
    private static final long RESEND_MILLIS = 200L;
    private static final long STOP_SECONDS = 30L;
    private static final long TEST_SECONDS = 180L;

    private static @NotNull Process startNode(@NotNull Path directory, @NotNull Path state, @NotNull String @NotNull ... options) throws IOException {
        String java = ProcessHandle.current().info().command().orElseThrow();
        List<String> command = new ArrayList<>(List.of(java, "-XX:-UsePerfData", "-Djava.io.tmpdir=" + directory, "-cp",
                                                       System.getProperty("java.class.path"), NodeMain.class.getName(),
                                                       "--state", state.toString(), "--bind", "127.0.0.1:0"));

        command.addAll(List.of(options));

        return new ProcessBuilder(command).redirectErrorStream(true).start();
    }

    private static void stopNode(@NotNull Process process) throws InterruptedException {
        process.destroy();
        Assertions.assertTrue(process.waitFor(STOP_SECONDS, TimeUnit.SECONDS));
    }

    private static @NotNull BufferedReader createReader(@NotNull Process process) {
        return new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    }

    /**
     * Reads a node's output until a line matches a pattern.
     */
    private static @NotNull Matcher awaitLine(@NotNull BufferedReader reader, @NotNull Pattern pattern) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(START_SECONDS);
        String line = reader.readLine();

        while (line != null && System.nanoTime() < deadline) {
            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
                return matcher;
            }

            line = reader.readLine();
        }

        throw Exceptions.newAssertionError("The node process did not print a line matching {0}.", pattern);
    }

    /**
     * Keeps draining a node's output, so that the process never blocks on it.
     */
    private static void startDrain(@NotNull BufferedReader reader) {
        Thread drain = new Thread(() -> drain(reader), "node-output");

        drain.setDaemon(true);
        drain.start();
    }

    private static void drain(@NotNull BufferedReader reader) {
        try {
            while (reader.readLine() != null) {
                // The output is not checked after the start.
            }
        } catch (IOException exception) {
            // The process ended.
        }
    }

    private static @NotNull Envelope ping(@NotNull InetSocketAddress node) throws IOException {
        MessageEncoder encoder = new MessageEncoder(IdentityKeyPair.generate(new SecureRandom()), ProtocolClock.SYSTEM,
                                                    new SplittableRandom(RANDOM_SEED));
        ByteBuffer buffer = ByteBuffer.allocate(Envelope.MAXIMUM_DATAGRAM_LENGTH + 1);
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(PING_MILLIS);
        long request = 1L;

        try (DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)) {
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(LOOPBACK, 0));

            while (System.nanoTime() < deadline) {
                channel.send(ByteBuffer.wrap(encoder.encodePaddedRequest(new PingMessage(), request)), node);

                long resend = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(RESEND_MILLIS);

                while (System.nanoTime() < resend) {
                    buffer.clear();

                    SocketAddress source = channel.receive(buffer);
                    Envelope envelope = source == null ? null : Envelope.decode(buffer.array(), buffer.position()).getEnvelope();

                    if (envelope != null && envelope.isReply() && envelope.getRequestNumber() == request) {
                        return envelope;
                    }
                }

                request++;
            }
        }

        throw Exceptions.newAssertionError("The node process did not answer the ping at {0}.", node);
    }


    @Test
    void run_should_print_the_usage_for_help_and_refuse_an_invalid_command_line() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        try (PrintStream outputStream = new PrintStream(output, true, StandardCharsets.UTF_8);
             PrintStream errorStream = new PrintStream(error, true, StandardCharsets.UTF_8)) {
            Assertions.assertEquals(NodeExitCode.SUCCESS, NodeMain.run(new String[]{"--help"}, outputStream, errorStream));
            Assertions.assertEquals(NodeExitCode.CONFIGURATION_ERROR, NodeMain.run(new String[]{"--bind", "127.0.0.1:7455"}, outputStream, errorStream));
        }

        Assertions.assertEquals(NodeCommandLine.USAGE, output.toString(StandardCharsets.UTF_8));
        Assertions.assertTrue(error.toString(StandardCharsets.UTF_8).contains("--state"));
    }

    @Test
    @Timeout(TEST_SECONDS)
    void main_should_serve_a_ping_on_a_loopback_socket(@TempDir @NotNull Path directory) throws IOException, InterruptedException {
        Path state = directory.resolve("state");
        Process process = startNode(directory, state);

        try {
            BufferedReader output = createReader(process);
            int port = Integer.parseInt(awaitLine(output, LISTEN_LINE).group(1));

            startDrain(output);

            IdentityKeyPair nodeKey = IdentityKeyFileUtils.read(state.resolve(NodeStateFolder.KEY_FILE));
            Envelope answer = ping(new InetSocketAddress(LOOPBACK, port));

            Assertions.assertEquals(nodeKey.getPublicKey(), answer.getSender());
            Assertions.assertTrue(answer.isSignatureValid());
            Assertions.assertEquals(MeshPeer.SOFTWARE, ((PongMessage) answer.getBody()).getSoftware());
            Assertions.assertTrue(Files.isDirectory(state.resolve(NodeStateFolder.LOG_DIRECTORY)));
        } finally {
            stopNode(process);
        }
    }

    @Test
    @Timeout(TEST_SECONDS)
    void a_node_joins_the_network_through_another_node_named_as_its_seed(@TempDir @NotNull Path directory) throws IOException, InterruptedException {
        Process seed = startNode(directory, directory.resolve("seed"));
        Process joiner = null;

        try {
            BufferedReader seedOutput = createReader(seed);
            String seedPort = awaitLine(seedOutput, LISTEN_LINE).group(1);

            joiner = startNode(directory, directory.resolve("joiner"), "--peer", "127.0.0.1:" + seedPort);

            BufferedReader joinerOutput = createReader(joiner);
            String joinerPort = awaitLine(joinerOutput, LISTEN_LINE).group(1);

            // The joiner is connected once the seed answered its hello, and the seed is reachable (a full peer) since that hello came from an
            // address it never sent to.
            awaitLine(joinerOutput, CONNECTED_LINE);
            startDrain(joinerOutput);
            Assertions.assertEquals(joinerPort, awaitLine(seedOutput, REACHABLE_LINE).group(1));
            startDrain(seedOutput);
        } finally {
            if (joiner != null) {
                stopNode(joiner);
            }

            stopNode(seed);
        }
    }
}

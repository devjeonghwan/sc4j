package org.fogofwarlabs.sc4j.demo.harness;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Checks the determinism of the demo across JVMs (the build's {@code determinismCheck}): it starts runtime JVMs with the demo jar headless and
 * compares what they wrote.
 * <ul>
 *     <li>Two local runs: two JVMs play the same computer game (the same seed and map) at the same time; their per-loop state hash files
 *     and outcomes must be equal.</li>
 *     <li>A network run: a host JVM opens a room on 127.0.0.1 and writes its lobby port to a file, a joiner JVM joins it; both play the
 *     network game, in which the person at each machine orders a worker, and their per-loop state hash files, outcomes and replay files
 *     must be equal. The people's orders must change the game: the network game's hashes differ from the local game's from a game loop
 *     after its start on.</li>
 *     <li>A replay run: a JVM plays the host's replay back; its per-loop state hashes and outcome must equal the network game's.</li>
 *     <li>A Beacon.Link run: the check opens a Beacon.Link node on 127.0.0.1 in its own JVM; a host JVM publishes a room at that node, a
 *     joiner JVM finds it and joins it, and both play the same network game over the overlay's channels. Their per-loop state hashes,
 *     outcomes and replay files must be equal, and the hashes and outcome must equal those of the network run (the same seed, map and
 *     orders on another network).</li>
 * </ul>
 * Each JVM gets its own install folder under the work folder with the demo jar in {@code mods/}. The check starts every JVM itself with a
 * known command line, records its process id in the report, and stops only those processes when one fails or runs too long; the node
 * stops with the check. Arguments: the class path of the runtime, the demo jar, the work folder, the seed and the map name.
 */
public final class DeterminismCheck {
    private static final @NotNull String RUNTIME_MAIN = "org.fogofwarlabs.sc4j.runtime.RuntimeMain";
    private static final @NotNull String MOD_FILE = "demo.jar";
    private static final @NotNull String MODS_FOLDER = "mods";
    private static final @NotNull String FRAME_HASHES_FILE = "frame-hashes.txt";
    private static final @NotNull String OUTCOME_FILE = "outcome.txt";
    private static final @NotNull String PORT_FILE = "lobby-port.txt";
    private static final @NotNull String NETWORK_REPLAY_FILE = "last-network-game.sc4r";
    private static final @NotNull String REPORT_FILE = "determinism-report.txt";
    private static final @NotNull String LOOPBACK = "127.0.0.1";
    private static final long RUN_TIMEOUT_SECONDS = 300L;
    private static final long PORT_TIMEOUT_SECONDS = 60L;
    private static final long PORT_POLL_MILLIS = 50L;
    private static final long NODE_POLL_MILLIS = 20L;
    private static final long NODE_STOP_MILLIS = 5000L;
    private static final int ANY_PORT = 0;
    private static final int ARGUMENT_COUNT = 5;
    /**
     * The argument positions of the runtime class path, the demo jar, the work folder, the seed and the map name.
     */
    private static final int RUNTIME_CLASS_PATH_INDEX = 0;
    private static final int MOD_JAR_INDEX = 1;
    private static final int WORK_FOLDER_INDEX = 2;
    private static final int SEED_INDEX = 3;
    private static final int MAP_INDEX = 4;
    private static final int FAILURE_EXIT = 1;

    /**
     * Holds one started runtime JVM: its name, install folder and process.
     */
    private static final class Run {
        private final @NotNull String name;
        private final @NotNull Path install;
        private final @NotNull Process process;

        private Run(@NotNull String name, @NotNull Path install, @NotNull Process process) {
            this.name    = name;
            this.install = install;
            this.process = process;
        }
    }

    /**
     * Runs the check.
     *
     * @param arguments the class path of the runtime, the demo jar, the work folder, the seed and the map name
     */
    public static void main(@NotNull String @NotNull [] arguments) {
        if (arguments.length != ARGUMENT_COUNT) {
            System.err.println("Arguments: <runtime class path> <demo jar> <work folder> <seed> <map>");
            System.exit(FAILURE_EXIT);
        }

        DeterminismCheck check = new DeterminismCheck(arguments[RUNTIME_CLASS_PATH_INDEX], Path.of(arguments[MOD_JAR_INDEX]),
                                                      Path.of(arguments[WORK_FOLDER_INDEX]), arguments[SEED_INDEX], arguments[MAP_INDEX]);
        boolean passed;

        try {
            passed = check.run();
        } catch (IOException | RuntimeException exception) {
            check.report.add("FAILED: " + exception);
            passed = false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            check.report.add("FAILED: interrupted");
            passed = false;
        } finally {
            check.stopRuns();
        }

        try {
            Files.write(check.workFolder.resolve(REPORT_FILE), check.report, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.err.println("The report cannot be written: " + exception.getMessage());
        }

        check.report.forEach(System.out::println);

        if (!passed) {
            System.exit(FAILURE_EXIT);
        }
    }

    private static @NotNull Path findJava() {
        Path bin = Path.of(System.getProperty("java.home"), "bin");
        Path windows = bin.resolve("java.exe");

        return Files.isRegularFile(windows) ? windows : bin.resolve("java");
    }

    private static @Nullable Path findFile(@NotNull Path folder, @NotNull String fileName) throws IOException {
        try (Stream<Path> files = Files.walk(folder)) {
            return files.filter(file -> file.getFileName().toString().equals(fileName)).findFirst().orElse(null);
        }
    }

    private static void deleteFolder(@NotNull Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            return;
        }

        try (Stream<Path> files = Files.walk(folder)) {
            for (Path file : files.sorted((left, right) -> right.getNameCount() - left.getNameCount()).toList()) {
                Files.delete(file);
            }
        }
    }


    private final @NotNull String runtimeClassPath;
    private final @NotNull Path modJar;
    private final @NotNull Path workFolder;
    private final @NotNull String seed;
    private final @NotNull String map;

    private final @NotNull List<@NotNull Run> runs;
    private final @NotNull List<@NotNull String> report;

    private DeterminismCheck(@NotNull String runtimeClassPath, @NotNull Path modJar, @NotNull Path workFolder,
                             @NotNull String seed, @NotNull String map) {
        this.runtimeClassPath = runtimeClassPath;
        this.modJar           = modJar;
        this.workFolder       = workFolder;
        this.seed             = seed;
        this.map              = map;

        this.runs   = new ArrayList<>();
        this.report = new ArrayList<>();
    }

    private boolean run() throws IOException, InterruptedException {
        long start = System.nanoTime();

        deleteFolder(this.workFolder);
        Files.createDirectories(this.workFolder);
        this.report.add("Determinism check of the demo: seed " + this.seed + ", map " + this.map);

        Run first = this.start("local-a", List.of(), List.of("--seed", this.seed, "--map", this.map));
        Run second = this.start("local-b", List.of(), List.of("--seed", this.seed, "--map", this.map));
        boolean localPassed = this.await(first) & this.await(second) && this.compare("local", first, second, null);
        Run host = this.start("network-host", List.of(), List.of("--host", this.workFolder.resolve("network-host").resolve(PORT_FILE).toString(),
                                                                 "--seed", this.seed, "--map", this.map));
        String port = this.readPort(host);
        Run joiner = this.start("network-joiner", List.of("--join", LOOPBACK + ':' + port), List.of());
        boolean networkPassed = this.await(host) & this.await(joiner) && this.compare("network", host, joiner, NETWORK_REPLAY_FILE) &&
                                localPassed && this.compareChanged(first, host);
        boolean replayPassed = networkPassed && this.compareReplay(host);
        boolean linkPassed = networkPassed && this.compareBeaconLink(host);
        boolean passed = localPassed && networkPassed && replayPassed && linkPassed;

        this.report.add("Time: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " ms");
        this.report.add(passed ? "PASSED" : "FAILED");

        return passed;
    }

    /**
     * Starts a runtime JVM with the demo headless in its own install folder; its output goes to {@code output.txt} there.
     */
    private @NotNull Run start(@NotNull String name, @NotNull List<@NotNull String> runtimeOptions, @NotNull List<@NotNull String> demoArguments) throws IOException {
        Path install = this.workFolder.resolve(name);
        Path mod = install.resolve(MODS_FOLDER).resolve(MOD_FILE);
        List<String> command = new ArrayList<>(List.of(findJava().toString(), "-cp", this.runtimeClassPath, RUNTIME_MAIN,
                                                       "--install", install.toString(), "--mod", mod.toString(), "--headless"));

        Files.createDirectories(mod.getParent());
        Files.copy(this.modJar, mod, StandardCopyOption.REPLACE_EXISTING);
        command.addAll(runtimeOptions);
        command.add("--");
        command.addAll(demoArguments);
        command.addAll(List.of("--frame-hashes", install.resolve(FRAME_HASHES_FILE).toString(),
                               "--outcome", install.resolve(OUTCOME_FILE).toString()));

        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(install.resolve("output.txt").toFile()).start();
        Run run = new Run(name, install, process);

        this.runs.add(run);
        this.report.add(name + ": process " + process.pid() + ": " + String.join(" ", command));

        return run;
    }

    private @NotNull String readPort(@NotNull Run host) throws IOException, InterruptedException {
        Path portFile = host.install.resolve(PORT_FILE);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(PORT_TIMEOUT_SECONDS);

        while (!Files.isRegularFile(portFile)) {
            if (!host.process.isAlive() || System.nanoTime() > deadline) {
                throw Exceptions.newIOException("The host {0} wrote no lobby port.", host.name);
            }

            Thread.sleep(PORT_POLL_MILLIS);
        }

        return Files.readString(portFile, StandardCharsets.UTF_8).trim();
    }

    private boolean await(@NotNull Run run) throws InterruptedException {
        long start = System.nanoTime();
        boolean finished = run.process.waitFor(RUN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        int exit = finished ? run.process.exitValue() : -1;

        this.report.add(run.name + ": process " + run.process.pid() + (finished ? " exited with " + exit : " ran too long") + " after " +
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " ms of waiting");

        return finished && exit == 0;
    }

    private boolean compare(@NotNull String kind, @NotNull Run first, @NotNull Run second, @Nullable String replayFile) throws IOException {
        List<String> firstHashes = Files.readAllLines(first.install.resolve(FRAME_HASHES_FILE));
        List<String> secondHashes = Files.readAllLines(second.install.resolve(FRAME_HASHES_FILE));
        String firstOutcome = Files.readString(first.install.resolve(OUTCOME_FILE)).trim();
        String secondOutcome = Files.readString(second.install.resolve(OUTCOME_FILE)).trim();
        boolean hashesEqual = !firstHashes.isEmpty() && firstHashes.equals(secondHashes);
        boolean outcomesEqual = firstOutcome.equals(secondOutcome);
        boolean replaysEqual = true;

        this.report.add(kind + ": " + firstHashes.size() + " and " + secondHashes.size() + " per-loop state hashes, " +
                        (hashesEqual ? "equal" : "different"));
        this.report.add(kind + ": outcome [" + firstOutcome.replace('\n', ' ') + "] and [" + secondOutcome.replace('\n', ' ') + "], " +
                        (outcomesEqual ? "equal" : "different"));

        if (replayFile != null) {
            Path firstReplay = findFile(first.install, replayFile);
            Path secondReplay = findFile(second.install, replayFile);

            replaysEqual = firstReplay != null && secondReplay != null &&
                           Arrays.equals(Files.readAllBytes(firstReplay), Files.readAllBytes(secondReplay));
            this.report.add(kind + ": replays " + firstReplay + " and " + secondReplay + ", " + (replaysEqual ? "equal" : "different or missing"));
        }

        return hashesEqual && outcomesEqual && replaysEqual;
    }

    /**
     * Compares the per-loop state hashes of the local game and of the network game of the same seed and map: the people's orders of the
     * network game must make them differ from a game loop after the start on.
     */
    private boolean compareChanged(@NotNull Run local, @NotNull Run network) throws IOException {
        List<String> localHashes = Files.readAllLines(local.install.resolve(FRAME_HASHES_FILE));
        List<String> networkHashes = Files.readAllLines(network.install.resolve(FRAME_HASHES_FILE));
        int firstDifference = 0;

        while (firstDifference < localHashes.size() && firstDifference < networkHashes.size() &&
               localHashes.get(firstDifference).equals(networkHashes.get(firstDifference))) {
            firstDifference++;
        }

        boolean changed = firstDifference > 0 && !localHashes.equals(networkHashes);

        this.report.add("orders: the network game's state hashes " +
                        (changed ? "leave the local game's after game loop " + (firstDifference + 1)
                                 : "do not leave the local game's after its start"));

        return changed;
    }

    /**
     * Plays the host's replay of the network game back in a new JVM and compares its per-loop state hashes and outcome with the host's.
     */
    private boolean compareReplay(@NotNull Run host) throws IOException, InterruptedException {
        Path replay = findFile(host.install, NETWORK_REPLAY_FILE);

        if (replay == null) {
            this.report.add("replay: the host wrote no replay");

            return false;
        }

        Run playback = this.start("network-replay", List.of("--replay", replay.toString()), List.of());

        return this.await(playback) && this.compare("replay", host, playback, null);
    }

    /**
     * Plays the network game over Beacon.Link: opens a node on 127.0.0.1 in this JVM, starts a host JVM that publishes a room there and a
     * joiner JVM that joins it, and compares their state hashes, outcomes and replays, and the hashes and outcome with the network run's.
     */
    private boolean compareBeaconLink(@NotNull Run network) throws IOException, InterruptedException {
        PeerSocket node = PeerSocket.open(PeerConfiguration.createDefault(), IdentityKeyPair.generate(new SecureRandom()),
                                          new InetSocketAddress(LOOPBACK, ANY_PORT), ProtocolClock.SYSTEM, new SecureRandom());
        Thread loop = new Thread(() -> {
            try {
                while (!node.isStopped()) {
                    node.poll(NODE_POLL_MILLIS);
                }
            } catch (IOException exception) {
                throw Exceptions.newUncheckedIOException(exception);
            }
        }, "beacon-link-node");
        String address = LOOPBACK + ':' + node.getLocalAddress().getPort();

        loop.setDaemon(true);
        loop.start();
        this.report.add("beaconlink: node at " + address + " in the check's JVM");

        try {
            // The runtime's --seed names the node; the demo's --seed after -- is the game's random seed.
            Run host = this.start("beaconlink-host", List.of("--seed", address),
                                  List.of("--beaconlink-host", "--seed", this.seed, "--map", this.map));
            Run joiner = this.start("beaconlink-joiner", List.of("--seed", address),
                                    List.of("--beaconlink-join", "--seed", this.seed, "--map", this.map));

            return this.await(host) & this.await(joiner) && this.compare("beaconlink", host, joiner, NETWORK_REPLAY_FILE) &&
                   this.compare("beaconlink and network", network, host, null);
        } finally {
            node.stop();
            loop.join(NODE_STOP_MILLIS);
            node.close();
        }
    }

    /**
     * Stops the started JVMs that still run: only the processes this check started.
     */
    private void stopRuns() {
        for (Run run : this.runs) {
            if (run.process.isAlive()) {
                this.report.add(run.name + ": stopping process " + run.process.pid());
                run.process.destroyForcibly();
            }
        }
    }
}

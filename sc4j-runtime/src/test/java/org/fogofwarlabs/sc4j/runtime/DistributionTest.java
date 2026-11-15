package org.fogofwarlabs.sc4j.runtime;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.peer.PeerSocket;
import org.fogofwarlabs.sc4j.beaconlink.peer.bootstrap.PeerStore;
import org.fogofwarlabs.sc4j.beaconlink.peer.configuration.PeerConfiguration;
import org.fogofwarlabs.sc4j.beaconlink.protocol.ProtocolClock;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.NodeListFile;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.NodeListReading;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKeyPair;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.fogofwarlabs.sc4j.manifest.test.TestJarUtils;
import org.fogofwarlabs.sc4j.runtime.cli.ExitCode;
import org.fogofwarlabs.sc4j.testmod.BeaconLinkMod;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Tests the built user package as a user unpacks it on a fresh computer without Java: it holds the start of the launcher, its own Java
 * runtime, the launcher with the shipped seed list, the runtime, mods and the licence files but no user data and no cache; the launcher,
 * started from the package root, lists the mods with the package's Java; and its runtime, started headless with no user data and no Java of
 * the computer and with the Beacon.Link options the launcher passes a fresh install (the shipped seed list, here naming a node on 127.0.0.1,
 * and the install's peers file), opens a Beacon.Link session and keeps the node in the peers file without writing a node list of the user.
 */
@RequiresDistribution
final class DistributionTest {
    private static final @NotNull InetAddress LOOPBACK = InetAddress.ofLiteral("127.0.0.1");
    private static final @NotNull String MOD_ID = "beacon-test";
    private static final @NotNull String MANIFEST = """
                                                    {"format": 1, "id": "beacon-test", "name": {"en": "Beacon test"}, "version": "1.0.0", "engine": "0.1", "entry": "%s"}
                                                    """;
    private static final @NotNull String WINDOWS_SCRIPT = "sc4j-runtime.bat";
    private static final @NotNull String UNIX_SCRIPT = "sc4j-runtime";
    private static final @NotNull String WINDOWS_START = "SC4J.bat";
    private static final @NotNull String UNIX_START = "SC4J";
    private static final @NotNull String JAVA_HOME_VARIABLE = "JAVA_HOME";
    private static final @NotNull String PATH_VARIABLE = "PATH";
    private static final @NotNull List<@NotNull String> JAVA_COMMANDS = List.of("java.exe", "java");
    private static final @NotNull String JAVA_RELEASE_FILE = "java/release";
    private static final @NotNull String JAVA_VERSION = "JAVA_VERSION=\"" + Runtime.version().feature();
    private static final @NotNull String OPERATING_SYSTEM_PROPERTY = "os.name";
    private static final @NotNull String WINDOWS_NAME_PREFIX = "windows";
    private static final @NotNull String NATIVE_ENCODING_PROPERTY = "native.encoding";
    private static final @NotNull String CACHE_FOLDER = ".cache";
    private static final long NODE_POLL_MILLIS = 20L;
    private static final long RUN_SECONDS = 150L;
    private static final long STOP_SECONDS = 10L;
    private static final long TEST_SECONDS = 240L;

    private static boolean isWindows() {
        return System.getProperty(OPERATING_SYSTEM_PROPERTY, "").toLowerCase(Locale.ROOT).startsWith(WINDOWS_NAME_PREFIX);
    }

    /**
     * Unpacks the user package into a folder and returns the install it holds: the package's only top folder.
     */
    private static @NotNull InstallLayout unpack(@NotNull Path directory) throws IOException {
        Path target = Files.createDirectories(directory.resolve("unpacked")).toAbsolutePath().normalize();

        try (ZipFile archive = new ZipFile(RuntimeTestResources.getDistribution().toFile())) {
            for (ZipEntry entry : Collections.list(archive.entries())) {
                Path file = target.resolve(entry.getName()).normalize();

                if (!file.startsWith(target)) {
                    throw Exceptions.newIOException("The package entry {0} leaves the package folder.", entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(file);
                } else {
                    Files.createDirectories(file.getParent());

                    try (InputStream input = archive.getInputStream(entry)) {
                        Files.copy(input, file);
                    }
                }
            }
        }

        List<Path> roots;

        try (Stream<Path> listed = Files.list(target)) {
            roots = listed.toList();
        }

        Assertions.assertEquals(1, roots.size(), "the package holds one folder: " + roots);

        return InstallLayout.of(roots.getFirst());
    }

    private static @NotNull String readOutput(@NotNull Path output) throws IOException {
        Charset charset = Charset.forName(System.getProperty(NATIVE_ENCODING_PROPERTY, Charset.defaultCharset().name()), Charset.defaultCharset());

        return new String(Files.readAllBytes(output), charset);
    }

    /**
     * Takes the computer's Java away from a process: no {@code JAVA_HOME} and no folder of {@code PATH} that holds a {@code java} command.
     */
    private static void hideComputerJava(@NotNull Map<@NotNull String, @NotNull String> environment) {
        List<String> kept = new ArrayList<>();

        environment.remove(JAVA_HOME_VARIABLE);

        for (String folder : environment.getOrDefault(PATH_VARIABLE, "").split(File.pathSeparator)) {
            if (!folder.isBlank() && !holdsJava(folder)) {
                kept.add(folder);
            }
        }

        environment.put(PATH_VARIABLE, String.join(File.pathSeparator, kept));
    }

    private static boolean holdsJava(@NotNull String folder) {
        try {
            Path directory = Path.of(folder.strip());

            return JAVA_COMMANDS.stream().anyMatch(command -> Files.isRegularFile(directory.resolve(command)));
        } catch (InvalidPathException exception) {
            return false;
        }
    }

    private static void makeExecutable(@NotNull Path script) throws IOException {
        if (!isWindows()) {
            Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
    }

    private static void stop(@NotNull Process process) throws InterruptedException {
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        process.waitFor(STOP_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    void user_package_holds_the_programs_mods_and_licences_without_user_data(@TempDir @NotNull Path directory) throws IOException,
                                                                                                                      ManifestException {
        InstallLayout install = unpack(directory);
        Path root = install.getRoot();
        List<Path> modJars;
        List<Path> caches;

        for (String file : List.of("LICENSE", "NOTICE", "licenses", WINDOWS_START, UNIX_START, "launcher/bin/sc4j-launcher",
                                   "launcher/bin/sc4j-launcher.bat", "runtime/bin/" + UNIX_SCRIPT, "runtime/bin/" + WINDOWS_SCRIPT,
                                   "runtime/lib/natives", JAVA_RELEASE_FILE, "java/legal")) {
            Assertions.assertTrue(Files.exists(root.resolve(file)), file);
        }

        Assertions.assertTrue(Files.readString(root.resolve(JAVA_RELEASE_FILE), StandardCharsets.UTF_8).contains(JAVA_VERSION),
                              "the package's Java runs the class files of this build");

        try (Stream<Path> listed = Files.list(install.getModsDirectory())) {
            modJars = listed.toList();
        }

        try (Stream<Path> walked = Files.walk(root)) {
            caches = walked.filter(path -> path.getFileName().toString().equals(CACHE_FOLDER)).toList();
        }

        NodeListReading seeds = new NodeListFile(install.getSeedsFile()).read();

        Assertions.assertNull(seeds.getProblem());
        Assertions.assertFalse(seeds.getNodes().isEmpty(), "the shipped seed list names a node");
        Assertions.assertFalse(Files.exists(install.getUserDataDirectory()));
        Assertions.assertEquals(List.of(), caches);
        Assertions.assertFalse(modJars.isEmpty(), "the package holds mods");

        for (Path jar : modJars) {
            Assertions.assertEquals(jar.getFileName().toString(), ModManifestReader.read(jar).getManifest().getId() + ".jar");
        }
    }

    @Test
    @Timeout(TEST_SECONDS)
    void the_start_of_the_package_lists_the_mods_with_the_package_java(@TempDir @NotNull Path directory) throws IOException, InterruptedException {
        InstallLayout install = unpack(directory);
        Path start = install.getRoot().resolve(isWindows() ? WINDOWS_START : UNIX_START);
        Path output = directory.resolve("launcher-output.txt");

        makeExecutable(start);
        makeExecutable(install.getLauncherDirectory().resolve("bin").resolve("sc4j-launcher"));

        ProcessBuilder builder = new ProcessBuilder(start.toString(), "--list");

        builder.directory(directory.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(output.toFile());
        hideComputerJava(builder.environment());

        Process launcher = builder.start();

        try {
            launcher.getOutputStream().close();
            Assertions.assertTrue(launcher.waitFor(RUN_SECONDS, TimeUnit.SECONDS), "the launcher ends in time");
        } finally {
            stop(launcher);
        }

        String text = readOutput(output);

        Assertions.assertEquals(0, launcher.exitValue(), text);
        Assertions.assertTrue(text.contains("Mods in " + install.getModsDirectory()), text);

        try (Stream<Path> listed = Files.list(install.getModsDirectory())) {
            for (Path jar : listed.toList()) {
                Assertions.assertTrue(text.contains(jar.getFileName().toString()), text);
            }
        }
    }

    @Test
    @Timeout(TEST_SECONDS)
    void a_fresh_install_opens_a_beacon_link_session_through_the_shipped_seed_list(@TempDir @NotNull Path directory) throws IOException,
                                                                                                                            InterruptedException {
        InstallLayout install = unpack(directory);
        Path modJar = install.getModsDirectory().resolve(MOD_ID + ".jar");
        Path script = install.getRuntimeScript(isWindows() ? WINDOWS_SCRIPT : UNIX_SCRIPT);
        Path output = directory.resolve("runtime-output.txt");
        Path peersFile = install.getBeaconLinkDirectory().resolve(PeerStore.FILE_NAME);

        Assertions.assertFalse(Files.exists(install.getUserDataDirectory()));
        TestJarUtils.write(modJar, TestJarUtils.createEntries(MANIFEST.formatted(BeaconLinkMod.class.getName()), BeaconLinkMod.class));

        makeExecutable(script);

        try (PeerSocket node = PeerSocket.open(PeerConfiguration.createDefault(), IdentityKeyPair.generate(new SecureRandom()),
                                               new InetSocketAddress(LOOPBACK, 0), ProtocolClock.SYSTEM, new SecureRandom())) {
            // The shipped seed list names the public node; the test lists the node on 127.0.0.1 in its place.
            Files.writeString(install.getSeedsFile(), "{\"format\": 1, \"nodes\": [\"127.0.0.1:" + node.getLocalAddress().getPort() + "\"]}",
                              StandardCharsets.UTF_8);

            ProcessBuilder builder = new ProcessBuilder(script.toString(), "--install", install.getRoot().toString(), "--mod", modJar.toString(),
                                                        "--seeds", install.getSeedsFile().toString(), "--peers", peersFile.toString(), "--headless");

            builder.directory(install.getRoot().toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(output.toFile());
            hideComputerJava(builder.environment());

            Process runtime = builder.start();

            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(RUN_SECONDS);

                runtime.getOutputStream().close();

                while (runtime.isAlive() && System.nanoTime() < deadline) {
                    node.poll(NODE_POLL_MILLIS);
                }

                Assertions.assertFalse(runtime.isAlive(), "the runtime ends in time");
            } finally {
                stop(runtime);
            }

            Assertions.assertEquals(ExitCode.SUCCESS.getStatus(), runtime.exitValue(), readOutput(output));
            Assertions.assertTrue(PeerStore.open(peersFile).getPeers().stream().anyMatch(peer -> peer.getKey().equals(node.getPeer().getKey())),
                                  "the runtime keeps the node in the peers file");
        }

        String run = Files.readString(install.getStorageDirectory(MOD_ID).resolve(BeaconLinkMod.RUN_FILE), StandardCharsets.UTF_8);

        Assertions.assertTrue(run.startsWith(BeaconLinkMod.REGISTERED + " ") && run.contains(LOOPBACK.getHostAddress()), run);
        Assertions.assertFalse(Files.exists(install.getNodesFile()), "the runtime writes no node list of the user");
    }
}

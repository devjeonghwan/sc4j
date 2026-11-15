package org.fogofwarlabs.sc4j.beaconlink.protocol.file;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.io.AtomicFileUtils;
import org.fogofwarlabs.sc4j.base.json.JsonReader;
import org.fogofwarlabs.sc4j.base.json.JsonWriterUtils;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

/**
 * Reads and writes the node list of an install, {@code <install>/userdata/beaconlink/}{@value #FILE_NAME}: the launcher edits it and the
 * runtime's Beacon.Link client reads it, both through this class. The file is JSON, format {@value #FORMAT}:
 * {@snippet lang = "json":
 * {
 *   "format": 1,
 *   "nodes": ["beacon.example.org:7455", {"address": "[2001:db8::1]:7455", "key": "<64 hexadecimal digits>"}]
 * }
 *}
 * A plain entry accepts any node key; an object entry pins the Ed25519 key the node must answer with ({@code key} may be left out). Reading
 * never throws: a file that cannot be used gives a {@link NodeListProblem} and no node. Writing keeps the pinned keys, writes an entry
 * without a key as a plain string and replaces the file atomically where the file system allows.
 * <p>
 * The seed list an install ships ({@code <install>/launcher/seeds.json}) has the same format. Until the user saves a node list, the
 * launcher and the runtime use the seed list in its place ({@link #selectExistingOr}).
 */
public final class NodeListFile {
    /**
     * The name of the node list file in the install's Beacon.Link folder.
     */
    public static final @NotNull String FILE_NAME = "nodes.json";
    /**
     * The node list format this release reads and writes.
     */
    public static final int FORMAT = 1;
    /**
     * The longest node list file read, in bytes.
     */
    public static final int MAXIMUM_FILE_LENGTH = 64 * 1024;

    private static final @NotNull String FORMAT_KEY = "format";
    private static final @NotNull String NODES_KEY = "nodes";
    private static final @NotNull String ADDRESS_KEY = "address";
    private static final @NotNull String KEY_KEY = "key";
    /**
     * The deepest nesting of arrays and objects read: the root object, the {@code nodes} array and its entry objects need three; deeper input
     * is refused before it reaches the recursive JSON parser.
     */
    private static final int MAXIMUM_NESTING_DEPTH = 8;

    private static @NotNull NodeListReading failed(@NotNull NodeListProblemKind kind, int entryIndex, @NotNull String detail) {
        return new NodeListReading(List.of(), new NodeListProblem(kind, entryIndex, detail));
    }

    private static boolean exceedsNestingDepth(byte @NotNull [] contents) {
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;

        for (byte value : contents) {
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (value == '\\') {
                    escaped = true;
                } else if (value == '"') {
                    inString = false;
                }
            } else if (value == '"') {
                inString = true;
            } else if (value == '[' || value == '{') {
                depth++;

                if (depth > MAXIMUM_NESTING_DEPTH) {
                    return true;
                }
            } else if (value == ']' || value == '}') {
                depth--;
            }
        }

        return false;
    }

    private static @NotNull ConfiguredNode readEntry(@Nullable Object entry) {
        if (entry instanceof String address) {
            return ConfiguredNode.parse(address, null);
        }

        if (entry instanceof Map<?, ?> object && object.get(ADDRESS_KEY) instanceof String address) {
            Object key = object.get(KEY_KEY);

            if (key == null) {
                return ConfiguredNode.parse(address, null);
            }

            if (key instanceof String hex) {
                return ConfiguredNode.parse(address, IdentityKey.of(HexFormat.of().parseHex(hex)));
            }
        }

        throw Exceptions.newIllegalArgumentException("Invalid node entry: {0}", entry);
    }


    private final @NotNull Path path;

    /**
     * Creates a {@link NodeListFile} instance; nothing is read or created.
     *
     * @param path the file path, normally {@code <install>/userdata/beaconlink/nodes.json}
     */
    public NodeListFile(@NotNull Path path) {
        this.path = path;
    }

    /**
     * @return the file path
     */
    public @NotNull Path getPath() {
        return this.path;
    }


    /**
     * Selects the node list a reader uses: this file once it exists, otherwise the fallback. The user's node list falls back to the seed list
     * the install ships, so that an install whose user never saved a node list uses the shipped seeds, and a saved list, an empty one
     * included, replaces them.
     *
     * @param fallback the node list used while this file does not exist
     * @return this file if it exists, otherwise {@code fallback}
     */
    public @NotNull NodeListFile selectExistingOr(@NotNull NodeListFile fallback) {
        return Files.exists(this.path) ? this : fallback;
    }

    /**
     * Reads the node list. A missing file holds no node; a file that cannot be used holds no node and gives the reason.
     *
     * @return the nodes in list order, or the problem of the file
     */
    public @NotNull NodeListReading read() {
        byte[] contents;

        try {
            if (Files.size(this.path) > MAXIMUM_FILE_LENGTH) {
                return failed(NodeListProblemKind.TOO_LARGE, NodeListProblem.NO_ENTRY,
                              Exceptions.format("{0} is longer than {1} bytes", this.path, MAXIMUM_FILE_LENGTH));
            }

            contents = Files.readAllBytes(this.path);
        } catch (NoSuchFileException exception) {
            return new NodeListReading(List.of(), null);
        } catch (IOException exception) {
            return failed(NodeListProblemKind.UNREADABLE, NodeListProblem.NO_ENTRY, String.valueOf(exception.getMessage()));
        }

        if (exceedsNestingDepth(contents)) {
            return failed(NodeListProblemKind.NOT_JSON, NodeListProblem.NO_ENTRY,
                          Exceptions.format("arrays and objects nest deeper than {0} levels", MAXIMUM_NESTING_DEPTH));
        }

        Object root;

        try {
            root = JsonReader.parse(contents);
        } catch (IOException exception) {
            return failed(NodeListProblemKind.NOT_JSON, NodeListProblem.NO_ENTRY, String.valueOf(exception.getMessage()));
        }

        if (!(root instanceof Map<?, ?> object) || !(object.get(FORMAT_KEY) instanceof Long format) ||
            !(object.get(NODES_KEY) instanceof List<?> entries)) {
            return failed(NodeListProblemKind.NOT_A_NODE_LIST, NodeListProblem.NO_ENTRY, "expected {\"format\": 1, \"nodes\": [...]}");
        }

        if (format != FORMAT) {
            return failed(NodeListProblemKind.UNSUPPORTED_FORMAT, NodeListProblem.NO_ENTRY,
                          Exceptions.format("format {0}, expected {1}", format, FORMAT));
        }

        List<ConfiguredNode> nodes = new ArrayList<>(entries.size());

        for (int index = 0; index < entries.size(); index++) {
            try {
                nodes.add(readEntry(entries.get(index)));
            } catch (IllegalArgumentException exception) {
                return failed(NodeListProblemKind.INVALID_ENTRY, index, String.valueOf(exception.getMessage()));
            }
        }

        return new NodeListReading(nodes, null);
    }

    /**
     * Writes the node list, replacing the file atomically where the file system allows; the folder is created when missing.
     *
     * @param nodes the nodes in list order
     * @throws IOException if the file cannot be written
     */
    public void write(@NotNull List<@NotNull ConfiguredNode> nodes) throws IOException {
        List<Object> entries = new ArrayList<>(nodes.size());

        for (ConfiguredNode node : nodes) {
            IdentityKey key = node.getKey();

            if (key == null) {
                entries.add(node.toString());
            } else {
                Map<String, Object> entry = new LinkedHashMap<>();

                entry.put(ADDRESS_KEY, node.toString());
                entry.put(KEY_KEY, HexFormat.of().formatHex(key.getBytes()));
                entries.add(entry);
            }
        }

        Map<String, Object> root = new LinkedHashMap<>();

        root.put(FORMAT_KEY, (long) FORMAT);
        root.put(NODES_KEY, entries);
        AtomicFileUtils.write(this.path, JsonWriterUtils.write(root).getBytes(StandardCharsets.UTF_8));
    }
}

package org.fogofwarlabs.sc4j.launcher.nodes;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.file.ConfiguredNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Edits the Beacon.Link node list (the seeds) without a user interface: it checks the editor's fields (host and port; keys are automatic and
 * never entered) with the reasons of a refusal, and computes the list after adding, replacing, removing or moving an entry. A key pinned by
 * a hand-written entry of the file (an advanced option) is kept silently: untouched entries keep it, and replacing an entry by one of the
 * same address keeps it. The editor proposes a list and keeps its own list unchanged until the caller has saved the proposal and calls
 * {@link #setNodes(List)}, so a failed save changes nothing.
 */
public final class NodeListEditor {
    /**
     * The most nodes the editor adds to a list.
     */
    public static final int MAXIMUM_NODES = 32;
    /**
     * The edited position of a new entry: no entry of the list is replaced.
     */
    public static final int NEW_ENTRY = -1;


    private final @NotNull List<@NotNull ConfiguredNode> nodes;

    /**
     * Creates a {@link NodeListEditor} instance.
     *
     * @param nodes the saved node list
     */
    public NodeListEditor(@NotNull List<@NotNull ConfiguredNode> nodes) {
        this.nodes = new ArrayList<>(nodes);
    }

    /**
     * @return the saved node list in list order
     */
    public @NotNull List<@NotNull ConfiguredNode> getNodes() {
        return List.copyOf(this.nodes);
    }

    /**
     * Sets the node list after a proposal was saved.
     *
     * @param nodes the saved node list
     */
    public void setNodes(@NotNull List<@NotNull ConfiguredNode> nodes) {
        this.nodes.clear();
        this.nodes.addAll(nodes);
    }


    /**
     * Checks the editor's fields for a new entry or for the replacement of an entry. Surrounding white space is ignored. The replacement of an
     * entry by one of the same address keeps the entry's pinned key; a new entry has none.
     *
     * @param hostText    the host field: a host name, an IPv4 address or an IPv6 address without brackets
     * @param portText    the port field
     * @param editedIndex the position of the replaced entry, or {@link #NEW_ENTRY} for a new entry
     * @return the entry, or the reasons the fields make none
     */
    public @NotNull NodeEntryCheck check(@NotNull String hostText, @NotNull String portText, int editedIndex) {
        Set<NodeEntryProblem> problems = EnumSet.noneOf(NodeEntryProblem.class);
        String host = hostText.strip();
        int port = ConfiguredNode.parsePort(portText.strip());

        if (!ConfiguredNode.isValidHost(host)) {
            problems.add(NodeEntryProblem.HOST_INVALID);
        }

        if (port < 0) {
            problems.add(NodeEntryProblem.PORT_INVALID);
        }

        if (editedIndex == NEW_ENTRY && this.nodes.size() >= MAXIMUM_NODES) {
            problems.add(NodeEntryProblem.LIST_FULL);
        }

        if (!problems.isEmpty()) {
            return new NodeEntryCheck(null, new ArrayList<>(problems));
        }

        ConfiguredNode node = ConfiguredNode.of(host, port, null);

        for (int index = 0; index < this.nodes.size(); index++) {
            if (index != editedIndex && this.nodes.get(index).hasSameAddress(node)) {
                return new NodeEntryCheck(null, List.of(NodeEntryProblem.DUPLICATE));
            }
        }

        if (editedIndex != NEW_ENTRY) {
            ConfiguredNode edited = this.nodes.get(this.checkIndex(editedIndex));

            if (edited.getKey() != null && edited.hasSameAddress(node)) {
                node = ConfiguredNode.of(host, port, edited.getKey());
            }
        }

        return new NodeEntryCheck(node, List.of());
    }

    /**
     * Proposes the list with an entry added at its end.
     *
     * @param node the checked entry
     * @return the proposed list
     */
    public @NotNull List<@NotNull ConfiguredNode> add(@NotNull ConfiguredNode node) {
        List<ConfiguredNode> proposal = new ArrayList<>(this.nodes);

        proposal.add(node);

        return proposal;
    }

    /**
     * Proposes the list with one entry replaced.
     *
     * @param index the position of the replaced entry
     * @param node  the checked entry
     * @return the proposed list
     * @throws IndexOutOfBoundsException if the position is outside the list
     */
    public @NotNull List<@NotNull ConfiguredNode> replace(int index, @NotNull ConfiguredNode node) {
        List<ConfiguredNode> proposal = new ArrayList<>(this.nodes);

        proposal.set(this.checkIndex(index), node);

        return proposal;
    }

    /**
     * Proposes the list without one entry.
     *
     * @param index the position of the removed entry
     * @return the proposed list
     * @throws IndexOutOfBoundsException if the position is outside the list
     */
    public @NotNull List<@NotNull ConfiguredNode> remove(int index) {
        List<ConfiguredNode> proposal = new ArrayList<>(this.nodes);

        proposal.remove(this.checkIndex(index));

        return proposal;
    }

    /**
     * Checks whether an entry can move to a position: both positions are inside the list and differ.
     *
     * @param index       the position of the moved entry
     * @param targetIndex the position the entry moves to
     * @return {@code true} if the entry can move there, {@code false} otherwise
     */
    public boolean canMove(int index, int targetIndex) {
        return index != targetIndex && index >= 0 && index < this.nodes.size() && targetIndex >= 0 && targetIndex < this.nodes.size();
    }

    /**
     * Proposes the list with one entry moved to another position; the entries between shift by one. The client contacts the listed nodes
     * in list order.
     *
     * @param index       the position of the moved entry
     * @param targetIndex the position the entry moves to
     * @return the proposed list
     * @throws IndexOutOfBoundsException if a position is outside the list
     */
    public @NotNull List<@NotNull ConfiguredNode> move(int index, int targetIndex) {
        List<ConfiguredNode> proposal = new ArrayList<>(this.nodes);
        ConfiguredNode node = proposal.remove(this.checkIndex(index));

        proposal.add(this.checkIndex(targetIndex), node);

        return proposal;
    }

    private int checkIndex(int index) {
        if (index < 0 || index >= this.nodes.size()) {
            throw Exceptions.newIndexOutOfBoundsException("The node position {0} is outside 0..{1}.", index, this.nodes.size() - 1);
        }

        return index;
    }
}

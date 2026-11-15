package org.fogofwarlabs.sc4j.launcher.peer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lists the roles of the launcher's node ({@link LauncherNode}), the setting "Help other players connect".
 */
public enum NodeRole {
    /**
     * The default: the node listens on its port and becomes a full peer when its reachability self-test proves that strangers reach it; it
     * stays a leaf otherwise.
     */
    AUTOMATIC("automatic"),
    /**
     * The node is a leaf only, on a free port: it joins the network and keeps the shared peers file up to date, but never serves strangers.
     */
    OFF("off");

    /**
     * Finds a role by its name in the node settings file.
     *
     * @param name the name, such as {@code automatic}
     * @return the role, or {@code null} if no role has the name
     */
    public static @Nullable NodeRole fromName(@NotNull String name) {
        for (NodeRole role : values()) {
            if (role.name.equals(name)) {
                return role;
            }
        }

        return null;
    }


    private final @NotNull String name;

    /**
     * Creates a {@link NodeRole} instance.
     *
     * @param name the name in the node settings file
     */
    NodeRole(@NotNull String name) {
        this.name = name;
    }

    /**
     * @return the name in the node settings file, such as {@code automatic}
     */
    public @NotNull String getName() {
        return this.name;
    }
}

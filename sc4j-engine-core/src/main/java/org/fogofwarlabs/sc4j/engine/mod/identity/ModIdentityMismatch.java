package org.fogofwarlabs.sc4j.engine.mod.identity;

import org.jetbrains.annotations.NotNull;

/**
 * Names the first part in which two {@link ModIdentity}s differ ({@link ModIdentity#findMismatch}), the reason a lobby refuses a joiner or a
 * replay refuses to load.
 */
public enum ModIdentityMismatch {
    /**
     * The engine API versions differ.
     */
    ENGINE_VERSION("engine version"),
    /**
     * The mods differ.
     */
    MOD_ID("mod"),
    /**
     * The mod versions differ.
     */
    MOD_VERSION("mod version"),
    /**
     * The contents of the mod jars differ.
     */
    MOD_CONTENT("mod content"),
    /**
     * The identity data the mods added differ, for example the version of external game data.
     */
    IDENTITY_DATA("identity data");

    private final @NotNull String description;

    ModIdentityMismatch(@NotNull String description) {
        this.description = description;
    }

    /**
     * @return the differing part in words, for messages
     */
    public @NotNull String getDescription() {
        return this.description;
    }
}

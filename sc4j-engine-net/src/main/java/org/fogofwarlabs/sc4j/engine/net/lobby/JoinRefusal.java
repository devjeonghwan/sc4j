package org.fogofwarlabs.sc4j.engine.net.lobby;

import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentityMismatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Names why a {@link NetworkLobby} host refused a joiner: another lobby protocol, a mod identity that differs (by its first differing part,
 * {@link ModIdentityMismatch}), a full room, a started game or a wrong password of a private room. The code is the reason's byte on the wire.
 */
public enum JoinRefusal {
    /**
     * The joiner speaks another lobby protocol version or sent an unreadable greeting.
     */
    PROTOCOL_VERSION(1, "another lobby protocol"),
    /**
     * The joiner runs another engine API version.
     */
    ENGINE_VERSION(2, "another engine version"),
    /**
     * The joiner runs another mod.
     */
    MOD_ID(3, "another mod"),
    /**
     * The joiner runs another version of the mod.
     */
    MOD_VERSION(4, "another mod version"),
    /**
     * The joiner's mod jar has other contents.
     */
    MOD_CONTENT(5, "other mod contents"),
    /**
     * The joiner's mod added other identity data, for example another version of external game data.
     */
    IDENTITY_DATA(6, "other identity data"),
    /**
     * The room has no free place.
     */
    ROOM_FULL(7, "the room is full"),
    /**
     * The room's game already started.
     */
    GAME_STARTED(8, "the game already started"),
    /**
     * The joiner did not prove the private room's password.
     */
    WRONG_PASSWORD(9, "wrong password");

    /**
     * Returns the refusal of a mod identity mismatch.
     *
     * @param mismatch the first differing part of the identities
     * @return the refusal
     */
    public static @NotNull JoinRefusal of(@NotNull ModIdentityMismatch mismatch) {
        return switch (mismatch) {
            case ENGINE_VERSION -> ENGINE_VERSION;
            case MOD_ID -> MOD_ID;
            case MOD_VERSION -> MOD_VERSION;
            case MOD_CONTENT -> MOD_CONTENT;
            case IDENTITY_DATA -> IDENTITY_DATA;
        };
    }

    /**
     * Finds the refusal of a wire code.
     *
     * @param code the code
     * @return the refusal, or {@code null} if no refusal has the code
     */
    public static @Nullable JoinRefusal find(int code) {
        for (JoinRefusal refusal : values()) {
            if (refusal.code == code) {
                return refusal;
            }
        }

        return null;
    }


    private final int code;
    private final @NotNull String description;

    JoinRefusal(int code, @NotNull String description) {
        this.code        = code;
        this.description = description;
    }

    /**
     * @return the reason's byte on the wire
     */
    public int getCode() {
        return this.code;
    }

    /**
     * @return the reason in words, for messages
     */
    public @NotNull String getDescription() {
        return this.description;
    }
}

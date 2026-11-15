package org.fogofwarlabs.sc4j.engine.client.skin;

import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Chooses the {@link HudSkin} of a player by the player's faction: the faction's skin when the table has one, else the default skin, which also
 * serves a player without faction and an observer.
 */
public final class HudSkinTable {
    private final @NotNull HudSkin defaultSkin;
    private final @NotNull Map<@NotNull Faction, @NotNull HudSkin> factionSkins;

    /**
     * Creates a {@link HudSkinTable} instance without faction skins.
     *
     * @param defaultSkin the skin of a faction without its own skin, of a player without faction and of an observer
     */
    public HudSkinTable(@NotNull HudSkin defaultSkin) {
        this.defaultSkin  = defaultSkin;
        this.factionSkins = new HashMap<>();
    }

    /**
     * @return the skin of a faction without its own skin, of a player without faction and of an observer
     */
    public @NotNull HudSkin getDefaultSkin() {
        return this.defaultSkin;
    }

    /**
     * Sets the skin of a faction.
     *
     * @param faction the faction
     * @param skin    the skin of the faction's players
     */
    public void setFactionSkin(@NotNull Faction faction, @NotNull HudSkin skin) {
        this.factionSkins.put(faction, skin);
    }


    /**
     * Finds the skin of a faction.
     *
     * @param faction the faction, or {@code null} for a player without faction or an observer
     * @return the faction's skin, or the default skin if the table has none for the faction
     */
    public @NotNull HudSkin findSkin(@Nullable Faction faction) {
        return faction != null ? this.factionSkins.getOrDefault(faction, this.defaultSkin) : this.defaultSkin;
    }
}

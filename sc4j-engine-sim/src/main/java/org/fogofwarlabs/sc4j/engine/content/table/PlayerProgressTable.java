package org.fogofwarlabs.sc4j.engine.content.table;

import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentRegistry;
import org.fogofwarlabs.sc4j.engine.content.registry.TableKind;
import org.fogofwarlabs.sc4j.engine.content.registry.TypeColumn;
import org.fogofwarlabs.sc4j.engine.content.type.TechType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.UpgradeType;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnKey;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnTable;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Holds what each player has of the content: upgrade levels and their maximums, researched and available techs,
 * available unit types, unit counts (every unit, complete or not) and completed unit counts, plus the per-player
 * extension columns a mod registers ({@link ContentRegistry#addPlayerColumn}). Requirements and modifiers read it; the mod's systems,
 * map settings, triggers and saved games write it. Every column holds one value per player and type, player-major
 * ({@code player * typeCount + typeIndex}).
 * <p>
 * A new table has every level and count at {@code 0}, no tech researched, every tech and unit type available and each
 * maximum upgrade level at the {@code maximumLevel} of the upgrade definition. The write rules of {@link TypeTable}
 * apply. The state hash mixes one digest of the table, the hash of the columns in the order of the accessors, each in
 * index order, then of the extension columns in {@link TableKind} order, index by index and within an index in
 * registration order. Like a type table, the table recomputes the digest only after a write (a setter or an extension
 * column write), and a table without values mixes nothing.
 */
public final class PlayerProgressTable implements StateHashContributor, SnapshotContributor {
    private static int getTypeCount(@NotNull ContentDefinition content, @NotNull TableKind kind) {
        return switch (kind) {
            case UNIT -> content.getUnitTypes().getCount();
            case WEAPON -> content.getWeaponTypes().getCount();
            case MOTION_PROFILE -> content.getMotionProfiles().getCount();
            case SPRITE -> content.getSpriteTypes().getCount();
            case IMAGE -> content.getImageTypes().getCount();
            case UPGRADE -> content.getUpgradeTypes().getCount();
            case TECH -> content.getTechTypes().getCount();
            case ORDER -> content.getOrderTypes().getCount();
            case SOUND -> content.getSoundTypes().getCount();
        };
    }

    private final int playerCount;
    private final int unitTypeCount;
    private final int upgradeCount;
    private final int techCount;

    private final int @NotNull [] upgradeLevel;
    private final int @NotNull [] maximumUpgradeLevel;
    private final boolean @NotNull [] techResearched;
    private final boolean @NotNull [] techAvailable;
    private final boolean @NotNull [] unitAvailable;
    private final int @NotNull [] unitCount;
    private final int @NotNull [] completedUnitCount;
    private final @NotNull ColumnTable @NotNull [] extensionColumns;

    private final @NotNull TableDigest digest;

    /**
     * Creates a {@link PlayerProgressTable} instance with the initial values of the class description.
     *
     * @param content     the content definition
     * @param playerCount the number of players
     */
    PlayerProgressTable(@NotNull ContentDefinition content, int playerCount) {
        this.playerCount   = playerCount;
        this.unitTypeCount = content.getUnitTypes().getCount();
        this.upgradeCount  = content.getUpgradeTypes().getCount();
        this.techCount     = content.getTechTypes().getCount();

        this.upgradeLevel        = new int[playerCount * this.upgradeCount];
        this.maximumUpgradeLevel = new int[playerCount * this.upgradeCount];
        this.techResearched      = new boolean[playerCount * this.techCount];
        this.techAvailable       = new boolean[playerCount * this.techCount];
        this.unitAvailable       = new boolean[playerCount * this.unitTypeCount];
        this.unitCount           = new int[playerCount * this.unitTypeCount];
        this.completedUnitCount  = new int[playerCount * this.unitTypeCount];
        this.extensionColumns    = new ColumnTable[TableKind.values().length];

        this.digest = new TableDigest();

        for (TableKind kind : TableKind.values()) {
            List<TypeColumn<?>> definitions = content.getPlayerColumns(kind);
            List<ColumnKey<?>> keys = new ArrayList<>(definitions.size());

            for (TypeColumn<?> column : definitions) {
                keys.add(column.getKey());
            }

            this.extensionColumns[kind.ordinal()] = new ColumnTable(keys, playerCount * getTypeCount(content, kind));

            for (TypeColumn<?> column : definitions) {
                column.initialize(this.extensionColumns[kind.ordinal()]);
            }
        }

        Arrays.fill(this.techAvailable, true);
        Arrays.fill(this.unitAvailable, true);

        for (int player = 0; player < playerCount; player++) {
            for (int upgrade = 0; upgrade < this.upgradeCount; upgrade++) {
                this.maximumUpgradeLevel[player * this.upgradeCount + upgrade] = content.getUpgradeTypes().getDefinition(upgrade).getMaximumLevel();
            }
        }
    }

    /**
     * Returns the per-player extension columns the mod registered for the types of a table, indexed player-major
     * ({@code player * typeCount + typeIndex}). A write to them counts for the state hash like a write through a setter.
     *
     * @param kind the type table whose types index the columns
     * @return the extension columns in registration order
     */
    public @NotNull ColumnTable getExtensionColumns(@NotNull TableKind kind) {
        return this.extensionColumns[kind.ordinal()];
    }

    /**
     * @return the number of players
     */
    public int getPlayerCount() {
        return this.playerCount;
    }

    /**
     * @return the number of unit types of the content, the type count of the per-player unit tables
     */
    public int getUnitTypeCount() {
        return this.unitTypeCount;
    }

    /**
     * @param player the player
     * @param type   the upgrade
     * @return the level of an upgrade the player has
     */
    public int getUpgradeLevel(@NotNull Player player, @NotNull UpgradeType type) {
        return this.getUpgradeLevel(player.getIndex(), type.getIndex());
    }

    /**
     * @param player    the player slot index
     * @param typeIndex the index of the upgrade
     * @return the level of an upgrade the player has
     */
    public int getUpgradeLevel(int player, int typeIndex) {
        return this.upgradeLevel[player * this.upgradeCount + typeIndex];
    }

    /**
     * Sets the level of an upgrade the player has.
     *
     * @param player the player
     * @param type   the upgrade
     * @param value  the value
     */
    public void setUpgradeLevel(@NotNull Player player, @NotNull UpgradeType type, int value) {
        this.setUpgradeLevel(player.getIndex(), type.getIndex(), value);
    }

    /**
     * Sets the level of an upgrade the player has.
     *
     * @param player    the player slot index
     * @param typeIndex the index of the upgrade
     * @param value     the value
     */
    public void setUpgradeLevel(int player, int typeIndex, int value) {
        this.upgradeLevel[player * this.upgradeCount + typeIndex] = value;

        this.digest.recordWrite();
    }

    /**
     * @param player the player
     * @param type   the upgrade
     * @return the highest level of an upgrade the player may research
     */
    public int getMaximumUpgradeLevel(@NotNull Player player, @NotNull UpgradeType type) {
        return this.getMaximumUpgradeLevel(player.getIndex(), type.getIndex());
    }

    /**
     * @param player    the player slot index
     * @param typeIndex the index of the upgrade
     * @return the highest level of an upgrade the player may research
     */
    public int getMaximumUpgradeLevel(int player, int typeIndex) {
        return this.maximumUpgradeLevel[player * this.upgradeCount + typeIndex];
    }

    /**
     * Sets the highest level of an upgrade the player may research.
     *
     * @param player the player
     * @param type   the upgrade
     * @param value  the value
     */
    public void setMaximumUpgradeLevel(@NotNull Player player, @NotNull UpgradeType type, int value) {
        this.setMaximumUpgradeLevel(player.getIndex(), type.getIndex(), value);
    }

    /**
     * Sets the highest level of an upgrade the player may research.
     *
     * @param player    the player slot index
     * @param typeIndex the index of the upgrade
     * @param value     the value
     */
    public void setMaximumUpgradeLevel(int player, int typeIndex, int value) {
        this.maximumUpgradeLevel[player * this.upgradeCount + typeIndex] = value;

        this.digest.recordWrite();
    }

    /**
     * @param player the player
     * @param type   the tech
     * @return {@code true} if the player has researched a tech, {@code false} otherwise
     */
    public boolean isTechResearched(@NotNull Player player, @NotNull TechType type) {
        return this.isTechResearched(player.getIndex(), type.getIndex());
    }

    /**
     * @param player    the player slot index
     * @param typeIndex the index of the tech
     * @return {@code true} if the player has researched a tech, {@code false} otherwise
     */
    public boolean isTechResearched(int player, int typeIndex) {
        return this.techResearched[player * this.techCount + typeIndex];
    }

    /**
     * Sets whether the player has researched a tech.
     *
     * @param player the player
     * @param type   the tech
     * @param value  the value
     */
    public void setTechResearched(@NotNull Player player, @NotNull TechType type, boolean value) {
        this.setTechResearched(player.getIndex(), type.getIndex(), value);
    }

    /**
     * Sets whether the player has researched a tech.
     *
     * @param player    the player slot index
     * @param typeIndex the index of the tech
     * @param value     the value
     */
    public void setTechResearched(int player, int typeIndex, boolean value) {
        this.techResearched[player * this.techCount + typeIndex] = value;

        this.digest.recordWrite();
    }

    /**
     * @param player the player
     * @param type   the tech
     * @return {@code true} if the player may research a tech, {@code false} otherwise
     */
    public boolean isTechAvailable(@NotNull Player player, @NotNull TechType type) {
        return this.isTechAvailable(player.getIndex(), type.getIndex());
    }

    /**
     * @param player    the player slot index
     * @param typeIndex the index of the tech
     * @return {@code true} if the player may research a tech, {@code false} otherwise
     */
    public boolean isTechAvailable(int player, int typeIndex) {
        return this.techAvailable[player * this.techCount + typeIndex];
    }

    /**
     * Sets whether the player may research a tech.
     *
     * @param player the player
     * @param type   the tech
     * @param value  the value
     */
    public void setTechAvailable(@NotNull Player player, @NotNull TechType type, boolean value) {
        this.setTechAvailable(player.getIndex(), type.getIndex(), value);
    }

    /**
     * Sets whether the player may research a tech.
     *
     * @param player    the player slot index
     * @param typeIndex the index of the tech
     * @param value     the value
     */
    public void setTechAvailable(int player, int typeIndex, boolean value) {
        this.techAvailable[player * this.techCount + typeIndex] = value;

        this.digest.recordWrite();
    }

    /**
     * @param player the player
     * @param type   the unit type
     * @return {@code true} if the player may build a unit type, {@code false} otherwise
     */
    public boolean isUnitAvailable(@NotNull Player player, @NotNull UnitType type) {
        return this.isUnitAvailable(player.getIndex(), type.getIndex());
    }

    /**
     * @param player    the player slot index
     * @param typeIndex the index of the unit type
     * @return {@code true} if the player may build a unit type, {@code false} otherwise
     */
    public boolean isUnitAvailable(int player, int typeIndex) {
        return this.unitAvailable[player * this.unitTypeCount + typeIndex];
    }

    /**
     * Sets whether the player may build a unit type.
     *
     * @param player the player
     * @param type   the unit type
     * @param value  the value
     */
    public void setUnitAvailable(@NotNull Player player, @NotNull UnitType type, boolean value) {
        this.setUnitAvailable(player.getIndex(), type.getIndex(), value);
    }

    /**
     * Sets whether the player may build a unit type.
     *
     * @param player    the player slot index
     * @param typeIndex the index of the unit type
     * @param value     the value
     */
    public void setUnitAvailable(int player, int typeIndex, boolean value) {
        this.unitAvailable[player * this.unitTypeCount + typeIndex] = value;

        this.digest.recordWrite();
    }

    /**
     * @param player the player
     * @param type   the unit type
     * @return the number of units of a unit type the player owns, complete or not
     */
    public int getUnitCount(@NotNull Player player, @NotNull UnitType type) {
        return this.getUnitCount(player.getIndex(), type.getIndex());
    }

    /**
     * @param player    the player slot index
     * @param typeIndex the index of the unit type
     * @return the number of units of a unit type the player owns, complete or not
     */
    public int getUnitCount(int player, int typeIndex) {
        return this.unitCount[player * this.unitTypeCount + typeIndex];
    }

    /**
     * Sets the number of units of a unit type the player owns, complete or not.
     *
     * @param player the player
     * @param type   the unit type
     * @param value  the value
     */
    public void setUnitCount(@NotNull Player player, @NotNull UnitType type, int value) {
        this.setUnitCount(player.getIndex(), type.getIndex(), value);
    }

    /**
     * Sets the number of units of a unit type the player owns, complete or not.
     *
     * @param player    the player slot index
     * @param typeIndex the index of the unit type
     * @param value     the value
     */
    public void setUnitCount(int player, int typeIndex, int value) {
        this.unitCount[player * this.unitTypeCount + typeIndex] = value;

        this.digest.recordWrite();
    }

    /**
     * @param player the player
     * @param type   the unit type
     * @return the number of completed units of a unit type the player owns
     */
    public int getCompletedUnitCount(@NotNull Player player, @NotNull UnitType type) {
        return this.getCompletedUnitCount(player.getIndex(), type.getIndex());
    }

    /**
     * @param player    the player slot index
     * @param typeIndex the index of the unit type
     * @return the number of completed units of a unit type the player owns
     */
    public int getCompletedUnitCount(int player, int typeIndex) {
        return this.completedUnitCount[player * this.unitTypeCount + typeIndex];
    }

    /**
     * Sets the number of completed units of a unit type the player owns.
     *
     * @param player the player
     * @param type   the unit type
     * @param value  the value
     */
    public void setCompletedUnitCount(@NotNull Player player, @NotNull UnitType type, int value) {
        this.setCompletedUnitCount(player.getIndex(), type.getIndex(), value);
    }

    /**
     * Sets the number of completed units of a unit type the player owns.
     *
     * @param player    the player slot index
     * @param typeIndex the index of the unit type
     * @param value     the value
     */
    public void setCompletedUnitCount(int player, int typeIndex, int value) {
        this.completedUnitCount[player * this.unitTypeCount + typeIndex] = value;

        this.digest.recordWrite();
    }


    /**
     * Mixes this table into the hash: its digest, recomputed column by column in the order of the accessors, each in
     * index order, if a setter wrote since the last digest. Allocates nothing.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        long writeCount = this.digest.getWriteCount();
        boolean empty = this.playerCount == 0 || this.unitTypeCount + this.upgradeCount + this.techCount == 0;

        for (ColumnTable columns : this.extensionColumns) {
            writeCount += columns.getWriteCount();
            empty &= columns.getCount() == 0;
        }

        if (empty) {
            return;
        }

        if (this.digest.needsUpdate(writeCount)) {
            StateHash values = this.digest.start();

            TypeTable.mixColumn(values, this.upgradeLevel);
            TypeTable.mixColumn(values, this.maximumUpgradeLevel);
            TypeTable.mixColumn(values, this.techResearched);
            TypeTable.mixColumn(values, this.techAvailable);
            TypeTable.mixColumn(values, this.unitAvailable);
            TypeTable.mixColumn(values, this.unitCount);
            TypeTable.mixColumn(values, this.completedUnitCount);

            for (ColumnTable columns : this.extensionColumns) {
                for (int index = 0; index < columns.getLength(); index++) {
                    columns.contributeStateHash(values, index);
                }
            }

            this.digest.finish(writeCount);
        }

        hash.mixLong(this.digest.getValue());
    }

    /**
     * Writes this table: the columns in the order of the state hash, each whole in index order, then the extension columns of each table kind.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInts(this.upgradeLevel);
        writer.writeInts(this.maximumUpgradeLevel);
        writer.writeBooleans(this.techResearched);
        writer.writeBooleans(this.techAvailable);
        writer.writeBooleans(this.unitAvailable);
        writer.writeInts(this.unitCount);
        writer.writeInts(this.completedUnitCount);
        writer.writeInt(this.extensionColumns.length);

        for (ColumnTable columns : this.extensionColumns) {
            columns.writeSnapshot(writer);
        }
    }

    /**
     * Reads this table back, replacing the current values; the next state hash recomputes the digest.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or a column holds another number of values
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readInts(this.upgradeLevel);
        reader.readInts(this.maximumUpgradeLevel);
        reader.readBooleans(this.techResearched);
        reader.readBooleans(this.techAvailable);
        reader.readBooleans(this.unitAvailable);
        reader.readInts(this.unitCount);
        reader.readInts(this.completedUnitCount);
        reader.readLength(this.extensionColumns.length);

        for (ColumnTable columns : this.extensionColumns) {
            columns.readSnapshot(reader);
        }

        this.digest.recordWrite();
    }
}

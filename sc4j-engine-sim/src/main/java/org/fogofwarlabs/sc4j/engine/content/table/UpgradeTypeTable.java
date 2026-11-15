package org.fogofwarlabs.sc4j.engine.content.table;

import org.fogofwarlabs.sc4j.engine.content.definition.UpgradeDefinition;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.TableKind;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.DefinitionRegistry;
import org.fogofwarlabs.sc4j.engine.content.type.UpgradeType;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Holds the per-game values of every {@link UpgradeType} as one array per column, indexed by the type index.
 * <p>
 * The engine columns, in state hash order, are {@code faction}, {@code baseCost}, {@code costFactor}, {@code baseTime},
 * {@code timeFactor} and {@code maximumLevel}. A cost column holds one value per type and resource kind, type-major. A
 * type reference holds the index of the referenced type, {@link ContentType#NO_INDEX} for none. The extension columns
 * the mod registered for {@link TableKind#UPGRADE} follow ({@link #getExtensionColumns()}).
 * <p>
 * The values start as the {@link UpgradeDefinition} of each type and may be written during the game; see {@link TypeTable}
 * for the write rules. Every accessor is a plain array access: an index outside the table throws
 * {@link ArrayIndexOutOfBoundsException}.
 */
public final class UpgradeTypeTable extends TypeTable {
    private final int resourceKindCount;

    private final int @NotNull [] faction;
    private final int @NotNull [] baseCost;
    private final int @NotNull [] costFactor;
    private final int @NotNull [] baseTime;
    private final int @NotNull [] timeFactor;
    private final int @NotNull [] maximumLevel;

    /**
     * Creates an {@link UpgradeTypeTable} instance from the definitions of a content.
     *
     * @param content the content definition
     * @throws IllegalArgumentException if a definition refers to a type that is not registered in the content
     */
    UpgradeTypeTable(@NotNull ContentDefinition content) {
        super(content, content.getUpgradeTypes().getCount(), content.getColumns(TableKind.UPGRADE));

        DefinitionRegistry<UpgradeType, UpgradeDefinition> definitions = content.getUpgradeTypes();
        int count = this.getCount();

        this.resourceKindCount = content.getResourceKinds().getCount();

        this.faction      = new int[count];
        this.baseCost     = new int[count * this.resourceKindCount];
        this.costFactor   = new int[count * this.resourceKindCount];
        this.baseTime     = new int[count];
        this.timeFactor   = new int[count];
        this.maximumLevel = new int[count];

        for (int index = 0; index < count; index++) {
            UpgradeDefinition definition = definitions.getDefinition(index);

            this.faction[index] = content.getFactions().getTypes().requireIndex(definition.getFaction());
            fillCost(content.getResourceKinds(), definition.getBaseCost(), this.baseCost, index * this.resourceKindCount);
            fillCost(content.getResourceKinds(), definition.getCostFactor(), this.costFactor, index * this.resourceKindCount);
            this.baseTime[index]     = definition.getBaseTime();
            this.timeFactor[index]   = definition.getTimeFactor();
            this.maximumLevel[index] = definition.getMaximumLevel();
        }

        this.initializeExtensionColumns();
    }

    /**
     * @param type the upgrade type
     * @return the faction that researches the upgrade, or {@code null} for none
     * @throws IndexOutOfBoundsException if the stored index names no registered type
     */
    public @Nullable Faction getFaction(@NotNull UpgradeType type) {
        return resolve(this.getContent().getFactions().getTypes(), this.faction[type.getIndex()]);
    }

    /**
     * @param typeIndex the index of the upgrade type
     * @return the index of the faction that researches the upgrade, or {@link ContentType#NO_INDEX} for none
     */
    public int getFactionIndex(int typeIndex) {
        return this.faction[typeIndex];
    }

    /**
     * Sets the faction that researches the upgrade.
     *
     * @param type  the upgrade type
     * @param value the faction registered in the same content, or {@code null} for none
     */
    public void setFaction(@NotNull UpgradeType type, @Nullable Faction value) {
        this.faction[type.getIndex()] = ContentType.indexOf(value);

        this.recordWrite();
    }

    /**
     * Sets the faction that researches the upgrade.
     *
     * @param typeIndex the index of the upgrade type
     * @param value     the index of the referenced type, or {@link ContentType#NO_INDEX} for none
     */
    public void setFactionIndex(int typeIndex, int value) {
        this.faction[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * Returns the amount of one resource kind of the cost of the first level.
     *
     * @param type the upgrade type
     * @param kind the resource kind
     * @return the amount
     */
    public int getBaseCost(@NotNull UpgradeType type, @NotNull ResourceKind kind) {
        return this.baseCost[type.getIndex() * this.resourceKindCount + kind.getIndex()];
    }

    /**
     * Returns the amount of one resource kind of the cost of the first level.
     *
     * @param typeIndex         the index of the upgrade type
     * @param resourceKindIndex the index of the resource kind
     * @return the amount
     */
    public int getBaseCost(int typeIndex, int resourceKindIndex) {
        return this.baseCost[typeIndex * this.resourceKindCount + resourceKindIndex];
    }

    /**
     * Sets the amount of one resource kind of the cost of the first level.
     *
     * @param type  the upgrade type
     * @param kind  the resource kind
     * @param value the amount
     */
    public void setBaseCost(@NotNull UpgradeType type, @NotNull ResourceKind kind, int value) {
        this.baseCost[type.getIndex() * this.resourceKindCount + kind.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the amount of one resource kind of the cost of the first level.
     *
     * @param typeIndex         the index of the upgrade type
     * @param resourceKindIndex the index of the resource kind
     * @param value             the amount
     */
    public void setBaseCost(int typeIndex, int resourceKindIndex, int value) {
        this.baseCost[typeIndex * this.resourceKindCount + resourceKindIndex] = value;

        this.recordWrite();
    }

    /**
     * Returns the amount of one resource kind of the cost added per further level.
     *
     * @param type the upgrade type
     * @param kind the resource kind
     * @return the amount
     */
    public int getCostFactor(@NotNull UpgradeType type, @NotNull ResourceKind kind) {
        return this.costFactor[type.getIndex() * this.resourceKindCount + kind.getIndex()];
    }

    /**
     * Returns the amount of one resource kind of the cost added per further level.
     *
     * @param typeIndex         the index of the upgrade type
     * @param resourceKindIndex the index of the resource kind
     * @return the amount
     */
    public int getCostFactor(int typeIndex, int resourceKindIndex) {
        return this.costFactor[typeIndex * this.resourceKindCount + resourceKindIndex];
    }

    /**
     * Sets the amount of one resource kind of the cost added per further level.
     *
     * @param type  the upgrade type
     * @param kind  the resource kind
     * @param value the amount
     */
    public void setCostFactor(@NotNull UpgradeType type, @NotNull ResourceKind kind, int value) {
        this.costFactor[type.getIndex() * this.resourceKindCount + kind.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the amount of one resource kind of the cost added per further level.
     *
     * @param typeIndex         the index of the upgrade type
     * @param resourceKindIndex the index of the resource kind
     * @param value             the amount
     */
    public void setCostFactor(int typeIndex, int resourceKindIndex, int value) {
        this.costFactor[typeIndex * this.resourceKindCount + resourceKindIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the upgrade type
     * @return the research time of the first level in frames
     */
    public int getBaseTime(@NotNull UpgradeType type) {
        return this.baseTime[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the upgrade type
     * @return the research time of the first level in frames
     */
    public int getBaseTime(int typeIndex) {
        return this.baseTime[typeIndex];
    }

    /**
     * Sets the research time of the first level in frames.
     *
     * @param type  the upgrade type
     * @param value the value
     */
    public void setBaseTime(@NotNull UpgradeType type, int value) {
        this.baseTime[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the research time of the first level in frames.
     *
     * @param typeIndex the index of the upgrade type
     * @param value     the value
     */
    public void setBaseTime(int typeIndex, int value) {
        this.baseTime[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the upgrade type
     * @return the research time added per further level in frames
     */
    public int getTimeFactor(@NotNull UpgradeType type) {
        return this.timeFactor[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the upgrade type
     * @return the research time added per further level in frames
     */
    public int getTimeFactor(int typeIndex) {
        return this.timeFactor[typeIndex];
    }

    /**
     * Sets the research time added per further level in frames.
     *
     * @param type  the upgrade type
     * @param value the value
     */
    public void setTimeFactor(@NotNull UpgradeType type, int value) {
        this.timeFactor[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the research time added per further level in frames.
     *
     * @param typeIndex the index of the upgrade type
     * @param value     the value
     */
    public void setTimeFactor(int typeIndex, int value) {
        this.timeFactor[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the upgrade type
     * @return the default maximum level
     */
    public int getMaximumLevel(@NotNull UpgradeType type) {
        return this.maximumLevel[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the upgrade type
     * @return the default maximum level
     */
    public int getMaximumLevel(int typeIndex) {
        return this.maximumLevel[typeIndex];
    }

    /**
     * Sets the default maximum level.
     *
     * @param type  the upgrade type
     * @param value the value
     */
    public void setMaximumLevel(@NotNull UpgradeType type, int value) {
        this.maximumLevel[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the default maximum level.
     *
     * @param typeIndex the index of the upgrade type
     * @param value     the value
     */
    public void setMaximumLevel(int typeIndex, int value) {
        this.maximumLevel[typeIndex] = value;

        this.recordWrite();
    }


    /**
     * Mixes the engine columns into the hash, column by column in the order of the class description, each in type
     * index order.
     *
     * @param hash the hash being computed
     */
    @Override
    void contributeEngineColumns(@NotNull StateHash hash) {
        mixColumn(hash, this.faction);
        mixColumn(hash, this.baseCost);
        mixColumn(hash, this.costFactor);
        mixColumn(hash, this.baseTime);
        mixColumn(hash, this.timeFactor);
        mixColumn(hash, this.maximumLevel);
    }

    /**
     * Writes the engine columns, column by column in the order of their hash, each whole in type index order.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    void writeEngineColumns(@NotNull StateSnapshotWriter writer) {
        writer.writeInts(this.faction);
        writer.writeInts(this.baseCost);
        writer.writeInts(this.costFactor);
        writer.writeInts(this.baseTime);
        writer.writeInts(this.timeFactor);
        writer.writeInts(this.maximumLevel);
    }

    /**
     * Reads the engine columns back, column by column in the order of their hash, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or a column holds another number of values
     */
    @Override
    void readEngineColumns(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readInts(this.faction);
        reader.readInts(this.baseCost);
        reader.readInts(this.costFactor);
        reader.readInts(this.baseTime);
        reader.readInts(this.timeFactor);
        reader.readInts(this.maximumLevel);
    }
}

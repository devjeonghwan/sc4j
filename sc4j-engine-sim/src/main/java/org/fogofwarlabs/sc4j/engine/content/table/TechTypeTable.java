package org.fogofwarlabs.sc4j.engine.content.table;

import org.fogofwarlabs.sc4j.engine.content.definition.TechDefinition;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.TableKind;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.DefinitionRegistry;
import org.fogofwarlabs.sc4j.engine.content.type.TechType;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Holds the per-game values of every {@link TechType} as one array per column, indexed by the type index.
 * <p>
 * The engine columns, in state hash order, are {@code faction}, {@code cost}, {@code researchTime} and
 * {@code energyCost}. A cost column holds one value per type and resource kind, type-major. A type reference holds the
 * index of the referenced type, {@link ContentType#NO_INDEX} for none. The extension columns the mod registered for
 * {@link TableKind#TECH} follow ({@link #getExtensionColumns()}).
 * <p>
 * The values start as the {@link TechDefinition} of each type and may be written during the game; see {@link TypeTable}
 * for the write rules. Every accessor is a plain array access: an index outside the table throws
 * {@link ArrayIndexOutOfBoundsException}.
 */
public final class TechTypeTable extends TypeTable {
    private final int resourceKindCount;

    private final int @NotNull [] faction;
    private final int @NotNull [] cost;
    private final int @NotNull [] researchTime;
    private final int @NotNull [] energyCost;

    /**
     * Creates a {@link TechTypeTable} instance from the definitions of a content.
     *
     * @param content the content definition
     * @throws IllegalArgumentException if a definition refers to a type that is not registered in the content
     */
    TechTypeTable(@NotNull ContentDefinition content) {
        super(content, content.getTechTypes().getCount(), content.getColumns(TableKind.TECH));

        DefinitionRegistry<TechType, TechDefinition> definitions = content.getTechTypes();
        int count = this.getCount();

        this.resourceKindCount = content.getResourceKinds().getCount();

        this.faction      = new int[count];
        this.cost         = new int[count * this.resourceKindCount];
        this.researchTime = new int[count];
        this.energyCost   = new int[count];

        for (int index = 0; index < count; index++) {
            TechDefinition definition = definitions.getDefinition(index);

            this.faction[index] = content.getFactions().getTypes().requireIndex(definition.getFaction());
            fillCost(content.getResourceKinds(), definition.getCost(), this.cost, index * this.resourceKindCount);
            this.researchTime[index] = definition.getResearchTime();
            this.energyCost[index]   = definition.getEnergyCost();
        }

        this.initializeExtensionColumns();
    }

    /**
     * @param type the tech type
     * @return the faction that researches the tech, or {@code null} for none
     * @throws IndexOutOfBoundsException if the stored index names no registered type
     */
    public @Nullable Faction getFaction(@NotNull TechType type) {
        return resolve(this.getContent().getFactions().getTypes(), this.faction[type.getIndex()]);
    }

    /**
     * @param typeIndex the index of the tech type
     * @return the index of the faction that researches the tech, or {@link ContentType#NO_INDEX} for none
     */
    public int getFactionIndex(int typeIndex) {
        return this.faction[typeIndex];
    }

    /**
     * Sets the faction that researches the tech.
     *
     * @param type  the tech type
     * @param value the faction registered in the same content, or {@code null} for none
     */
    public void setFaction(@NotNull TechType type, @Nullable Faction value) {
        this.faction[type.getIndex()] = ContentType.indexOf(value);

        this.recordWrite();
    }

    /**
     * Sets the faction that researches the tech.
     *
     * @param typeIndex the index of the tech type
     * @param value     the index of the referenced type, or {@link ContentType#NO_INDEX} for none
     */
    public void setFactionIndex(int typeIndex, int value) {
        this.faction[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * Returns the amount of one resource kind of the research cost.
     *
     * @param type the tech type
     * @param kind the resource kind
     * @return the amount
     */
    public int getCost(@NotNull TechType type, @NotNull ResourceKind kind) {
        return this.cost[type.getIndex() * this.resourceKindCount + kind.getIndex()];
    }

    /**
     * Returns the amount of one resource kind of the research cost.
     *
     * @param typeIndex         the index of the tech type
     * @param resourceKindIndex the index of the resource kind
     * @return the amount
     */
    public int getCost(int typeIndex, int resourceKindIndex) {
        return this.cost[typeIndex * this.resourceKindCount + resourceKindIndex];
    }

    /**
     * Sets the amount of one resource kind of the research cost.
     *
     * @param type  the tech type
     * @param kind  the resource kind
     * @param value the amount
     */
    public void setCost(@NotNull TechType type, @NotNull ResourceKind kind, int value) {
        this.cost[type.getIndex() * this.resourceKindCount + kind.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the amount of one resource kind of the research cost.
     *
     * @param typeIndex         the index of the tech type
     * @param resourceKindIndex the index of the resource kind
     * @param value             the amount
     */
    public void setCost(int typeIndex, int resourceKindIndex, int value) {
        this.cost[typeIndex * this.resourceKindCount + resourceKindIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the tech type
     * @return the research time in frames
     */
    public int getResearchTime(@NotNull TechType type) {
        return this.researchTime[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the tech type
     * @return the research time in frames
     */
    public int getResearchTime(int typeIndex) {
        return this.researchTime[typeIndex];
    }

    /**
     * Sets the research time in frames.
     *
     * @param type  the tech type
     * @param value the value
     */
    public void setResearchTime(@NotNull TechType type, int value) {
        this.researchTime[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the research time in frames.
     *
     * @param typeIndex the index of the tech type
     * @param value     the value
     */
    public void setResearchTime(int typeIndex, int value) {
        this.researchTime[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the tech type
     * @return the energy one use costs, in the energy unit of the mod
     */
    public int getEnergyCost(@NotNull TechType type) {
        return this.energyCost[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the tech type
     * @return the energy one use costs, in the energy unit of the mod
     */
    public int getEnergyCost(int typeIndex) {
        return this.energyCost[typeIndex];
    }

    /**
     * Sets the energy one use costs, in the energy unit of the mod.
     *
     * @param type  the tech type
     * @param value the value
     */
    public void setEnergyCost(@NotNull TechType type, int value) {
        this.energyCost[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the energy one use costs, in the energy unit of the mod.
     *
     * @param typeIndex the index of the tech type
     * @param value     the value
     */
    public void setEnergyCost(int typeIndex, int value) {
        this.energyCost[typeIndex] = value;

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
        mixColumn(hash, this.cost);
        mixColumn(hash, this.researchTime);
        mixColumn(hash, this.energyCost);
    }

    /**
     * Writes the engine columns, column by column in the order of their hash, each whole in type index order.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    void writeEngineColumns(@NotNull StateSnapshotWriter writer) {
        writer.writeInts(this.faction);
        writer.writeInts(this.cost);
        writer.writeInts(this.researchTime);
        writer.writeInts(this.energyCost);
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
        reader.readInts(this.cost);
        reader.readInts(this.researchTime);
        reader.readInts(this.energyCost);
    }
}

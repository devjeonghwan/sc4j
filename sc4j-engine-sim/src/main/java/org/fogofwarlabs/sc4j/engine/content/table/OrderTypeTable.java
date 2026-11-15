package org.fogofwarlabs.sc4j.engine.content.table;

import org.fogofwarlabs.sc4j.engine.content.definition.OrderDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.TableKind;
import org.fogofwarlabs.sc4j.engine.content.type.*;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Holds the per-game values of every {@link OrderType} as one array per column, indexed by the type index.
 * <p>
 * The engine columns, in state hash order, are {@code weapon}, {@code tech}, {@code interruptible} and
 * {@code queueable}. A type reference holds the index of the referenced type, {@link ContentType#NO_INDEX} for none.
 * The extension columns the mod registered for {@link TableKind#ORDER} follow ({@link #getExtensionColumns()}).
 * <p>
 * The values start as the {@link OrderDefinition} of each type and may be written during the game; see {@link TypeTable}
 * for the write rules. Every accessor is a plain array access: an index outside the table throws
 * {@link ArrayIndexOutOfBoundsException}.
 */
public final class OrderTypeTable extends TypeTable {
    private final int @NotNull [] weapon;
    private final int @NotNull [] tech;
    private final boolean @NotNull [] interruptible;
    private final boolean @NotNull [] queueable;

    /**
     * Creates an {@link OrderTypeTable} instance from the definitions of a content.
     *
     * @param content the content definition
     * @throws IllegalArgumentException if a definition refers to a type that is not registered in the content
     */
    OrderTypeTable(@NotNull ContentDefinition content) {
        super(content, content.getOrderTypes().getCount(), content.getColumns(TableKind.ORDER));

        DefinitionRegistry<OrderType, OrderDefinition> definitions = content.getOrderTypes();
        int count = this.getCount();

        this.weapon        = new int[count];
        this.tech          = new int[count];
        this.interruptible = new boolean[count];
        this.queueable     = new boolean[count];

        for (int index = 0; index < count; index++) {
            OrderDefinition definition = definitions.getDefinition(index);

            this.weapon[index]        = content.getWeaponTypes().getTypes().requireIndex(definition.getWeapon());
            this.tech[index]          = content.getTechTypes().getTypes().requireIndex(definition.getTech());
            this.interruptible[index] = definition.isInterruptible();
            this.queueable[index]     = definition.isQueueable();
        }

        this.initializeExtensionColumns();
    }

    /**
     * @param type the order type
     * @return the weapon the order uses, or {@code null} for none
     * @throws IndexOutOfBoundsException if the stored index names no registered type
     */
    public @Nullable WeaponType getWeapon(@NotNull OrderType type) {
        return resolve(this.getContent().getWeaponTypes().getTypes(), this.weapon[type.getIndex()]);
    }

    /**
     * @param typeIndex the index of the order type
     * @return the index of the weapon the order uses, or {@link ContentType#NO_INDEX} for none
     */
    public int getWeaponIndex(int typeIndex) {
        return this.weapon[typeIndex];
    }

    /**
     * Sets the weapon the order uses.
     *
     * @param type  the order type
     * @param value the weapon type registered in the same content, or {@code null} for none
     */
    public void setWeapon(@NotNull OrderType type, @Nullable WeaponType value) {
        this.weapon[type.getIndex()] = ContentType.indexOf(value);

        this.recordWrite();
    }

    /**
     * Sets the weapon the order uses.
     *
     * @param typeIndex the index of the order type
     * @param value     the index of the referenced type, or {@link ContentType#NO_INDEX} for none
     */
    public void setWeaponIndex(int typeIndex, int value) {
        this.weapon[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the order type
     * @return the tech the order uses, or {@code null} for none
     * @throws IndexOutOfBoundsException if the stored index names no registered type
     */
    public @Nullable TechType getTech(@NotNull OrderType type) {
        return resolve(this.getContent().getTechTypes().getTypes(), this.tech[type.getIndex()]);
    }

    /**
     * @param typeIndex the index of the order type
     * @return the index of the tech the order uses, or {@link ContentType#NO_INDEX} for none
     */
    public int getTechIndex(int typeIndex) {
        return this.tech[typeIndex];
    }

    /**
     * Sets the tech the order uses.
     *
     * @param type  the order type
     * @param value the tech type registered in the same content, or {@code null} for none
     */
    public void setTech(@NotNull OrderType type, @Nullable TechType value) {
        this.tech[type.getIndex()] = ContentType.indexOf(value);

        this.recordWrite();
    }

    /**
     * Sets the tech the order uses.
     *
     * @param typeIndex the index of the order type
     * @param value     the index of the referenced type, or {@link ContentType#NO_INDEX} for none
     */
    public void setTechIndex(int typeIndex, int value) {
        this.tech[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the order type
     * @return {@code true} if a new order replaces the order at once, {@code false} otherwise
     */
    public boolean isInterruptible(@NotNull OrderType type) {
        return this.interruptible[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the order type
     * @return {@code true} if a new order replaces the order at once, {@code false} otherwise
     */
    public boolean isInterruptible(int typeIndex) {
        return this.interruptible[typeIndex];
    }

    /**
     * Sets whether a new order replaces the order at once.
     *
     * @param type  the order type
     * @param value the value
     */
    public void setInterruptible(@NotNull OrderType type, boolean value) {
        this.interruptible[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets whether a new order replaces the order at once.
     *
     * @param typeIndex the index of the order type
     * @param value     the value
     */
    public void setInterruptible(int typeIndex, boolean value) {
        this.interruptible[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the order type
     * @return {@code true} if the order can be queued after others, {@code false} otherwise
     */
    public boolean isQueueable(@NotNull OrderType type) {
        return this.queueable[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the order type
     * @return {@code true} if the order can be queued after others, {@code false} otherwise
     */
    public boolean isQueueable(int typeIndex) {
        return this.queueable[typeIndex];
    }

    /**
     * Sets whether the order can be queued after others.
     *
     * @param type  the order type
     * @param value the value
     */
    public void setQueueable(@NotNull OrderType type, boolean value) {
        this.queueable[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets whether the order can be queued after others.
     *
     * @param typeIndex the index of the order type
     * @param value     the value
     */
    public void setQueueable(int typeIndex, boolean value) {
        this.queueable[typeIndex] = value;

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
        mixColumn(hash, this.weapon);
        mixColumn(hash, this.tech);
        mixColumn(hash, this.interruptible);
        mixColumn(hash, this.queueable);
    }

    /**
     * Writes the engine columns, column by column in the order of their hash, each whole in type index order.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    void writeEngineColumns(@NotNull StateSnapshotWriter writer) {
        writer.writeInts(this.weapon);
        writer.writeInts(this.tech);
        writer.writeBooleans(this.interruptible);
        writer.writeBooleans(this.queueable);
    }

    /**
     * Reads the engine columns back, column by column in the order of their hash, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or a column holds another number of values
     */
    @Override
    void readEngineColumns(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readInts(this.weapon);
        reader.readInts(this.tech);
        reader.readBooleans(this.interruptible);
        reader.readBooleans(this.queueable);
    }
}

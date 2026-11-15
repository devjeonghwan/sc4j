package org.fogofwarlabs.sc4j.engine.content.table;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.TypeColumn;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.TypeRegistry;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnKey;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnTable;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the per-game values of one content kind: one primitive array per engine column and the mod's extension
 * columns, all indexed by the type index. A game creates its tables from the content definition when it is created,
 * so every game starts from the definitions and changes only its own copy.
 * <p>
 * Write rules (determinism contract):
 * <ul>
 *     <li>Only the simulation writes: frame phases, systems, commands, triggers and the loading of map settings or a
 *     saved game before the first frame. A write takes effect for every later read; there is no notification and no
 *     undo.</li>
 *     <li>A write never changes the number of types or the columns; a value is any {@code int} (or {@code boolean}) the
 *     column holds, and the engine does not validate it. A mod that needs narrower values (its own storage widths) clamps or
 *     wraps them before it writes.</li>
 *     <li>The state hash mixes one {@code 64}-bit digest of the table: the hash of the engine columns, column by column,
 *     each in type index order, then of the extension columns, type by type in index order and within a type in
 *     registration order. The digest is recomputed only after a write: every engine setter and every write to an
 *     extension column counts one ({@link org.fogofwarlabs.sc4j.engine.entity.column.Column#getWriteCount()}), and an
 *     unchanged count reuses the last digest. A table without types mixes nothing.</li>
 * </ul>
 * <p>
 * The accessors of a column come in a form that names the type and a form that takes the type index. The index forms are
 * the documented {@code int} fast path of the tables: for hot loops, and for a mod's adapter that addresses the values by
 * index (for example a view of a table in the mod's own memory layout, which converts the mod's own numbers to
 * type indices with its own map); every other caller names the type.
 */
public abstract sealed class TypeTable implements StateHashContributor, SnapshotContributor permits UnitTypeTable, WeaponTypeTable, MotionProfileTable,
                                                                                                    SpriteTypeTable, ImageTypeTable, UpgradeTypeTable,
                                                                                                    TechTypeTable, OrderTypeTable, SoundTypeTable {
    /**
     * Copies a cost into the cost column of one type: the amount of each resource kind at its kind index.
     *
     * @param resourceKinds the resource kinds of the content
     * @param cost          the cost of the definition
     * @param values        the cost column
     * @param offset        the position of the first resource kind of the type in the column
     * @throws IllegalArgumentException if the cost names a resource kind that is not registered in the content
     */
    static void fillCost(@NotNull TypeRegistry<ResourceKind> resourceKinds, @NotNull ResourceCost cost, int @NotNull [] values, int offset) {
        for (int entry = 0; entry < cost.getCount(); entry++) {
            values[offset + resourceKinds.requireIndex(cost.getKind(entry))] = cost.getAmount(entry);
        }
    }

    /**
     * Mixes every value of an {@code int} column into the hash, in index order.
     *
     * @param hash   the hash being computed
     * @param values the column
     */
    static void mixColumn(@NotNull StateHash hash, int @NotNull [] values) {
        for (int value : values) {
            hash.mix(value);
        }
    }

    /**
     * Mixes every value of a {@code boolean} column into the hash, in index order.
     *
     * @param hash   the hash being computed
     * @param values the column
     */
    static void mixColumn(@NotNull StateHash hash, boolean @NotNull [] values) {
        for (boolean value : values) {
            hash.mixBoolean(value);
        }
    }

    /**
     * Returns the identity a type reference column names.
     *
     * @param <T>   the referenced kind
     * @param types the registry of the referenced kind
     * @param index the stored index, {@link ContentType#NO_INDEX} for none
     * @return the identity, or {@code null} for {@link ContentType#NO_INDEX}
     * @throws IndexOutOfBoundsException if the index names no identity of the registry
     */
    static <T extends ContentType> @Nullable T resolve(@NotNull TypeRegistry<T> types, int index) {
        return index == ContentType.NO_INDEX ? null : types.get(index);
    }

    private final @NotNull ContentDefinition content;
    private final int count;
    private final @NotNull List<@NotNull TypeColumn<?>> columnDefinitions;
    private final @NotNull ColumnTable extensionColumns;

    private final @NotNull TableDigest digest;

    /**
     * Creates a {@link TypeTable} instance with empty extension columns; the subclass fills its engine columns and then
     * calls {@link #initializeExtensionColumns()}.
     *
     * @param content           the content definition, whose registries resolve the type reference columns
     * @param count             the number of types
     * @param columnDefinitions the extension columns the mod registered for the table
     */
    TypeTable(@NotNull ContentDefinition content, int count, @NotNull List<@NotNull TypeColumn<?>> columnDefinitions) {
        List<ColumnKey<?>> keys = new ArrayList<>(columnDefinitions.size());

        for (TypeColumn<?> column : columnDefinitions) {
            keys.add(column.getKey());
        }

        this.content           = content;
        this.count             = count;
        this.columnDefinitions = columnDefinitions;
        this.extensionColumns  = new ColumnTable(keys, count);

        this.digest = new TableDigest();
    }

    /**
     * @return the content definition the table was created from
     */
    final @NotNull ContentDefinition getContent() {
        return this.content;
    }

    /**
     * @return the number of types
     */
    public final int getCount() {
        return this.count;
    }

    /**
     * Returns the extension columns the mod registered for this table, indexed by the type index. A system resolves its
     * columns once, when it is attached, with {@link ColumnTable#get}.
     *
     * @return the extension columns in registration order
     */
    public final @NotNull ColumnTable getExtensionColumns() {
        return this.extensionColumns;
    }


    /**
     * Mixes this table into the hash: its digest, recomputed from the engine columns and then the extension columns if
     * the table was written since the last digest, as the class description says. Allocates nothing.
     *
     * @param hash the hash being computed
     */
    @Override
    public final void contributeStateHash(@NotNull StateHash hash) {
        if (this.count == 0) {
            return;
        }

        long writeCount = this.digest.getWriteCount() + this.extensionColumns.getWriteCount();

        if (this.digest.needsUpdate(writeCount)) {
            StateHash values = this.digest.start();

            this.contributeEngineColumns(values);

            for (int index = 0; index < this.count; index++) {
                this.extensionColumns.contributeStateHash(values, index);
            }

            this.digest.finish(writeCount);
        }

        hash.mixLong(this.digest.getValue());
    }

    /**
     * Writes this table: the type count, the engine columns, then the extension columns.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public final void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInt(this.count);
        this.writeEngineColumns(writer);
        this.extensionColumns.writeSnapshot(writer);
    }

    /**
     * Reads this table back, replacing the current values; the next state hash recomputes the digest.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or holds another number of types or columns
     */
    @Override
    public final void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readLength(this.count);
        this.readEngineColumns(reader);
        this.extensionColumns.readSnapshot(reader);

        this.recordWrite();
    }

    /**
     * Sets the initial values of the extension columns, in registration order, with the initializers of the mod.
     */
    final void initializeExtensionColumns() {
        for (TypeColumn<?> column : this.columnDefinitions) {
            column.initialize(this.extensionColumns);
        }
    }

    /**
     * Counts one write to an engine column; every engine setter of a subclass calls it.
     */
    final void recordWrite() {
        this.digest.recordWrite();
    }

    /**
     * Mixes the engine columns into the hash, column by column, each in type index order.
     *
     * @param hash the hash being computed
     */
    abstract void contributeEngineColumns(@NotNull StateHash hash);

    /**
     * Writes the engine columns, column by column in the order of their hash, each whole in type index order.
     *
     * @param writer the writer of the snapshot
     */
    abstract void writeEngineColumns(@NotNull StateSnapshotWriter writer);

    /**
     * Reads the engine columns back, column by column in the order of their hash, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or a column holds another number of values
     */
    abstract void readEngineColumns(@NotNull StateSnapshotReader reader) throws IOException;
}

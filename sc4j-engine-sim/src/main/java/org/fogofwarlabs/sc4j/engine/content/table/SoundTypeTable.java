package org.fogofwarlabs.sc4j.engine.content.table;

import org.fogofwarlabs.sc4j.engine.content.definition.SoundDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.TableKind;
import org.fogofwarlabs.sc4j.engine.content.type.DefinitionRegistry;
import org.fogofwarlabs.sc4j.engine.content.type.SoundType;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Holds the per-game values of every {@link SoundType} as one array per column, indexed by the type index.
 * <p>
 * The engine columns, in state hash order, are {@code priority} and {@code volume}. The extension columns the mod
 * registered for {@link TableKind#SOUND} follow ({@link #getExtensionColumns()}).
 * <p>
 * The values start as the {@link SoundDefinition} of each type and may be written during the game; see {@link TypeTable}
 * for the write rules. Every accessor is a plain array access: an index outside the table throws
 * {@link ArrayIndexOutOfBoundsException}.
 */
public final class SoundTypeTable extends TypeTable {
    private final int @NotNull [] priority;
    private final int @NotNull [] volume;

    /**
     * Creates a {@link SoundTypeTable} instance from the definitions of a content.
     *
     * @param content the content definition
     * @throws IllegalArgumentException if a definition refers to a type that is not registered in the content
     */
    SoundTypeTable(@NotNull ContentDefinition content) {
        super(content, content.getSoundTypes().getCount(), content.getColumns(TableKind.SOUND));

        DefinitionRegistry<SoundType, SoundDefinition> definitions = content.getSoundTypes();
        int count = this.getCount();

        this.priority = new int[count];
        this.volume   = new int[count];

        for (int index = 0; index < count; index++) {
            SoundDefinition definition = definitions.getDefinition(index);

            this.priority[index] = definition.getPriority();
            this.volume[index]   = definition.getVolume();
        }

        this.initializeExtensionColumns();
    }

    /**
     * @param type the sound type
     * @return the priority; a sound of higher priority replaces one of lower priority
     */
    public int getPriority(@NotNull SoundType type) {
        return this.priority[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the sound type
     * @return the priority; a sound of higher priority replaces one of lower priority
     */
    public int getPriority(int typeIndex) {
        return this.priority[typeIndex];
    }

    /**
     * Sets the priority; a sound of higher priority replaces one of lower priority.
     *
     * @param type  the sound type
     * @param value the value
     */
    public void setPriority(@NotNull SoundType type, int value) {
        this.priority[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the priority; a sound of higher priority replaces one of lower priority.
     *
     * @param typeIndex the index of the sound type
     * @param value     the value
     */
    public void setPriority(int typeIndex, int value) {
        this.priority[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the sound type
     * @return the volume in percent
     */
    public int getVolume(@NotNull SoundType type) {
        return this.volume[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the sound type
     * @return the volume in percent
     */
    public int getVolume(int typeIndex) {
        return this.volume[typeIndex];
    }

    /**
     * Sets the volume in percent.
     *
     * @param type  the sound type
     * @param value the value
     */
    public void setVolume(@NotNull SoundType type, int value) {
        this.volume[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the volume in percent.
     *
     * @param typeIndex the index of the sound type
     * @param value     the value
     */
    public void setVolume(int typeIndex, int value) {
        this.volume[typeIndex] = value;

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
        mixColumn(hash, this.priority);
        mixColumn(hash, this.volume);
    }

    /**
     * Writes the engine columns, column by column in the order of their hash, each whole in type index order.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    void writeEngineColumns(@NotNull StateSnapshotWriter writer) {
        writer.writeInts(this.priority);
        writer.writeInts(this.volume);
    }

    /**
     * Reads the engine columns back, column by column in the order of their hash, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or a column holds another number of values
     */
    @Override
    void readEngineColumns(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readInts(this.priority);
        reader.readInts(this.volume);
    }
}

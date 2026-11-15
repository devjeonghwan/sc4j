package org.fogofwarlabs.sc4j.engine.content.table;

import org.fogofwarlabs.sc4j.engine.content.definition.SpriteDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.TableKind;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.DefinitionRegistry;
import org.fogofwarlabs.sc4j.engine.content.type.ImageType;
import org.fogofwarlabs.sc4j.engine.content.type.SpriteType;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Holds the per-game values of every {@link SpriteType} as one array per column, indexed by the type index.
 * <p>
 * The engine columns, in state hash order, are {@code image} and {@code selectionSize}. A type reference holds the
 * index of the referenced type, {@link ContentType#NO_INDEX} for none. The extension columns the mod registered for
 * {@link TableKind#SPRITE} follow ({@link #getExtensionColumns()}).
 * <p>
 * The values start as the {@link SpriteDefinition} of each type and may be written during the game; see {@link TypeTable}
 * for the write rules. Every accessor is a plain array access: an index outside the table throws
 * {@link ArrayIndexOutOfBoundsException}.
 */
public final class SpriteTypeTable extends TypeTable {
    private final int @NotNull [] image;
    private final int @NotNull [] selectionSize;

    /**
     * Creates a {@link SpriteTypeTable} instance from the definitions of a content.
     *
     * @param content the content definition
     * @throws IllegalArgumentException if a definition refers to a type that is not registered in the content
     */
    SpriteTypeTable(@NotNull ContentDefinition content) {
        super(content, content.getSpriteTypes().getCount(), content.getColumns(TableKind.SPRITE));

        DefinitionRegistry<SpriteType, SpriteDefinition> definitions = content.getSpriteTypes();
        int count = this.getCount();

        this.image         = new int[count];
        this.selectionSize = new int[count];

        for (int index = 0; index < count; index++) {
            SpriteDefinition definition = definitions.getDefinition(index);

            this.image[index]         = content.getImageTypes().getTypes().requireIndex(definition.getImage());
            this.selectionSize[index] = definition.getSelectionSize();
        }

        this.initializeExtensionColumns();
    }

    /**
     * @param type the sprite type
     * @return the primary image type, or {@code null} for none
     * @throws IndexOutOfBoundsException if the stored index names no registered type
     */
    public @Nullable ImageType getImage(@NotNull SpriteType type) {
        return resolve(this.getContent().getImageTypes().getTypes(), this.image[type.getIndex()]);
    }

    /**
     * @param typeIndex the index of the sprite type
     * @return the index of the primary image type, or {@link ContentType#NO_INDEX} for none
     */
    public int getImageIndex(int typeIndex) {
        return this.image[typeIndex];
    }

    /**
     * Sets the primary image type.
     *
     * @param type  the sprite type
     * @param value the image type registered in the same content, or {@code null} for none
     */
    public void setImage(@NotNull SpriteType type, @Nullable ImageType value) {
        this.image[type.getIndex()] = ContentType.indexOf(value);

        this.recordWrite();
    }

    /**
     * Sets the primary image type.
     *
     * @param typeIndex the index of the sprite type
     * @param value     the index of the referenced type, or {@link ContentType#NO_INDEX} for none
     */
    public void setImageIndex(int typeIndex, int value) {
        this.image[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the sprite type
     * @return the size of the selection marker in pixels, {@code 0} without one
     */
    public int getSelectionSize(@NotNull SpriteType type) {
        return this.selectionSize[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the sprite type
     * @return the size of the selection marker in pixels, {@code 0} without one
     */
    public int getSelectionSize(int typeIndex) {
        return this.selectionSize[typeIndex];
    }

    /**
     * Sets the size of the selection marker in pixels, {@code 0} without one.
     *
     * @param type  the sprite type
     * @param value the value
     */
    public void setSelectionSize(@NotNull SpriteType type, int value) {
        this.selectionSize[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the size of the selection marker in pixels, {@code 0} without one.
     *
     * @param typeIndex the index of the sprite type
     * @param value     the value
     */
    public void setSelectionSize(int typeIndex, int value) {
        this.selectionSize[typeIndex] = value;

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
        mixColumn(hash, this.image);
        mixColumn(hash, this.selectionSize);
    }

    /**
     * Writes the engine columns, column by column in the order of their hash, each whole in type index order.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    void writeEngineColumns(@NotNull StateSnapshotWriter writer) {
        writer.writeInts(this.image);
        writer.writeInts(this.selectionSize);
    }

    /**
     * Reads the engine columns back, column by column in the order of their hash, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or a column holds another number of values
     */
    @Override
    void readEngineColumns(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readInts(this.image);
        reader.readInts(this.selectionSize);
    }
}

package org.fogofwarlabs.sc4j.engine.content.table;

import org.fogofwarlabs.sc4j.engine.animation.AnimationSet;
import org.fogofwarlabs.sc4j.engine.content.definition.ImageDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.TableKind;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.DefinitionRegistry;
import org.fogofwarlabs.sc4j.engine.content.type.ImageType;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Holds the per-game values of every {@link ImageType} as one array per column, indexed by the type index.
 * <p>
 * The engine columns, in state hash order, are {@code animationSetIndex}, {@code turning} and {@code clickable}. The
 * extension columns the mod registered for {@link TableKind#IMAGE} follow ({@link #getExtensionColumns()}).
 * <p>
 * The values start as the {@link ImageDefinition} of each type and may be written during the game; see {@link TypeTable}
 * for the write rules. Every accessor is a plain array access: an index outside the table throws
 * {@link ArrayIndexOutOfBoundsException}.
 */
public final class ImageTypeTable extends TypeTable {
    private final int @NotNull [] animationSetIndex;
    private final boolean @NotNull [] turning;
    private final boolean @NotNull [] clickable;

    /**
     * Creates an {@link ImageTypeTable} instance from the definitions of a content.
     *
     * @param content the content definition
     * @throws IllegalArgumentException if a definition refers to a type that is not registered in the content
     */
    ImageTypeTable(@NotNull ContentDefinition content) {
        super(content, content.getImageTypes().getCount(), content.getColumns(TableKind.IMAGE));

        DefinitionRegistry<ImageType, ImageDefinition> definitions = content.getImageTypes();
        int count = this.getCount();

        this.animationSetIndex = new int[count];
        this.turning           = new boolean[count];
        this.clickable         = new boolean[count];

        for (int index = 0; index < count; index++) {
            ImageDefinition definition = definitions.getDefinition(index);
            AnimationSet animationSet = definition.getAnimationSet();

            this.animationSetIndex[index] = animationSet != null ? animationSet.getIndex() : ContentType.NO_INDEX;
            this.turning[index]           = definition.isTurning();
            this.clickable[index]         = definition.isClickable();
        }

        this.initializeExtensionColumns();
    }

    /**
     * @param type the image type
     * @return the index of the animation set in the animation library of the mod, {@link ContentType#NO_INDEX} without one
     */
    public int getAnimationSetIndex(@NotNull ImageType type) {
        return this.animationSetIndex[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the image type
     * @return the index of the animation set in the animation library of the mod, {@link ContentType#NO_INDEX} without one
     */
    public int getAnimationSetIndex(int typeIndex) {
        return this.animationSetIndex[typeIndex];
    }

    /**
     * Sets the index of the animation set in the animation library of the mod, {@link ContentType#NO_INDEX} without one.
     *
     * @param type  the image type
     * @param value the value
     */
    public void setAnimationSetIndex(@NotNull ImageType type, int value) {
        this.animationSetIndex[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the index of the animation set in the animation library of the mod, {@link ContentType#NO_INDEX} without one.
     *
     * @param typeIndex the index of the image type
     * @param value     the value
     */
    public void setAnimationSetIndex(int typeIndex, int value) {
        this.animationSetIndex[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the image type
     * @return {@code true} if the image has frames for each direction, {@code false} otherwise
     */
    public boolean isTurning(@NotNull ImageType type) {
        return this.turning[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the image type
     * @return {@code true} if the image has frames for each direction, {@code false} otherwise
     */
    public boolean isTurning(int typeIndex) {
        return this.turning[typeIndex];
    }

    /**
     * Sets whether the image has frames for each direction.
     *
     * @param type  the image type
     * @param value the value
     */
    public void setTurning(@NotNull ImageType type, boolean value) {
        this.turning[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets whether the image has frames for each direction.
     *
     * @param typeIndex the index of the image type
     * @param value     the value
     */
    public void setTurning(int typeIndex, boolean value) {
        this.turning[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the image type
     * @return {@code true} if a click on the image selects its owner, {@code false} otherwise
     */
    public boolean isClickable(@NotNull ImageType type) {
        return this.clickable[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the image type
     * @return {@code true} if a click on the image selects its owner, {@code false} otherwise
     */
    public boolean isClickable(int typeIndex) {
        return this.clickable[typeIndex];
    }

    /**
     * Sets whether a click on the image selects its owner.
     *
     * @param type  the image type
     * @param value the value
     */
    public void setClickable(@NotNull ImageType type, boolean value) {
        this.clickable[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets whether a click on the image selects its owner.
     *
     * @param typeIndex the index of the image type
     * @param value     the value
     */
    public void setClickable(int typeIndex, boolean value) {
        this.clickable[typeIndex] = value;

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
        mixColumn(hash, this.animationSetIndex);
        mixColumn(hash, this.turning);
        mixColumn(hash, this.clickable);
    }

    /**
     * Writes the engine columns, column by column in the order of their hash, each whole in type index order.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    void writeEngineColumns(@NotNull StateSnapshotWriter writer) {
        writer.writeInts(this.animationSetIndex);
        writer.writeBooleans(this.turning);
        writer.writeBooleans(this.clickable);
    }

    /**
     * Reads the engine columns back, column by column in the order of their hash, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or a column holds another number of values
     */
    @Override
    void readEngineColumns(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readInts(this.animationSetIndex);
        reader.readBooleans(this.turning);
        reader.readBooleans(this.clickable);
    }
}

package org.fogofwarlabs.sc4j.engine.entity;

import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.ImageType;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Holds the engine fields of an image: its type, its current frame and its offset from the sprite in pixels.
 * <p>
 * The animation program state comes with the animation system; a mod's own image data is an extension column of
 * the image pool, or the fields of its subclass when the mod pools its images itself (the index is then the mod pool's
 * own slot index).
 */
public class Image extends Entity {
    private @Nullable ImageType type;
    private int frameIndex;
    private int offsetX;
    private int offsetY;

    /**
     * Creates an {@link Image} instance for one pool slot, without type at frame {@code 0} without offset.
     *
     * @param index the slot index, {@code 1} up to the pool capacity
     */
    public Image(int index) {
        super(index);

        this.type       = null;
        this.frameIndex = 0;
        this.offsetX    = 0;
        this.offsetY    = 0;
    }

    /**
     * @return the image type, or {@code null} before one is set
     */
    public @Nullable ImageType getType() {
        return this.type;
    }

    /**
     * Sets the image type.
     *
     * @param type the image type, registered in the game content, or {@code null} for none
     */
    public void setType(@Nullable ImageType type) {
        this.type = type;
    }

    /**
     * @return the index of the current frame of the image type's frames
     */
    public int getFrameIndex() {
        return this.frameIndex;
    }

    /**
     * Sets the current frame.
     *
     * @param frameIndex the index of the frame of the image type's frames
     */
    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }

    /**
     * @return the horizontal offset from the sprite position in pixels
     */
    public int getOffsetX() {
        return this.offsetX;
    }

    /**
     * @return the vertical offset from the sprite position in pixels
     */
    public int getOffsetY() {
        return this.offsetY;
    }

    /**
     * Sets the offset from the sprite position.
     *
     * @param offsetX the horizontal offset in pixels
     * @param offsetY the vertical offset in pixels
     */
    public void setOffset(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }


    /**
     * Mixes this part of the simulation state into the hash: the type index, the frame index and the offset.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        hash.mix(ContentType.indexOf(this.type));
        hash.mix(this.frameIndex);
        hash.mix(this.offsetX);
        hash.mix(this.offsetY);
    }

    /**
     * Writes this part of the simulation state: the type index, the frame index and the offset.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeType(this.type);
        writer.writeInt(this.frameIndex);
        writer.writeInt(this.offsetX);
        writer.writeInt(this.offsetY);
    }

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or names no image type of the game
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        this.type       = reader.readType(reader.getGame().getContent().getDefinition().getImageTypes().getTypes());
        this.frameIndex = reader.readInt();
        this.offsetX    = reader.readInt();
        this.offsetY    = reader.readInt();
    }
}

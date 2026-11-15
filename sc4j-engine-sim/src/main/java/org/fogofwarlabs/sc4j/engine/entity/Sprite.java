package org.fogofwarlabs.sc4j.engine.entity;

import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.SpriteType;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Holds the engine fields of a sprite: its type and its position in pixels.
 * <p>
 * The images of a sprite and the animation state come with the animation system; a mod's own sprite data is an
 * extension column of the sprite pool.
 */
public final class Sprite extends Entity {
    private @Nullable SpriteType type;
    private int positionX;
    private int positionY;

    /**
     * Creates a {@link Sprite} instance for one pool slot, without type at {@code (0, 0)}.
     *
     * @param index the slot index, {@code 1} up to the pool capacity
     */
    public Sprite(int index) {
        super(index);

        this.type      = null;
        this.positionX = 0;
        this.positionY = 0;
    }

    /**
     * @return the sprite type, or {@code null} before one is set
     */
    public @Nullable SpriteType getType() {
        return this.type;
    }

    /**
     * Sets the sprite type.
     *
     * @param type the sprite type, registered in the game content
     */
    public void setType(@NotNull SpriteType type) {
        this.type = type;
    }

    /**
     * @return the x coordinate in pixels
     */
    public int getPositionX() {
        return this.positionX;
    }

    /**
     * @return the y coordinate in pixels
     */
    public int getPositionY() {
        return this.positionY;
    }

    /**
     * Sets the position.
     *
     * @param positionX the x coordinate in pixels
     * @param positionY the y coordinate in pixels
     */
    public void setPosition(int positionX, int positionY) {
        this.positionX = positionX;
        this.positionY = positionY;
    }


    /**
     * Mixes this part of the simulation state into the hash: the type index and the position.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        hash.mix(ContentType.indexOf(this.type));
        hash.mix(this.positionX);
        hash.mix(this.positionY);
    }

    /**
     * Writes this part of the simulation state: the type index and the position.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeType(this.type);
        writer.writeInt(this.positionX);
        writer.writeInt(this.positionY);
    }

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or names no sprite type of the game
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        this.type      = reader.readType(reader.getGame().getContent().getDefinition().getSpriteTypes().getTypes());
        this.positionX = reader.readInt();
        this.positionY = reader.readInt();
    }
}

package org.fogofwarlabs.sc4j.engine.content.table;

import org.fogofwarlabs.sc4j.engine.content.definition.MotionProfileDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.TableKind;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.DefinitionRegistry;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.SpriteType;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Holds the per-game values of every {@link MotionProfile} as one array per column, indexed by the type index.
 * <p>
 * The engine columns, in state hash order, are {@code sprite}, {@code topSpeed}, {@code acceleration},
 * {@code stoppingDistance}, {@code turnRate} and {@code acceleratingTurn}. A type reference holds the index of the referenced type,
 * {@link ContentType#NO_INDEX} for none. The extension columns the mod registered for {@link TableKind#MOTION_PROFILE} follow
 * ({@link #getExtensionColumns()}).
 * <p>
 * The values start as the {@link MotionProfileDefinition} of each type and may be written during the game; see {@link TypeTable}
 * for the write rules. Every accessor is a plain array access: an index outside the table throws
 * {@link ArrayIndexOutOfBoundsException}.
 */
public final class MotionProfileTable extends TypeTable {
    private final int @NotNull [] sprite;
    private final int @NotNull [] topSpeed;
    private final int @NotNull [] acceleration;
    private final int @NotNull [] stoppingDistance;
    private final int @NotNull [] turnRate;
    private final boolean @NotNull [] acceleratingTurn;

    /**
     * Creates a {@link MotionProfileTable} instance from the definitions of a content.
     *
     * @param content the content definition
     * @throws IllegalArgumentException if a definition refers to a type that is not registered in the content
     */
    MotionProfileTable(@NotNull ContentDefinition content) {
        super(content, content.getMotionProfiles().getCount(), content.getColumns(TableKind.MOTION_PROFILE));

        DefinitionRegistry<MotionProfile, MotionProfileDefinition> definitions = content.getMotionProfiles();
        int count = this.getCount();

        this.sprite           = new int[count];
        this.topSpeed         = new int[count];
        this.acceleration     = new int[count];
        this.stoppingDistance = new int[count];
        this.turnRate         = new int[count];
        this.acceleratingTurn = new boolean[count];

        for (int index = 0; index < count; index++) {
            MotionProfileDefinition definition = definitions.getDefinition(index);

            this.sprite[index]           = content.getSpriteTypes().getTypes().requireIndex(definition.getSprite());
            this.topSpeed[index]         = definition.getTopSpeed();
            this.acceleration[index]     = definition.getAcceleration();
            this.stoppingDistance[index] = definition.getStoppingDistance();
            this.turnRate[index]         = definition.getTurnRate();
            this.acceleratingTurn[index] = definition.isAcceleratingTurn();
        }

        this.initializeExtensionColumns();
    }

    /**
     * @param type the motion profile
     * @return the sprite type that draws the object, or {@code null} for none
     * @throws IndexOutOfBoundsException if the stored index names no registered type
     */
    public @Nullable SpriteType getSprite(@NotNull MotionProfile type) {
        return resolve(this.getContent().getSpriteTypes().getTypes(), this.sprite[type.getIndex()]);
    }

    /**
     * @param typeIndex the index of the motion profile
     * @return the index of the sprite type that draws the object, or {@link ContentType#NO_INDEX} for none
     */
    public int getSpriteIndex(int typeIndex) {
        return this.sprite[typeIndex];
    }

    /**
     * Sets the sprite type that draws the object.
     *
     * @param type  the motion profile
     * @param value the sprite type registered in the same content, or {@code null} for none
     */
    public void setSprite(@NotNull MotionProfile type, @Nullable SpriteType value) {
        this.sprite[type.getIndex()] = ContentType.indexOf(value);

        this.recordWrite();
    }

    /**
     * Sets the sprite type that draws the object.
     *
     * @param typeIndex the index of the motion profile
     * @param value     the index of the referenced type, or {@link ContentType#NO_INDEX} for none
     */
    public void setSpriteIndex(int typeIndex, int value) {
        this.sprite[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the motion profile
     * @return the top speed, in the speed unit of the mod
     */
    public int getTopSpeed(@NotNull MotionProfile type) {
        return this.topSpeed[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the motion profile
     * @return the top speed, in the speed unit of the mod
     */
    public int getTopSpeed(int typeIndex) {
        return this.topSpeed[typeIndex];
    }

    /**
     * Sets the top speed, in the speed unit of the mod.
     *
     * @param type  the motion profile
     * @param value the value
     */
    public void setTopSpeed(@NotNull MotionProfile type, int value) {
        this.topSpeed[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the top speed, in the speed unit of the mod.
     *
     * @param typeIndex the index of the motion profile
     * @param value     the value
     */
    public void setTopSpeed(int typeIndex, int value) {
        this.topSpeed[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the motion profile
     * @return the acceleration, in the speed unit of the mod per frame
     */
    public int getAcceleration(@NotNull MotionProfile type) {
        return this.acceleration[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the motion profile
     * @return the acceleration, in the speed unit of the mod per frame
     */
    public int getAcceleration(int typeIndex) {
        return this.acceleration[typeIndex];
    }

    /**
     * Sets the acceleration, in the speed unit of the mod per frame.
     *
     * @param type  the motion profile
     * @param value the value
     */
    public void setAcceleration(@NotNull MotionProfile type, int value) {
        this.acceleration[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the acceleration, in the speed unit of the mod per frame.
     *
     * @param typeIndex the index of the motion profile
     * @param value     the value
     */
    public void setAcceleration(int typeIndex, int value) {
        this.acceleration[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the motion profile
     * @return the distance needed to stop from top speed, in the distance unit of the mod
     */
    public int getStoppingDistance(@NotNull MotionProfile type) {
        return this.stoppingDistance[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the motion profile
     * @return the distance needed to stop from top speed, in the distance unit of the mod
     */
    public int getStoppingDistance(int typeIndex) {
        return this.stoppingDistance[typeIndex];
    }

    /**
     * Sets the distance needed to stop from top speed, in the distance unit of the mod.
     *
     * @param type  the motion profile
     * @param value the value
     */
    public void setStoppingDistance(@NotNull MotionProfile type, int value) {
        this.stoppingDistance[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the distance needed to stop from top speed, in the distance unit of the mod.
     *
     * @param typeIndex the index of the motion profile
     * @param value     the value
     */
    public void setStoppingDistance(int typeIndex, int value) {
        this.stoppingDistance[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the motion profile
     * @return the direction change per frame, in the direction unit of the mod
     */
    public int getTurnRate(@NotNull MotionProfile type) {
        return this.turnRate[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the motion profile
     * @return the direction change per frame, in the direction unit of the mod
     */
    public int getTurnRate(int typeIndex) {
        return this.turnRate[typeIndex];
    }

    /**
     * Sets the direction change per frame, in the direction unit of the mod.
     *
     * @param type  the motion profile
     * @param value the value
     */
    public void setTurnRate(@NotNull MotionProfile type, int value) {
        this.turnRate[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the direction change per frame, in the direction unit of the mod.
     *
     * @param typeIndex the index of the motion profile
     * @param value     the value
     */
    public void setTurnRate(int typeIndex, int value) {
        this.turnRate[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the motion profile
     * @return {@code true} if the turn rate of an object grows by one each frame it turns, {@code false} otherwise
     */
    public boolean isAcceleratingTurn(@NotNull MotionProfile type) {
        return this.acceleratingTurn[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the motion profile
     * @return {@code true} if the turn rate of an object grows by one each frame it turns, {@code false} otherwise
     */
    public boolean isAcceleratingTurn(int typeIndex) {
        return this.acceleratingTurn[typeIndex];
    }

    /**
     * Sets whether the turn rate of an object grows by one each frame it turns.
     *
     * @param type  the motion profile
     * @param value the value
     */
    public void setAcceleratingTurn(@NotNull MotionProfile type, boolean value) {
        this.acceleratingTurn[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets whether the turn rate of an object grows by one each frame it turns.
     *
     * @param typeIndex the index of the motion profile
     * @param value     the value
     */
    public void setAcceleratingTurn(int typeIndex, boolean value) {
        this.acceleratingTurn[typeIndex] = value;

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
        mixColumn(hash, this.sprite);
        mixColumn(hash, this.topSpeed);
        mixColumn(hash, this.acceleration);
        mixColumn(hash, this.stoppingDistance);
        mixColumn(hash, this.turnRate);
        mixColumn(hash, this.acceleratingTurn);
    }

    /**
     * Writes the engine columns, column by column in the order of their hash, each whole in type index order.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    void writeEngineColumns(@NotNull StateSnapshotWriter writer) {
        writer.writeInts(this.sprite);
        writer.writeInts(this.topSpeed);
        writer.writeInts(this.acceleration);
        writer.writeInts(this.stoppingDistance);
        writer.writeInts(this.turnRate);
        writer.writeBooleans(this.acceleratingTurn);
    }

    /**
     * Reads the engine columns back, column by column in the order of their hash, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or a column holds another number of values
     */
    @Override
    void readEngineColumns(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readInts(this.sprite);
        reader.readInts(this.topSpeed);
        reader.readInts(this.acceleration);
        reader.readInts(this.stoppingDistance);
        reader.readInts(this.turnRate);
        reader.readBooleans(this.acceleratingTurn);
    }
}

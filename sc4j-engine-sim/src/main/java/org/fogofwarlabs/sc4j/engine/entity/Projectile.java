package org.fogofwarlabs.sc4j.engine.entity;

import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Holds the engine fields of a projectile: its position and the motion state a motion model moves it by ({@link MovingEntity}). What it carries
 * (weapon, source, target) belongs to the projectile system that launched it. A mod that pools its projectiles itself extends this class, so
 * the engine motion model or the mod's own motion algorithm moves its projectiles over the same fields.
 */
public class Projectile extends MovingEntity {
    /**
     * Creates a {@link Projectile} instance for one pool slot at {@code (0, 0)} with the motion fields at their initial values.
     *
     * @param index the slot index, {@code 1} up to the pool capacity
     */
    public Projectile(int index) {
        super(index);
    }

    /**
     * Mixes this part of the simulation state into the hash: the position, the motion profile index and the motion fields in declaration order.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        this.contributeMotionStateHash(hash);
    }

    /**
     * Writes this part of the simulation state: the position, the motion profile index and the motion fields in declaration order.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        this.writeMotionSnapshot(writer);
    }

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or names no motion profile of the game
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        this.readMotionSnapshot(reader);
    }
}

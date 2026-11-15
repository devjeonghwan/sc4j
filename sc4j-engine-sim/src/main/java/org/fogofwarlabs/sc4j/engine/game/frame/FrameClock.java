package org.fogofwarlabs.sc4j.engine.game.frame;

import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Counts the frames of a game and holds whether it is paused.
 * <p>
 * Between frames {@link #getFrame()} is the number of frames that advanced the game; during a frame it is the number of
 * the running frame, {@code 1} for the first. A paused frame does not advance the counter.
 */
public final class FrameClock implements StateHashContributor, SnapshotContributor {
    private int frame;
    private boolean paused;

    /**
     * Creates a {@link FrameClock} instance at frame {@code 0}, not paused.
     */
    public FrameClock() {
        this.frame  = 0;
        this.paused = false;
    }

    /**
     * @return the frame number: the frames that advanced the game, including the running one
     */
    public int getFrame() {
        return this.frame;
    }

    /**
     * Sets the frame number as it is, for example when a saved game is restored.
     *
     * @param frame the frame number
     */
    public void setFrame(int frame) {
        this.frame = frame;
    }

    /**
     * @return {@code true} if the next frames do not advance the game, {@code false} otherwise
     */
    public boolean isPaused() {
        return this.paused;
    }

    /**
     * Sets whether the next frames advance the game. A command or a phase that runs while paused sets it; it takes effect
     * at the next frame.
     *
     * @param paused whether the next frames do not advance the game
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }


    /**
     * Advances the frame counter at the start of a frame that advances the game.
     */
    void advance() {
        this.frame++;
    }

    /**
     * Mixes this part of the simulation state into the hash: the frame number and the paused flag.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        hash.mix(this.frame);
        hash.mixBoolean(this.paused);
    }

    /**
     * Writes this part of the simulation state: the frame number and the paused flag.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInt(this.frame);
        writer.writeBoolean(this.paused);
    }

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        this.frame  = reader.readInt();
        this.paused = reader.readBoolean();
    }
}

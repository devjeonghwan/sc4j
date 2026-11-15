package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Holds the execution state of one trigger for one player in the standard trigger system: the action it resumes at, the frames a wait action
 * still waits, whether it repeats and whether it is done. An action that does not finish keeps its state here between runs.
 */
public final class RunningTrigger implements SnapshotContributor {
    private final @NotNull TriggerDefinition definition;
    private final @NotNull Player player;

    private int actionIndex;
    private int waitRemaining;
    private boolean waiting;
    private boolean repeating;
    private boolean done;

    /**
     * Creates a {@link RunningTrigger} instance that has not run.
     *
     * @param definition the trigger
     * @param player     the player it runs for
     */
    public RunningTrigger(@NotNull TriggerDefinition definition, @NotNull Player player) {
        this.definition = definition;
        this.player     = player;

        this.actionIndex   = 0;
        this.waitRemaining = 0;
        this.waiting       = false;
        this.repeating     = definition.isRepeating();
        this.done          = false;
    }

    /**
     * @return the trigger
     */
    public @NotNull TriggerDefinition getDefinition() {
        return this.definition;
    }

    /**
     * @return the player the trigger runs for
     */
    public @NotNull Player getPlayer() {
        return this.player;
    }

    /**
     * @return the index of the action the next run resumes at, {@code 0} when the trigger tests its conditions first
     */
    public int getActionIndex() {
        return this.actionIndex;
    }

    /**
     * Sets the index of the action the next run resumes at.
     *
     * @param actionIndex the action index, {@code 0} to test the conditions first
     */
    public void setActionIndex(int actionIndex) {
        this.actionIndex = actionIndex;
    }

    /**
     * @return {@code true} if a wait action of the trigger is waiting, {@code false} otherwise
     */
    public boolean isWaiting() {
        return this.waiting;
    }

    /**
     * @return the frames the waiting action still waits
     */
    public int getWaitRemaining() {
        return this.waitRemaining;
    }

    /**
     * Sets the wait of a wait action.
     *
     * @param waiting       whether the action waits
     * @param waitRemaining the frames it still waits
     */
    public void setWait(boolean waiting, int waitRemaining) {
        this.waiting       = waiting;
        this.waitRemaining = waitRemaining;
    }

    /**
     * @return {@code true} if the trigger runs again each time its conditions hold, {@code false} if it is done after one run
     */
    public boolean isRepeating() {
        return this.repeating;
    }

    /**
     * Sets whether the trigger runs again each time its conditions hold.
     *
     * @param repeating whether the trigger repeats
     */
    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
    }

    /**
     * @return {@code true} if the trigger ran and does not repeat, so it does not run again, {@code false} otherwise
     */
    public boolean isDone() {
        return this.done;
    }

    /**
     * Sets whether the trigger is done.
     *
     * @param done whether the trigger is done
     */
    public void setDone(boolean done) {
        this.done = done;
    }

    /**
     * Mixes the execution state into a state hash: the action index, the wait, the repeating and the done flag.
     *
     * @param hash the hash being computed
     */
    public void contributeStateHash(@NotNull StateHash hash) {
        hash.mix(this.actionIndex);
        hash.mix(this.waitRemaining);
        hash.mixBoolean(this.waiting);
        hash.mixBoolean(this.repeating);
        hash.mixBoolean(this.done);
    }

    /**
     * Writes this part of the simulation state: the action index, the remaining wait, the waiting, repeating and done flags.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInt(this.actionIndex);
        writer.writeInt(this.waitRemaining);
        writer.writeBoolean(this.waiting);
        writer.writeBoolean(this.repeating);
        writer.writeBoolean(this.done);
    }

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        this.actionIndex   = reader.readInt();
        this.waitRemaining = reader.readInt();
        this.waiting       = reader.readBoolean();
        this.repeating     = reader.readBoolean();
        this.done          = reader.readBoolean();
    }
}

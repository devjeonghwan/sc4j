package org.fogofwarlabs.sc4j.engine.client.scene;

import org.jetbrains.annotations.NotNull;

/**
 * Describes a piece of loading work that {@link LoadingService} advances on the render thread in time slices, so that the loading screen
 * keeps drawing: for example an image set, text tables or the data of a game.
 */
public interface LoadingTask {
    /**
     * @return the text shown on the loading screen, for example {@code "Loading graphics"}
     */
    @NotNull String getTitle();

    /**
     * @return {@code true} if the task is complete or canceled, {@code false} otherwise
     */
    boolean isFinished();

    /**
     * Does up to about {@code budgetNanoseconds} of work on the render thread (with the graphics context current).
     *
     * @param budgetNanoseconds the time after which the call returns
     * @return the progress from {@code 0} to {@code 1}
     */
    float advance(long budgetNanoseconds);

    /**
     * Stops the task and frees its partial work; afterwards {@link #isFinished()} returns {@code true}.
     */
    void cancel();
}

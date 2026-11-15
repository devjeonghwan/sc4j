package org.fogofwarlabs.sc4j.engine.client.scene;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Runs {@link LoadingTask}s one after another on the render thread in time slices, so that a loading screen keeps drawing: the client loop
 * calls {@link #advance(long)} once per frame while {@link #isLoading()} and then draws its loading screen from {@link #getTitle()} and
 * {@link #getProgress()}. One service serves every loading of a client (startup, a graphics change, a game start); input and the simulation
 * wait meanwhile.
 */
public final class LoadingService {
    private final @NotNull Deque<LoadingTask> queue;

    private @Nullable LoadingTask current;
    private float progress;

    /**
     * Creates a {@link LoadingService} instance.
     */
    public LoadingService() {
        this.queue = new ArrayDeque<>();

        this.current  = null;
        this.progress = 0;
    }

    /**
     * @return {@code true} if a task is running or queued, {@code false} otherwise
     */
    public boolean isLoading() {
        return this.current != null;
    }

    /**
     * @return the progress of the running task from {@code 0} to {@code 1}, or {@code 1} when idle
     */
    public float getProgress() {
        return this.current == null ? 1 : this.progress;
    }

    /**
     * @return the title of the running task, or {@code null} when idle
     */
    public @Nullable String getTitle() {
        return this.current == null ? null : this.current.getTitle();
    }


    /**
     * Starts a task, canceling the running and queued ones (a newer request of the same kind replaces an older one).
     *
     * @param task the task
     */
    public void start(@NotNull LoadingTask task) {
        this.cancel();

        this.current  = task;
        this.progress = 0;
    }

    /**
     * Queues a task after the running one, or starts it when idle.
     *
     * @param task the task
     */
    public void enqueue(@NotNull LoadingTask task) {
        if (this.current == null) {
            this.current  = task;
            this.progress = 0;
        } else {
            this.queue.addLast(task);
        }
    }

    /**
     * Advances the running task by up to a time budget; a finished task is replaced by the next queued one within the same budget.
     *
     * @param budgetNanoseconds the render-thread time for this frame
     */
    public void advance(long budgetNanoseconds) {
        long deadline = System.nanoTime() + budgetNanoseconds;

        while (this.current != null) {
            long remaining = deadline - System.nanoTime();

            if (remaining <= 0) {
                return;
            }

            this.progress = Math.max(0, Math.min(1, this.current.advance(remaining)));

            if (!this.current.isFinished()) {
                return;
            }

            this.current  = this.queue.pollFirst();
            this.progress = 0;
        }
    }

    /**
     * Cancels the running and queued tasks.
     */
    public void cancel() {
        if (this.current != null) {
            this.current.cancel();
        }

        for (LoadingTask task : this.queue) {
            task.cancel();
        }

        this.queue.clear();
        this.current = null;
    }
}

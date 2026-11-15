package org.fogofwarlabs.sc4j.engine.client.scene;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine loading service: task order, progress, titles and cancellation.
 */
final class LoadingServiceTest {
    private static final long BUDGET = 1_000_000_000L;
    private static final float HALF = 0.5f;

    /**
     * Finishes after a fixed number of steps, one step per {@link #advance(long)} call.
     */
    private static final class CountingTask implements LoadingTask {
        private final @NotNull String title;
        private final int steps;

        private int done;
        private boolean canceled;

        private CountingTask(@NotNull String title, int steps) {
            this.title = title;
            this.steps = steps;

            this.done     = 0;
            this.canceled = false;
        }

        /**
         * @return the text shown on the loading screen, for example {@code "Loading graphics"}
         */
        @Override
        public @NotNull String getTitle() {
            return this.title;
        }

        /**
         * @return {@code true} if the task is complete or canceled, {@code false} otherwise
         */
        @Override
        public boolean isFinished() {
            return this.canceled || this.done >= this.steps;
        }

        /**
         * Does up to about {@code budgetNanoseconds} of work on the render thread (with the graphics context current).
         *
         * @param budgetNanoseconds the time after which the call returns
         * @return the progress from {@code 0} to {@code 1}
         */
        @Override
        public float advance(long budgetNanoseconds) {
            this.done++;

            return (float) this.done / this.steps;
        }

        /**
         * Stops the task and frees its partial work; afterwards {@link #isFinished()} returns {@code true}.
         */
        @Override
        public void cancel() {
            this.canceled = true;
        }
    }

    @Test
    void idle_service_reports_no_title_and_full_progress() {
        LoadingService service = new LoadingService();

        Assertions.assertFalse(service.isLoading());
        Assertions.assertNull(service.getTitle());
        Assertions.assertEquals(1, service.getProgress());
    }

    @Test
    void advance_should_run_queued_tasks_in_order_and_report_progress() {
        LoadingService service = new LoadingService();
        CountingTask first = new CountingTask("first", 2);
        CountingTask second = new CountingTask("second", 1);

        service.enqueue(first);
        service.enqueue(second);

        Assertions.assertTrue(service.isLoading());
        Assertions.assertEquals("first", service.getTitle());
        Assertions.assertEquals(0, service.getProgress());

        service.advance(BUDGET);

        Assertions.assertEquals("first", service.getTitle());
        Assertions.assertEquals(HALF, service.getProgress());

        // The finished task hands the rest of the budget to the next one.
        service.advance(BUDGET);

        Assertions.assertTrue(first.isFinished());
        Assertions.assertTrue(second.isFinished());
        Assertions.assertFalse(service.isLoading());
    }

    @Test
    void start_should_cancel_the_running_and_queued_tasks() {
        LoadingService service = new LoadingService();
        CountingTask running = new CountingTask("running", 2);
        CountingTask queued = new CountingTask("queued", 2);
        CountingTask replacement = new CountingTask("replacement", 2);

        service.enqueue(running);
        service.enqueue(queued);
        service.start(replacement);

        Assertions.assertTrue(running.isFinished());
        Assertions.assertTrue(queued.isFinished());
        Assertions.assertEquals("replacement", service.getTitle());

        service.cancel();

        Assertions.assertTrue(replacement.isFinished());
        Assertions.assertFalse(service.isLoading());
    }

    @Test
    void advance_should_return_at_once_when_the_budget_is_spent() {
        LoadingService service = new LoadingService();
        CountingTask task = new CountingTask("task", 1);

        service.enqueue(task);
        service.advance(0);

        Assertions.assertFalse(task.isFinished());
        Assertions.assertTrue(service.isLoading());
    }
}

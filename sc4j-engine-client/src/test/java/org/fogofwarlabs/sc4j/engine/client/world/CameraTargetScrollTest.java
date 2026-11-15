package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.projection.SquareProjection;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the scripted camera move: the target is aligned, the steps shrink near it, the arrival callback runs once on arrival, when the
 * camera is held at the end of its range, or when another scroll starts, and the engine camera follows it like a game's own camera.
 */
final class CameraTargetScrollTest {
    private static final int ALIGNMENT = 8;
    private static final int SPEED = 6;
    private static final int MAXIMUM = 1000;
    private static final int TARGET = 29;
    private static final int ALIGNED_TARGET = 24;
    private static final int ARRIVAL_TICKS = 5;
    private static final int SMALL_MAXIMUM = 10;
    private static final int VIEW_SIZE = 64;
    private static final int MAP_SIZE = 2000;
    private static final int SCREEN_SIZE = 400;
    private static final int ENGINE_TARGET = 640;
    private static final int TICK_LIMIT = 100;
    private static final int SPEED_COUNT = 7;
    private static final int STEP_COUNT = 7;
    private static final int SMALL_SPEED_COUNT = 3;
    private static final int SMALL_STEP_COUNT = 5;
    private static final int SMALL_SPEED = 2;
    private static final int FAR_TARGET = 512;
    private static final int CAPPED_TICKS = 6;
    private static final int CAPPED_POSITION = 62;

    /**
     * A table of seven rows whose steps double with the index: 1, 2, 4, 8, 16, 32, 64 pixels.
     */
    private static final @NotNull ScrollStepTable DOUBLING_STEPS = new DoublingSteps(SPEED_COUNT, STEP_COUNT);

    /**
     * A table whose steps double with the index in every row.
     */
    private static final class DoublingSteps implements ScrollStepTable {
        private final int speedCount;
        private final int stepCount;

        DoublingSteps(int speedCount, int stepCount) {
            this.speedCount = speedCount;
            this.stepCount  = stepCount;
        }

        @Override
        public int getSpeedCount() {
            return this.speedCount;
        }

        @Override
        public int getStepCount() {
            return this.stepCount;
        }

        @Override
        public int getStep(int speed, int index) {
            return 1 << index;
        }
    }

    @Test
    void tick_should_reach_the_aligned_target_and_call_back_once() {
        CameraTargetScroll scroll = new CameraTargetScroll(ALIGNMENT, SPEED, DOUBLING_STEPS);
        WholePixelCamera camera = new WholePixelCamera(0, ALIGNED_TARGET, MAXIMUM, MAXIMUM, VIEW_SIZE, VIEW_SIZE);
        int[] calls = new int[1];

        scroll.start(TARGET, TARGET, () -> calls[0]++);

        Assertions.assertTrue(scroll.isActive());

        for (int tick = 0; tick < ARRIVAL_TICKS; tick++) {
            Assertions.assertTrue(scroll.tick(camera));
        }

        Assertions.assertArrayEquals(new float[]{ALIGNED_TARGET, ALIGNED_TARGET}, camera.getPosition(), "2, 6, 14, 22, then the 2 pixel step lands");
        Assertions.assertEquals(1, calls[0]);

        scroll.tick(camera);

        Assertions.assertEquals(1, calls[0], "the callback runs once");

        scroll.finish();

        Assertions.assertFalse(scroll.isActive());
        Assertions.assertFalse(scroll.tick(camera));
    }

    @Test
    void tick_should_call_back_when_the_camera_is_held_at_the_end_of_its_range() {
        CameraTargetScroll scroll = new CameraTargetScroll(ALIGNMENT, SPEED, DOUBLING_STEPS);
        WholePixelCamera camera = new WholePixelCamera(0, 0, SMALL_MAXIMUM, SMALL_MAXIMUM, VIEW_SIZE, VIEW_SIZE);
        int[] calls = new int[1];

        scroll.start(ALIGNED_TARGET, 0, () -> calls[0]++);

        while (calls[0] == 0) {
            scroll.tick(camera);
        }

        Assertions.assertEquals(SMALL_MAXIMUM, camera.getX(), "the camera stopped at its largest position");
    }

    @Test
    void tick_should_move_the_engine_camera_to_the_target() {
        Camera camera = new Camera(new SquareProjection(), MAP_SIZE, MAP_SIZE);
        CameraTargetScroll scroll = new CameraTargetScroll(ALIGNMENT, SPEED, DOUBLING_STEPS);
        boolean[] arrived = new boolean[1];

        camera.setScreenSize(SCREEN_SIZE, SCREEN_SIZE);
        scroll.start(ENGINE_TARGET, ENGINE_TARGET, () -> arrived[0] = true);

        for (int tick = 0; tick < TICK_LIMIT && !arrived[0]; tick++) {
            scroll.tick(camera);
        }

        Assertions.assertTrue(arrived[0]);
        Assertions.assertEquals(ENGINE_TARGET, camera.getX());
        Assertions.assertEquals(ENGINE_TARGET, camera.getY());
    }

    @Test
    void tick_should_stop_accelerating_at_the_last_step_of_the_table() {
        ScrollStepTable steps = new DoublingSteps(SMALL_SPEED_COUNT, SMALL_STEP_COUNT);
        CameraTargetScroll scroll = new CameraTargetScroll(ALIGNMENT, SMALL_SPEED, steps);
        WholePixelCamera camera = new WholePixelCamera(0, 0, MAXIMUM, MAXIMUM, VIEW_SIZE, VIEW_SIZE);

        scroll.start(FAR_TARGET, 0, () -> {
        });

        for (int tick = 0; tick < CAPPED_TICKS; tick++) {
            scroll.tick(camera);
        }

        Assertions.assertEquals(CAPPED_POSITION, camera.getX(), "2, 4, 8, then the fifth step of 16 pixels three times");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CameraTargetScroll(ALIGNMENT, SMALL_SPEED_COUNT, steps),
                                "a speed row past the table");
    }

    @Test
    void start_should_run_the_pending_callback_first() {
        CameraTargetScroll scroll = new CameraTargetScroll(ALIGNMENT, SPEED, DOUBLING_STEPS);
        int[] calls = new int[1];

        scroll.start(ALIGNED_TARGET, ALIGNED_TARGET, () -> calls[0]++);
        scroll.start(0, 0, () -> calls[0] += ARRIVAL_TICKS);

        Assertions.assertEquals(1, calls[0]);

        scroll.reset();

        Assertions.assertFalse(scroll.isActive());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CameraTargetScroll(ALIGNMENT + 1, SPEED, DOUBLING_STEPS));
    }
}

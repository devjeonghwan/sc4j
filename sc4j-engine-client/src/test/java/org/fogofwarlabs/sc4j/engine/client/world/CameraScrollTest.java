package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.fogofwarlabs.sc4j.engine.client.projection.SquareProjection;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Tests camera scrolling with both rates of the one scroll component: stepped key scrolling accelerates by the step table and stops at the
 * camera's range, the pointer at an edge scrolls toward a side the camera can still move and reports its direction, a resting pointer does not
 * scroll right after a key scroll, a continuous rate moves by speed and time in screen pixels, and turned-off edge or key scrolling does not
 * move the camera.
 */
final class CameraScrollTest {
    private static final int EDGE_WIDTH = 2;
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
    private static final int MAXIMUM = 1000;
    private static final int START = 100;
    private static final int CENTER_X = 320;
    private static final int CENTER_Y = 240;
    private static final int UP_LEFT = 7;
    private static final int RIGHT = 2;
    private static final int DOWN_RIGHT = 3;
    private static final int MAP_SIZE = 2000;
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final int CONTINUOUS_EDGE_WIDTH = 4;
    private static final float SPEED = 1000;
    private static final long MILLISECONDS = 100;
    private static final float DISTANCE = 100;
    private static final float DELTA = 0.001f;
    private static final int SPEED_COUNT = 7;
    private static final int STEP_COUNT = 7;
    private static final int SMALL_SPEED_COUNT = 3;
    private static final int SMALL_STEP_COUNT = 5;
    private static final int ACCELERATING_UPDATES = 8;

    private static final @NotNull ScrollStepTable STEPS = new LinearScrollStepTable(SPEED_COUNT, STEP_COUNT);

    private static void update(@NotNull CameraScroll scroll, @NotNull CameraControl camera, int keyX, int keyY, int pointerX, int pointerY,
                               boolean pointerInside) {
        scroll.update(camera, keyX, keyY, pointerX, pointerY, pointerInside, WIDTH, HEIGHT, MILLISECONDS);
    }

    private static int step(int index) {
        return STEPS.getStep(SPEED_COUNT / 2, index);
    }

    @Test
    void update_should_accelerate_stepped_key_scrolling_and_reset_without_movement() {
        CameraScroll scroll = new CameraScroll(EDGE_WIDTH, new SteppedScrollRate(STEPS));
        WholePixelCamera camera = new WholePixelCamera(START, START, MAXIMUM, MAXIMUM, WIDTH, HEIGHT);

        update(scroll, camera, 1, 0, CENTER_X, CENTER_Y, true);

        Assertions.assertArrayEquals(new float[]{START + step(1), START}, camera.getPosition(), "the first update takes step 1");

        update(scroll, camera, 1, 1, CENTER_X, CENTER_Y, true);

        Assertions.assertArrayEquals(new float[]{START + step(1) + step(2), START + step(2)}, camera.getPosition(),
                                     "the second update takes step 2 on both axes");

        float stopped = camera.getX();

        update(scroll, camera, 0, 0, CENTER_X, CENTER_Y, true);
        update(scroll, camera, 1, 0, CENTER_X, CENTER_Y, true);

        Assertions.assertEquals(stopped + step(1), camera.getX(), "a still update resets the step");
    }

    @Test
    void update_should_stop_accelerating_at_the_last_step_of_the_table() {
        ScrollStepTable steps = new LinearScrollStepTable(SMALL_SPEED_COUNT, SMALL_STEP_COUNT);
        SteppedScrollRate rate = new SteppedScrollRate(steps);
        CameraScroll scroll = new CameraScroll(EDGE_WIDTH, rate);
        WholePixelCamera camera = new WholePixelCamera(0, 0, MAXIMUM, MAXIMUM, WIDTH, HEIGHT);
        float lastStep = 0;

        for (int update = 0; update < ACCELERATING_UPDATES; update++) {
            float x = camera.getX();

            update(scroll, camera, 1, 0, CENTER_X, CENTER_Y, true);

            lastStep = camera.getX() - x;
        }

        Assertions.assertEquals(steps.getStep(rate.getKeySpeed(), SMALL_STEP_COUNT - 1), lastStep, "the fifth step of a five-step table");
    }

    @Test
    void update_should_keep_the_camera_inside_its_range() {
        CameraScroll scroll = new CameraScroll(EDGE_WIDTH, new SteppedScrollRate(STEPS));
        WholePixelCamera camera = new WholePixelCamera(1, MAXIMUM - 1, MAXIMUM, MAXIMUM, WIDTH, HEIGHT);

        update(scroll, camera, -1, 1, CENTER_X, CENTER_Y, true);

        Assertions.assertArrayEquals(new float[]{0, MAXIMUM}, camera.getPosition());

        WholePixelCamera narrow = new WholePixelCamera(0, 0, -1, MAXIMUM, WIDTH, HEIGHT);

        update(scroll, narrow, 1, 0, CENTER_X, CENTER_Y, true);

        Assertions.assertEquals(0, narrow.getX(), "a largest position below the smallest one leaves no room");
    }

    @Test
    void update_should_scroll_at_open_edges_and_wait_for_the_pointer_after_a_key_scroll() {
        CameraScroll scroll = new CameraScroll(EDGE_WIDTH, new SteppedScrollRate(STEPS));
        WholePixelCamera camera = new WholePixelCamera(START, START, MAXIMUM, MAXIMUM, WIDTH, HEIGHT);

        update(scroll, camera, 0, 0, 0, 0, true);

        Assertions.assertEquals(UP_LEFT, scroll.getEdgeDirection());
        Assertions.assertEquals(-1, scroll.getEdgeX());
        Assertions.assertEquals(-1, scroll.getEdgeY());
        Assertions.assertArrayEquals(new float[]{START - step(1), START - step(1)}, camera.getPosition());

        update(scroll, camera, 0, 0, WIDTH - 1, HEIGHT - 1, true);

        Assertions.assertEquals(DOWN_RIGHT, scroll.getEdgeDirection());

        update(scroll, camera, 1, 0, WIDTH - 1, HEIGHT - 1, true);

        float[] afterKeys = camera.getPosition();

        update(scroll, camera, 0, 0, WIDTH - 1, HEIGHT - 1, true);

        Assertions.assertArrayEquals(afterKeys, camera.getPosition(), "a pointer resting since the key scroll does not scroll");
        Assertions.assertEquals(CameraScroll.NO_DIRECTION, scroll.getEdgeDirection());

        update(scroll, camera, 0, 0, WIDTH - EDGE_WIDTH, CENTER_Y, true);

        Assertions.assertEquals(RIGHT, scroll.getEdgeDirection(), "a moved pointer scrolls again");
        Assertions.assertEquals(afterKeys[0] + step(1), camera.getX());

        update(scroll, camera, 0, 0, 0, 0, false);

        Assertions.assertEquals(RIGHT, scroll.getEdgeDirection(), "no edge test outside the scroll area");

        scroll.clearEdge();

        Assertions.assertEquals(CameraScroll.NO_DIRECTION, scroll.getEdgeDirection());

        WholePixelCamera corner = new WholePixelCamera(0, 0, MAXIMUM, MAXIMUM, WIDTH, HEIGHT);

        update(scroll, corner, 0, 0, 0, CENTER_Y, true);

        Assertions.assertEquals(CameraScroll.NO_DIRECTION, scroll.getEdgeDirection(), "no edge toward a side the camera cannot move");
        Assertions.assertArrayEquals(new float[]{0, 0}, corner.getPosition());
    }

    @Test
    void update_should_move_by_speed_and_time_with_a_continuous_rate() {
        Camera camera = new Camera(new SquareProjection(), MAP_SIZE, MAP_SIZE);
        ContinuousScrollRate rate = new ContinuousScrollRate();
        CameraScroll scroll = new CameraScroll(CONTINUOUS_EDGE_WIDTH, rate);

        camera.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        camera.centerOn(MAP_SIZE / 2f, MAP_SIZE / 2f);
        rate.setEdgeSpeed(SPEED);
        rate.setKeySpeed(SPEED);

        float x = camera.getX();
        float y = camera.getY();

        scroll.update(camera, 0, 0, SCREEN_WIDTH / 2f, SCREEN_HEIGHT / 2f, true, SCREEN_WIDTH, SCREEN_HEIGHT, MILLISECONDS);

        Assertions.assertEquals(x, camera.getX(), DELTA, "no edge, no keys");

        scroll.update(camera, 0, 0, SCREEN_WIDTH - 1, 0, true, SCREEN_WIDTH, SCREEN_HEIGHT, MILLISECONDS);

        Assertions.assertEquals(x + DISTANCE, camera.getX(), DELTA);
        Assertions.assertEquals(y - DISTANCE, camera.getY(), DELTA);

        camera.setZoom(2);

        x = camera.getX();
        y = camera.getY();

        scroll.update(camera, -1, 1, SCREEN_WIDTH - 1, 0, true, SCREEN_WIDTH, SCREEN_HEIGHT, MILLISECONDS);

        Assertions.assertEquals(x - DISTANCE / 2, camera.getX(), DELTA, "held keys before the edge, the speed in screen pixels");
        Assertions.assertEquals(y + DISTANCE / 2, camera.getY(), DELTA);
    }

    @Test
    void update_should_not_move_the_camera_by_turned_off_edge_or_key_scrolling() {
        CameraScroll scroll = new CameraScroll(EDGE_WIDTH, new SteppedScrollRate(STEPS));
        WholePixelCamera camera = new WholePixelCamera(START, START, MAXIMUM, MAXIMUM, WIDTH, HEIGHT);

        Assertions.assertTrue(scroll.isEdgeScrolling());
        Assertions.assertTrue(scroll.isKeyScrolling());

        scroll.setEdgeScrolling(false);
        scroll.setKeyScrolling(false);
        update(scroll, camera, 1, 1, 0, 0, true);

        Assertions.assertArrayEquals(new float[]{START, START}, camera.getPosition());
        Assertions.assertEquals(CameraScroll.NO_DIRECTION, scroll.getEdgeDirection());

        scroll.setKeyScrolling(true);
        update(scroll, camera, 1, 0, 0, 0, true);

        Assertions.assertEquals(START + step(1), camera.getX());
    }

    @Test
    void get_keyboard_direction_should_prefer_the_diagonal_keys() {
        Assertions.assertArrayEquals(new int[]{-1, -1}, CameraScroll.getKeyboardDirection(Set.of(Key.HOME, Key.RIGHT)::contains));
        Assertions.assertArrayEquals(new int[]{1, 1}, CameraScroll.getKeyboardDirection(Set.of(Key.PAGE_DOWN)::contains));
        Assertions.assertArrayEquals(new int[]{-1, 1}, CameraScroll.getKeyboardDirection(Set.of(Key.LEFT, Key.DOWN)::contains));
        Assertions.assertArrayEquals(new int[]{0, 0}, CameraScroll.getKeyboardDirection(Set.of()::contains));
    }

    @Test
    void camera_scroll_rejects_an_edge_width_that_is_not_positive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CameraScroll(0, new ContinuousScrollRate()));
    }
}

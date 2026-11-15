package org.fogofwarlabs.sc4j.engine.client.world;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests view jumps: centring snaps the top-left corner, the alert position is stored as the top-left corner without a snap (in the default
 * keeper or a game's own), camera slots jump back exactly, and each jump reads the camera when it is made.
 */
final class ViewJumpsTest {
    private static final int CAMERA_X = 100;
    private static final int CAMERA_Y = 200;
    private static final int VIEW_WIDTH = 640;
    /**
     * An odd view height; half of it is {@link #HALF_VIEW_HEIGHT}, rounded down.
     */
    private static final int VIEW_HEIGHT = 383;
    private static final int HALF_VIEW_WIDTH = 320;
    private static final int HALF_VIEW_HEIGHT = 191;
    private static final int TARGET = 1000;
    private static final int MAXIMUM = 4000;
    private static final int SLOT_COUNT = 3;
    private static final int SNAP = 8;
    private static final int SNAP_MASK = ~7;
    /**
     * An alert position whose x lies within half a view of the map's left edge.
     */
    private static final int ALERT_X = 100;
    private static final int SLOT = 1;
    private static final int GAME_ALERT_X = 64;
    private static final int GAME_ALERT_Y = 96;

    @Test
    void view_jumps_snap_center_and_store_alerts_as_top_left() {
        WholePixelCamera camera = new WholePixelCamera(CAMERA_X, CAMERA_Y, MAXIMUM, MAXIMUM, VIEW_WIDTH, VIEW_HEIGHT);
        ViewJumps jumps = new ViewJumps(camera, SLOT_COUNT, SNAP);

        Assertions.assertEquals(SLOT_COUNT, jumps.getSlotCount());

        jumps.startAlert();
        jumps.saveSlot(SLOT);
        camera.moveTo(0, 0);
        jumps.jumpToAlert();

        Assertions.assertArrayEquals(new float[]{CAMERA_X, CAMERA_Y}, camera.getPosition(), "the alert position starts at the camera position");

        jumps.centerOn(TARGET, TARGET);

        Assertions.assertArrayEquals(new float[]{(TARGET - HALF_VIEW_WIDTH) & SNAP_MASK, (TARGET - HALF_VIEW_HEIGHT) & SNAP_MASK},
                                     camera.getPosition());

        jumps.storeAlert(ALERT_X, TARGET);
        jumps.jumpToAlert();

        Assertions.assertEquals(0, camera.getX(), "only the lower bound is clamped");
        Assertions.assertEquals(TARGET - HALF_VIEW_HEIGHT, camera.getY(), "the alert position is not snapped");

        jumps.jumpToSlot(SLOT);

        Assertions.assertArrayEquals(new float[]{CAMERA_X, CAMERA_Y}, camera.getPosition(), "the slot holds the camera of its save");
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> jumps.jumpToSlot(SLOT_COUNT));
    }

    @Test
    void jump_to_alert_should_read_the_game_alert_position() {
        int[] alert = new int[2];
        WholePixelCamera camera = new WholePixelCamera(0, 0, MAXIMUM, MAXIMUM, VIEW_WIDTH, VIEW_HEIGHT);
        ViewJumps.AlertPosition position = new ViewJumps.AlertPosition() {
            @Override
            public int getAlertX() {
                return alert[0];
            }

            @Override
            public int getAlertY() {
                return alert[1];
            }

            @Override
            public void setAlert(int x, int y) {
                alert[0] = x;
                alert[1] = y;
            }
        };
        ViewJumps jumps = new ViewJumps(camera, position, SLOT_COUNT, SNAP);

        alert[0] = GAME_ALERT_X;
        alert[1] = GAME_ALERT_Y;
        jumps.jumpToAlert();

        Assertions.assertArrayEquals(new float[]{GAME_ALERT_X, GAME_ALERT_Y}, camera.getPosition(),
                                     "a change of the game's alert position is followed");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ViewJumps(camera, SLOT_COUNT, SNAP + 1));
    }
}

package org.fogofwarlabs.sc4j.engine.client.hud;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the minimap ping table: a ping's lifetime, a full table that drops new pings, and the reuse of the first free slot.
 */
final class MinimapPingsTest {
    private static final int SLOTS = 16;
    private static final int FRAMES = 20;
    private static final int PING_X = 320;
    private static final int PING_Y = 640;
    private static final int COLOR = 0x11;
    /**
     * The world x distance between the pings that fill the slot table.
     */
    private static final int SLOT_SPACING = 32;
    private static final int DROPPED_PING_X = 9999;
    private static final int REUSED_SLOT_X = 64;
    private static final int REUSED_SLOT_Y = 96;
    private static final int TINTING_PLAYER = 3;

    @Test
    void ping_shows_every_frame_then_frees_its_slot() {
        MinimapPings pings = new MinimapPings(SLOTS, FRAMES);

        pings.add(PING_X, PING_Y, COLOR, false);

        Assertions.assertEquals(0, pings.getFrame(0));
        Assertions.assertEquals(COLOR, pings.getColor(0));

        for (int tick = 1; tick < FRAMES; tick++) {
            Assertions.assertTrue(pings.advance());
            Assertions.assertEquals(tick, pings.getFrame(0));
        }

        // The tick on the last frame frees the slot and still reports activity, so that the minimap redraws once more.
        Assertions.assertTrue(pings.advance());
        Assertions.assertFalse(pings.isAnyActive());
        Assertions.assertFalse(pings.advance());
    }

    @Test
    void full_table_drops_a_ping_and_freed_slots_are_reused() {
        MinimapPings pings = new MinimapPings(SLOTS, FRAMES);

        for (int slot = 0; slot < SLOTS; slot++) {
            pings.add(slot * SLOT_SPACING, 0, COLOR, false);
        }

        pings.add(DROPPED_PING_X, 0, COLOR, false);

        for (int slot = 0; slot < SLOTS; slot++) {
            Assertions.assertEquals(slot * SLOT_SPACING, pings.getX(slot));
        }

        for (int tick = 0; tick < FRAMES; tick++) {
            pings.advance();
        }

        pings.add(REUSED_SLOT_X, REUSED_SLOT_Y, TINTING_PLAYER, true);

        Assertions.assertTrue(pings.isActive(0));
        Assertions.assertEquals(REUSED_SLOT_Y, pings.getY(0));
        Assertions.assertTrue(pings.isPlayerColor(0));
        Assertions.assertFalse(pings.isActive(1));

        pings.clear();

        Assertions.assertFalse(pings.isAnyActive());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MinimapPings(0, FRAMES));
    }
}

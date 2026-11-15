package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.image.ImageCanvas;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.text.TextStyle;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine default replay panel: it shows the game time reached and the replay's game time with the speed, a press on its progress bar
 * seeks to the pointed share of the replay, and its buttons pause and play, slow down, speed up and follow the next player, whose name the last
 * button shows.
 */
final class ReplayPanelTest {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 120;
    private static final int LINE_HEIGHT = 10;
    private static final int CHARACTER_WIDTH = 6;
    private static final float PANEL_LEFT = 20;
    private static final float PANEL_TOP = 10;
    private static final float PANEL_WIDTH = 300;
    private static final float PANEL_HEIGHT = 90;
    private static final long LOOP_MILLISECONDS = 42;
    private static final long LOOP_COUNT = 5667;
    private static final long LOOP = 1500;
    /**
     * The padding of the panel around its line, bar and buttons.
     */
    private static final float PADDING = 6;
    private static final float HALF_DIVISOR = 2;
    /**
     * The distance of a pressed point from the top left corner of a button.
     */
    private static final float BUTTON_INSET = 2;

    /**
     * Keeps every drawn line and measures each character as one width.
     */
    private static final class RecordingFont implements Font {
        private final @NotNull List<@NotNull String> lines;

        private RecordingFont() {
            this.lines = new ArrayList<>();
        }

        @Override
        public int getLineHeight() {
            return LINE_HEIGHT;
        }

        @Override
        public @NotNull TextStyle getDefaultStyle() {
            return TextStyle.DEFAULT;
        }

        @Override
        public float measure(@NotNull CharSequence text, @NotNull TextStyle style) {
            return text.length() * CHARACTER_WIDTH;
        }

        @Override
        public float draw(@NotNull Canvas canvas, @NotNull CharSequence text, float x, float y, int color, @NotNull TextStyle style) {
            this.lines.add(text.toString());

            return x + this.measure(text, style);
        }
    }

    /**
     * Records what the panel asks of the playback.
     */
    private static final class RecordingPlayback implements ReplayPanel.Playback {
        private final @NotNull List<@NotNull String> calls;
        private long loop;
        private int speed;
        private boolean paused;

        private RecordingPlayback() {
            this.calls  = new ArrayList<>();
            this.loop   = LOOP;
            this.speed  = 1;
            this.paused = false;
        }

        @Override
        public long getLoop() {
            return this.loop;
        }

        @Override
        public long getLoopCount() {
            return LOOP_COUNT;
        }

        @Override
        public int getSpeed() {
            return this.speed;
        }

        @Override
        public boolean isPaused() {
            return this.paused;
        }

        @Override
        public void setPaused(boolean paused) {
            this.paused = paused;
            this.calls.add(paused ? "pause" : "play");
        }

        @Override
        public void increaseSpeed() {
            this.speed *= 2;
            this.calls.add("faster");
        }

        @Override
        public void decreaseSpeed() {
            this.calls.add("slower");
        }

        @Override
        public void seek(long loop) {
            this.loop = loop;
            this.calls.add("seek");
        }

        @Override
        public @NotNull String getViewedPlayerName() {
            return "Dawnhold";
        }

        @Override
        public void viewNextPlayer() {
            this.calls.add("next player");
        }
    }

    private static void press(@NotNull ReplayPanel panel, float x, float y) {
        Assertions.assertTrue(panel.handleInput(InputEvent.createButtonPressed(PointerButton.LEFT, 0, x, y, false)), "the panel takes the press");
    }

    private static float @NotNull [] findButtonCenter(@NotNull ReplayPanel panel, int button) {
        for (float y = panel.getY(); y < panel.getY() + panel.getHeight(); y++) {
            for (float x = panel.getX(); x < panel.getX() + panel.getWidth(); x++) {
                if (panel.findButtonAt(x, y) == button) {
                    return new float[]{x + BUTTON_INSET, y + BUTTON_INSET};
                }
            }
        }

        return Assertions.fail("the panel shows button " + button);
    }

    @Test
    void replay_panel_shows_the_game_time_and_seeks_pauses_changes_speed_and_follows_the_next_player() {
        RecordingFont font = new RecordingFont();
        RecordingPlayback playback = new RecordingPlayback();
        ReplayPanel panel = new ReplayPanel(playback, font, LOOP_MILLISECONDS);
        ImageCanvas canvas = new ImageCanvas(WIDTH, HEIGHT);

        panel.setBounds(PANEL_LEFT, PANEL_TOP, PANEL_WIDTH, PANEL_HEIGHT);
        canvas.begin(WIDTH, HEIGHT);
        panel.draw(canvas);
        canvas.end();

        Assertions.assertEquals(List.of("1:03 / 3:58", "x1", ReplayPanel.PAUSE_LABEL, ReplayPanel.SLOWER_LABEL, ReplayPanel.FASTER_LABEL, "Dawnhold"),
                                font.lines);

        long middle = panel.findLoopAt(PANEL_LEFT + PANEL_WIDTH / HALF_DIVISOR, PANEL_TOP + PADDING + LINE_HEIGHT + PADDING);

        Assertions.assertEquals(LOOP_COUNT / HALF_DIVISOR, middle, 1, "the middle of the bar is the middle of the replay");

        press(panel, PANEL_LEFT + PANEL_WIDTH / HALF_DIVISOR, PANEL_TOP + PADDING + LINE_HEIGHT + PADDING);

        Assertions.assertEquals(middle, playback.getLoop());

        for (int button = 0; button < ReplayPanel.BUTTON_COUNT; button++) {
            float[] center = findButtonCenter(panel, button);

            press(panel, center[0], center[1]);
        }

        Assertions.assertEquals(List.of("seek", "pause", "slower", "faster", "next player"), playback.calls);

        font.lines.clear();
        canvas.begin(WIDTH, HEIGHT);
        panel.draw(canvas);
        canvas.end();

        Assertions.assertEquals("Paused", font.lines.get(1), "a paused playback says so instead of its speed");
        Assertions.assertEquals(ReplayPanel.PLAY_LABEL, font.lines.get(2), "the pause button plays a paused playback");
    }
}

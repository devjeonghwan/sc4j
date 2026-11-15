package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.game.GameTimeUtils;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.skin.HudSkin;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.world.StatusBarUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Shows the playback of a replay in a HUD rectangle and controls it: the engine default replay panel, which a replay viewer hosts in the
 * console's queue region ({@link org.fogofwarlabs.sc4j.engine.client.game.GameScreen#setQueuePanel}). Its top line shows the game time
 * reached and the replay's game time ({@link GameTimeUtils}) at the left and the speed (or that the playback is paused or at its end) at the
 * right; below it a bar shows how far the playback is, and a left press on the bar seeks there. A row of buttons below pauses or plays,
 * slows down ({@code -} on a narrow panel), speeds up ({@code +}) and follows the next player, whose name the last button shows. The mod's
 * {@link Playback} does what the panel asks.
 * <p>
 * The panel draws no box of its own: its host draws the skin's queue panel box under it. The buttons draw in the skin's command button boxes.
 */
public final class ReplayPanel extends HudBlock {
    /**
     * The label of the pause button while the playback plays.
     */
    public static final @NotNull String PAUSE_LABEL = "Pause";
    /**
     * The label of the pause button while the playback is paused.
     */
    public static final @NotNull String PLAY_LABEL = "Play";
    /**
     * The label of the slower button.
     */
    public static final @NotNull String SLOWER_LABEL = "Slower";
    /**
     * The label of the faster button.
     */
    public static final @NotNull String FASTER_LABEL = "Faster";
    /**
     * The label of the slower button when {@link #SLOWER_LABEL} does not fit it.
     */
    public static final @NotNull String SLOWER_SHORT_LABEL = "-";
    /**
     * The label of the faster button when {@link #FASTER_LABEL} does not fit it.
     */
    public static final @NotNull String FASTER_SHORT_LABEL = "+";
    /**
     * The number of buttons in the panel's row: pause, slower, faster and the player.
     */
    public static final int BUTTON_COUNT = 4;
    /**
     * The index of the pause button in the row.
     */
    public static final int PAUSE_BUTTON = 0;
    /**
     * The index of the slower button in the row.
     */
    public static final int SLOWER_BUTTON = 1;
    /**
     * The index of the faster button in the row.
     */
    public static final int FASTER_BUTTON = 2;
    /**
     * The index of the player button in the row.
     */
    public static final int PLAYER_BUTTON = 3;
    /**
     * The value of a position that is no button.
     */
    public static final int NO_BUTTON = -1;

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int STATUS_COLOR = 0xC8D2DCFF;
    private static final int PROGRESS_COLOR = 0xE0B040FF;
    private static final float PADDING = 6;
    private static final float GAP = 4;
    private static final float BAR_HEIGHT = 8;
    private static final float BUTTON_MARGIN = 4;
    private static final float HALF_DIVISOR = 2;
    private static final @NotNull String TIME_SEPARATOR = " / ";
    private static final @NotNull String SPEED_PREFIX = "x";
    private static final @NotNull String PAUSED = "Paused";
    private static final @NotNull String END = "End";

    /**
     * Plays a replay back for a {@link ReplayPanel}.
     */
    public interface Playback {
        /**
         * @return the game loop the playback reached
         */
        long getLoop();

        /**
         * @return the game loops of the replay
         */
        long getLoopCount();

        /**
         * @return how many game loops play in the time of one at normal speed
         */
        int getSpeed();

        /**
         * @return {@code true} if the playback is paused, {@code false} otherwise
         */
        boolean isPaused();

        /**
         * Pauses or resumes the playback.
         *
         * @param paused whether the playback pauses
         */
        void setPaused(boolean paused);

        /**
         * Plays faster, up to the highest speed.
         */
        void increaseSpeed();

        /**
         * Plays slower, down to normal speed.
         */
        void decreaseSpeed();

        /**
         * Seeks the playback to a game loop.
         *
         * @param loop the game loop, within the replay
         */
        void seek(long loop);

        /**
         * @return the shown name of the player the viewer follows
         */
        @NotNull String getViewedPlayerName();

        /**
         * Follows the next player.
         */
        void viewNextPlayer();
    }


    private final @NotNull Playback playback;
    private final @NotNull Font font;
    private final long loopMilliseconds;
    private final @NotNull StringBuilder text;

    private float pointerX;
    private float pointerY;

    /**
     * Creates a {@link ReplayPanel} instance.
     *
     * @param playback         the playback it shows and controls
     * @param font             the font of its texts
     * @param loopMilliseconds the client time of one game loop at normal speed in milliseconds, for the game time
     */
    public ReplayPanel(@NotNull Playback playback, @NotNull Font font, long loopMilliseconds) {
        this.playback         = playback;
        this.font             = font;
        this.loopMilliseconds = loopMilliseconds;
        this.text             = new StringBuilder();

        this.pointerX = -1;
        this.pointerY = -1;
    }

    /**
     * Finds the button at a logical position.
     *
     * @param x the logical column
     * @param y the logical row
     * @return the button's index in the row ({@link #PAUSE_BUTTON} to {@link #PLAYER_BUTTON}), or {@link #NO_BUTTON}
     */
    public int findButtonAt(float x, float y) {
        float top = this.getButtonTop();

        if (!this.contains(x, y) || y < top || y >= top + this.getButtonHeight()) {
            return NO_BUTTON;
        }

        float width = this.getButtonWidth();

        for (int index = 0; index < BUTTON_COUNT; index++) {
            float left = this.getX() + PADDING + index * (width + GAP);

            if (x >= left && x < left + width) {
                return index;
            }
        }

        return NO_BUTTON;
    }

    /**
     * Finds the game loop under a logical position of the progress bar.
     *
     * @param x the logical column
     * @param y the logical row
     * @return the game loop, or {@code -1} if the position is not on the bar
     */
    public long findLoopAt(float x, float y) {
        float left = this.getX() + PADDING;
        float width = Math.max(this.getWidth() - 2 * PADDING, 1);
        float top = this.getBarTop();

        if (x < left || x >= left + width || y < top - GAP || y >= top + BAR_HEIGHT + GAP) {
            return -1;
        }

        return Math.clamp(Math.round((x - left) / width * this.playback.getLoopCount()), 0, this.playback.getLoopCount());
    }

    /**
     * Draws the block: the time line, the progress bar and the button row.
     *
     * @param canvas the canvas, between its begin and end
     */
    @Override
    public void draw(@NotNull Canvas canvas) {
        HudSkin skin = this.getSkin();
        long loop = this.playback.getLoop();
        long loopCount = this.playback.getLoopCount();
        float left = this.getX() + PADDING;
        float right = this.getX() + this.getWidth() - PADDING;
        float top = this.getY() + PADDING;

        canvas.setClip((int) this.getX(), (int) this.getY(), (int) Math.ceil(this.getWidth()), (int) Math.ceil(this.getHeight()));
        this.text.setLength(0);
        this.text.append(GameTimeUtils.format(loop, this.loopMilliseconds)).append(TIME_SEPARATOR)
                 .append(GameTimeUtils.format(loopCount, this.loopMilliseconds));
        this.font.draw(canvas, this.text, left, top, TEXT_COLOR);

        this.text.setLength(0);

        if (this.playback.isPaused()) {
            this.text.append(PAUSED);
        } else if (loop >= loopCount) {
            this.text.append(END);
        } else {
            this.text.append(SPEED_PREFIX).append(this.playback.getSpeed());
        }

        this.font.draw(canvas, this.text, right - this.font.measure(this.text), top, STATUS_COLOR);
        StatusBarUtils.drawBar(canvas, left, this.getBarTop(), right - left, BAR_HEIGHT,
                               (int) Math.min(loop, Integer.MAX_VALUE), (int) Math.min(Math.max(loopCount, 1), Integer.MAX_VALUE),
                               PROGRESS_COLOR, StatusBarUtils.EMPTY_COLOR);

        float buttonTop = this.getButtonTop();
        float buttonWidth = this.getButtonWidth();
        float buttonHeight = this.getButtonHeight();
        int hovered = this.findButtonAt(this.pointerX, this.pointerY);

        for (int index = 0; index < BUTTON_COUNT; index++) {
            float buttonLeft = left + index * (buttonWidth + GAP);
            String label = switch (index) {
                case PAUSE_BUTTON -> this.playback.isPaused() ? PLAY_LABEL : PAUSE_LABEL;
                case SLOWER_BUTTON -> this.font.measure(SLOWER_LABEL) + 2 * BUTTON_MARGIN <= buttonWidth ? SLOWER_LABEL : SLOWER_SHORT_LABEL;
                case FASTER_BUTTON -> this.font.measure(FASTER_LABEL) + 2 * BUTTON_MARGIN <= buttonWidth ? FASTER_LABEL : FASTER_SHORT_LABEL;
                default -> this.playback.getViewedPlayerName();
            };

            skin.getCommandButton().findStyle(true, false, hovered == index).draw(canvas, buttonLeft, buttonTop, buttonWidth, buttonHeight);
            canvas.setClip((int) buttonLeft, (int) buttonTop, (int) Math.ceil(buttonWidth), (int) Math.ceil(buttonHeight));
            this.font.draw(canvas, label, buttonLeft + Math.max((buttonWidth - this.font.measure(label)) / HALF_DIVISOR, BUTTON_MARGIN),
                           buttonTop + (buttonHeight - this.font.getLineHeight()) / HALF_DIVISOR, TEXT_COLOR);
            canvas.setClip((int) this.getX(), (int) this.getY(), (int) Math.ceil(this.getWidth()), (int) Math.ceil(this.getHeight()));
        }

        canvas.setClip(0, 0, -1, -1);
    }

    /**
     * Handles an input event: a left press on the bar seeks there, on a button runs it; other presses inside are taken. A pointer move is
     * remembered for the hovered button and not taken.
     *
     * @param event the event
     * @return {@code true} if the panel took the event, {@code false} otherwise
     */
    @Override
    public boolean handleInput(@NotNull InputEvent event) {
        if (event.getType() == InputEvent.Type.POINTER_MOVED) {
            this.pointerX = event.getX();
            this.pointerY = event.getY();

            return false;
        }

        if (event.getType() != InputEvent.Type.BUTTON_PRESSED || event.getButton() != PointerButton.LEFT ||
            !this.contains(event.getX(), event.getY())) {
            return super.handleInput(event);
        }

        long loop = this.findLoopAt(event.getX(), event.getY());

        if (loop >= 0) {
            this.playback.seek(loop);

            return true;
        }

        switch (this.findButtonAt(event.getX(), event.getY())) {
            case PAUSE_BUTTON -> this.playback.setPaused(!this.playback.isPaused());
            case SLOWER_BUTTON -> this.playback.decreaseSpeed();
            case FASTER_BUTTON -> this.playback.increaseSpeed();
            case PLAYER_BUTTON -> this.playback.viewNextPlayer();
            default -> {
                // A press beside the bar and the buttons does nothing.
            }
        }

        return true;
    }

    private float getBarTop() {
        return this.getY() + PADDING + this.font.getLineHeight() + GAP;
    }

    private float getButtonTop() {
        return this.getBarTop() + BAR_HEIGHT + PADDING;
    }

    private float getButtonHeight() {
        return Math.max(Math.min(this.font.getLineHeight() + 2 * BUTTON_MARGIN, this.getY() + this.getHeight() - PADDING - this.getButtonTop()), 0);
    }

    private float getButtonWidth() {
        return Math.max((this.getWidth() - 2 * PADDING - (BUTTON_COUNT - 1) * GAP) / BUTTON_COUNT, 0);
    }
}

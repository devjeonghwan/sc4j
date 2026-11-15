package org.fogofwarlabs.sc4j.engine.client.console;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Shows the engine default console of a client: a line the player types into and the latest message lines. While the console is open it takes
 * typed characters, Backspace, Enter (submits the line and closes) and Escape (drops the line and closes); a submitted command line
 * ({@link ConsoleCommandRegistry#isCommand}) runs in the registry and its paged reply joins the message lines, any other line goes to the chat
 * handler.
 * <p>
 * With line times set ({@link #setLineTimes}) a message line shows fully for its shown time after it was added, then fades out over its fade
 * time and is no longer drawn (it stays in {@link #getLines}); the time passes with {@link #update}. While the console is open every kept line
 * shows fully. Without line times the lines never fade. Each drawn line stands on a translucent dark backing as wide as its text, which fades
 * with it, so it reads over bright ground.
 */
public final class Console {
    /**
     * The text drawn before the typed line.
     */
    public static final @NotNull String PROMPT = "> ";

    private static final int ALPHA_MASK = 0xFF;
    private static final int BACKING_COLOR = 0x00000000;
    private static final int BACKING_ALPHA = 0x90;
    private static final float BACKING_PADDING = 3;
    /**
     * The largest age of a message line in milliseconds, so that adding the time of an update never overflows.
     */
    private static final long MAXIMUM_LINE_AGE = Long.MAX_VALUE / 2;

    private final @NotNull ConsoleCommandRegistry registry;
    private final @NotNull Consumer<String> chat;
    private final int maximumLines;

    private final @NotNull Deque<String> lines;
    private final long @NotNull [] ages;
    private final @NotNull StringBuilder input;
    private boolean open;

    private long shownMilliseconds;
    private long fadeMilliseconds;

    /**
     * Creates a closed {@link Console} instance without message lines.
     *
     * @param registry     the commands
     * @param chat         the receiver of a submitted line that is no command
     * @param maximumLines the most message lines the console keeps; older lines drop out
     * @throws IllegalArgumentException if the console keeps no line
     */
    public Console(@NotNull ConsoleCommandRegistry registry, @NotNull Consumer<String> chat, int maximumLines) {
        if (maximumLines < 1) {
            throw Exceptions.newIllegalArgumentException("Invalid console line count: {0}", maximumLines);
        }

        this.registry     = registry;
        this.chat         = chat;
        this.maximumLines = maximumLines;

        this.lines = new ArrayDeque<>(maximumLines);
        this.ages  = new long[maximumLines];
        this.input = new StringBuilder();
        this.open  = false;

        this.shownMilliseconds = 0;
        this.fadeMilliseconds  = 0;
    }

    /**
     * @return the commands
     */
    public @NotNull ConsoleCommandRegistry getRegistry() {
        return this.registry;
    }

    /**
     * @return the message lines, oldest first
     */
    public @NotNull List<String> getLines() {
        return new ArrayList<>(this.lines);
    }

    /**
     * @return the typed line
     */
    public @NotNull String getInput() {
        return this.input.toString();
    }

    /**
     * @return {@code true} if the console takes typed input, {@code false} otherwise
     */
    public boolean isOpen() {
        return this.open;
    }

    /**
     * Sets how long a message line shows: fully for a time after it was added, then fading out over another; a shown time of {@code 0} keeps
     * the lines shown.
     *
     * @param shownMilliseconds the time a line shows fully, {@code 0} for always
     * @param fadeMilliseconds  the time a line then fades out over
     * @throws IllegalArgumentException if a time is negative
     */
    public void setLineTimes(long shownMilliseconds, long fadeMilliseconds) {
        if (shownMilliseconds < 0 || fadeMilliseconds < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid console line times {0} and {1}", shownMilliseconds, fadeMilliseconds);
        }

        this.shownMilliseconds = shownMilliseconds;
        this.fadeMilliseconds  = fadeMilliseconds;
    }


    /**
     * Opens the console with an empty line.
     */
    public void open() {
        this.input.setLength(0);
        this.open = true;
    }

    /**
     * Closes the console and drops the typed line.
     */
    public void close() {
        this.input.setLength(0);
        this.open = false;
    }

    /**
     * Adds a message line, dropping the oldest when the console holds its most lines.
     *
     * @param line the line
     */
    public void addLine(@NotNull String line) {
        if (this.lines.size() >= this.maximumLines) {
            this.lines.removeFirst();
            System.arraycopy(this.ages, 1, this.ages, 0, this.ages.length - 1);
        }

        this.ages[this.lines.size()] = 0;
        this.lines.addLast(line);
    }

    /**
     * Lets time pass for the message lines' fading.
     *
     * @param milliseconds the time since the last update
     */
    public void update(long milliseconds) {
        for (int index = 0; index < this.lines.size(); index++) {
            this.ages[index] = Math.min(this.ages[index] + Math.max(milliseconds, 0), MAXIMUM_LINE_AGE);
        }
    }

    /**
     * Submits a line: a command line runs ({@link ConsoleCommandRegistry#runPaged}) and its reply joins the message lines, any other line goes to
     * the chat handler.
     *
     * @param line the line
     */
    public void submit(@NotNull String line) {
        if (!ConsoleCommandRegistry.isCommand(line)) {
            this.chat.accept(line);

            return;
        }

        for (String reply : this.registry.runPaged(line)) {
            this.addLine(reply);
        }
    }

    /**
     * Handles an input event while the console is open: a typed character joins the line, Backspace removes the last character, Enter submits a
     * line that is not blank and closes, Escape closes.
     *
     * @param event the event
     * @return {@code true} if the console is open and used the event, {@code false} otherwise
     */
    public boolean handleInput(@NotNull InputEvent event) {
        if (!this.open) {
            return false;
        }

        if (event.getType() == InputEvent.Type.CHARACTER_TYPED) {
            this.input.appendCodePoint(event.getCodePoint());

            return true;
        }

        if (!event.isKeyDown()) {
            return false;
        }

        switch (event.getKey()) {
            case BACKSPACE -> {
                if (!this.input.isEmpty()) {
                    this.input.setLength(this.input.offsetByCodePoints(this.input.length(), -1));
                }
            }
            case ENTER, KEYPAD_ENTER -> {
                String line = this.input.toString();

                this.close();

                if (!line.isBlank()) {
                    this.submit(line);
                }
            }
            case ESCAPE -> this.close();
            default -> {
                return false;
            }
        }

        return true;
    }

    /**
     * Draws the message lines that still show from the top, fading ones in less alpha, then the prompt and the typed line below them while
     * the console is open; each line on its backing.
     *
     * @param canvas the canvas, between its {@link Canvas#begin} and {@link Canvas#end}
     * @param font   the font
     * @param left   the left of the lines
     * @param top    the top of the first line
     * @param color  the packed {@code 0xRRGGBBAA} text colour
     */
    public void draw(@NotNull Canvas canvas, @NotNull Font font, float left, float top, int color) {
        float y = top;
        int index = 0;

        for (String line : this.lines) {
            int alpha = this.findAlpha(this.ages[index++], color & ALPHA_MASK);

            if (alpha > 0) {
                drawBacking(canvas, font, line, left, y, alpha * BACKING_ALPHA / ALPHA_MASK);
                font.draw(canvas, line, left, y, color & ~ALPHA_MASK | alpha);
                y += font.getLineHeight();
            }
        }

        if (this.open) {
            String typed = PROMPT + this.input;

            drawBacking(canvas, font, typed, left, y, BACKING_ALPHA);
            font.draw(canvas, typed, left, y, color);
        }
    }

    private static void drawBacking(@NotNull Canvas canvas, @NotNull Font font, @NotNull String line, float left, float top, int alpha) {
        canvas.fillRectangle(left - BACKING_PADDING, top, font.measure(line) + 2 * BACKING_PADDING, font.getLineHeight(), BACKING_COLOR | alpha);
    }

    /**
     * Finds the alpha of a line of an age: the full alpha while the console is open, without line times or within the shown time, then
     * falling to {@code 0} over the fade time.
     */
    private int findAlpha(long age, int fullAlpha) {
        if (this.open || this.shownMilliseconds == 0 || age < this.shownMilliseconds) {
            return fullAlpha;
        }

        long faded = age - this.shownMilliseconds;

        if (faded >= this.fadeMilliseconds) {
            return 0;
        }

        return (int) (fullAlpha * (this.fadeMilliseconds - faded) / this.fadeMilliseconds);
    }
}

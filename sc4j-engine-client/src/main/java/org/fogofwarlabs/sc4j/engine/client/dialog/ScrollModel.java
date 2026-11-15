package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.jetbrains.annotations.NotNull;

/**
 * Holds the value of a scroll bar or a slider between a minimum and a maximum and the press that holds one of its parts
 * ({@link ScrollPart}): the step parts move the value by one, the track before and after the thumb by a page, and a drag of the thumb
 * maps the pointer onto the value. A control draws the bar in its own look and gives the model its lengths in whole pixels; the model
 * holds no look.
 * <p>
 * A press steps at once. While the part stays held, {@link #repeat} (one tick of the owner's timer) steps again: a step part after
 * {@value #STEP_REPEAT_TICKS} tick, the track after {@value #PAGE_REPEAT_TICKS} ticks, and only while the pointer is over the held part
 * ({@link #movePointer}). An owner without a timer gets one step per press.
 */
public final class ScrollModel {
    /**
     * The ticks between the repeated steps of a held step part.
     */
    public static final int STEP_REPEAT_TICKS = 1;
    /**
     * The ticks between the repeated pages of a held track part.
     */
    public static final int PAGE_REPEAT_TICKS = 3;

    private int minimum;
    private int maximum;
    private int value;
    private int page;

    private @NotNull ScrollPart heldPart;
    private boolean pointerOver;
    private boolean dragging;
    private int dragOffset;
    private int countdown;

    /**
     * Creates a {@link ScrollModel} instance over {@code 0} to {@code 0} with a page of {@code 1}.
     */
    public ScrollModel() {
        this.minimum = 0;
        this.maximum = 0;
        this.value   = 0;
        this.page    = 1;

        this.heldPart    = ScrollPart.NONE;
        this.pointerOver = false;
        this.dragging    = false;
        this.dragOffset  = 0;
        this.countdown   = 0;
    }

    /**
     * @return the smallest value
     */
    public int getMinimum() {
        return this.minimum;
    }

    /**
     * @return the largest value
     */
    public int getMaximum() {
        return this.maximum;
    }

    /**
     * Sets the range and clamps the value into it.
     *
     * @param minimum the smallest value
     * @param maximum the largest value; a value below the minimum is the minimum
     */
    public void setRange(int minimum, int maximum) {
        this.minimum = minimum;
        this.maximum = Math.max(minimum, maximum);
        this.value   = Math.clamp(this.value, this.minimum, this.maximum);
    }

    /**
     * @return the value
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Sets the value.
     *
     * @param value the value, clamped to the range
     */
    public void setValue(int value) {
        this.value = Math.clamp(value, this.minimum, this.maximum);
    }

    /**
     * @return the step of a press on the track
     */
    public int getPage() {
        return this.page;
    }

    /**
     * Sets the step of a press on the track.
     *
     * @param page the step, usually the shown part of the scrolled content
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * @return the part a press holds, or {@link ScrollPart#NONE}
     */
    public @NotNull ScrollPart getHeldPart() {
        return this.heldPart;
    }

    /**
     * @param part a part
     * @return {@code true} if a press holds the part and the pointer is over it, {@code false} otherwise
     */
    public boolean isPartPressed(@NotNull ScrollPart part) {
        return this.heldPart == part && this.pointerOver;
    }

    /**
     * @return {@code true} if a press holds the thumb, {@code false} otherwise
     */
    public boolean isDragging() {
        return this.dragging;
    }

    /**
     * Returns the thumb's offset from the start of its travel: while it is dragged, where the pointer put it; else the value's place,
     * rounded to the nearest pixel, at the start for the minimum and at the end for the maximum.
     *
     * @param travel the thumb's travel: the track length less the thumb length
     * @return the offset in pixels
     */
    public int getThumbOffset(int travel) {
        if (this.dragging) {
            return this.dragOffset;
        }

        int range = this.maximum - this.minimum;

        if (range == 0 || this.value == this.minimum) {
            return 0;
        }

        if (this.value == this.maximum) {
            return travel;
        }

        return ((this.value - this.minimum) * travel + range / 2) / range;
    }


    /**
     * Finds the part at an offset along the bar.
     *
     * @param offset           the pointer's offset from the bar's start
     * @param stepBackEnd      the last offset of the step part before the track
     * @param stepForwardStart the first offset of the step part after the track
     * @param thumbStart       the offset of the thumb's start, usually the track start plus {@link #getThumbOffset}
     * @param thumbLength      the thumb's length
     * @return the part, never {@link ScrollPart#NONE}: a caller tests first whether the pointer is on the bar
     */
    public @NotNull ScrollPart findPart(float offset, float stepBackEnd, float stepForwardStart, float thumbStart, float thumbLength) {
        if (offset <= stepBackEnd) {
            return ScrollPart.STEP_BACK;
        }

        if (offset >= stepForwardStart) {
            return ScrollPart.STEP_FORWARD;
        }

        if (offset < thumbStart) {
            return ScrollPart.PAGE_BACK;
        }

        return offset < thumbStart + thumbLength ? ScrollPart.THUMB : ScrollPart.PAGE_FORWARD;
    }

    /**
     * Presses a part, with the pointer over it: a step or track part moves the value at once; the thumb starts a drag ({@link #dragTo}).
     *
     * @param part the pressed part
     * @return {@code true} if the value changed, {@code false} otherwise
     */
    public boolean press(@NotNull ScrollPart part) {
        int before = this.value;

        this.heldPart    = part;
        this.pointerOver = true;
        this.dragging    = part == ScrollPart.THUMB;
        this.countdown   = STEP_REPEAT_TICKS;

        if (!this.dragging) {
            this.step();
        }

        return this.value != before;
    }

    /**
     * Repeats the held step or track part for one timer tick; nothing while nothing or the thumb is held.
     *
     * @return {@code true} if the value changed, {@code false} otherwise
     */
    public boolean repeat() {
        if (this.heldPart == ScrollPart.NONE || this.dragging) {
            return false;
        }

        int before = this.value;

        this.step();

        return this.value != before;
    }

    private void step() {
        if (--this.countdown > 0) {
            return;
        }

        boolean stepPart = this.heldPart == ScrollPart.STEP_BACK || this.heldPart == ScrollPart.STEP_FORWARD;

        this.countdown = stepPart ? STEP_REPEAT_TICKS : PAGE_REPEAT_TICKS;

        if (!this.pointerOver) {
            return;
        }

        int delta = switch (this.heldPart) {
            case STEP_BACK -> -1;
            case STEP_FORWARD -> 1;
            case PAGE_BACK -> -this.page;
            case PAGE_FORWARD -> this.page;
            case NONE, THUMB -> 0;
        };

        this.setValue(this.value + delta);
    }

    /**
     * Moves the pointer while a step or track part is held: the part repeats only while the pointer is over it.
     *
     * @param part the part under the pointer, or {@link ScrollPart#NONE} off the bar
     */
    public void movePointer(@NotNull ScrollPart part) {
        if (this.heldPart != ScrollPart.NONE && !this.dragging) {
            this.pointerOver = part == this.heldPart;
        }
    }

    /**
     * Drags the thumb: its start follows the pointer less half the thumb, from the track start, with the pointer clamped to a limit; the
     * value is the thumb's share of the travel over one more than the range, so the maximum takes the last share. Nothing while the thumb
     * is not held.
     *
     * @param pointer      the pointer's offset from the bar's start
     * @param trackStart   the offset of the track's start
     * @param pointerLimit the largest pointer offset the thumb follows
     * @param thumbLength  the thumb's length
     * @param travel       the thumb's travel
     * @return {@code true} if the value changed, {@code false} otherwise
     */
    public boolean dragTo(float pointer, int trackStart, int pointerLimit, int thumbLength, int travel) {
        if (!this.dragging) {
            return false;
        }

        int before = this.value;
        int offset = Math.min((int) Math.floor(pointer), pointerLimit) - thumbLength / 2 - trackStart;

        this.dragOffset = Math.max(0, offset);

        if (travel > 0) {
            this.setValue(this.minimum + (this.maximum - this.minimum + 1) * this.dragOffset / travel);
        }

        return this.value != before;
    }

    /**
     * Releases the held part.
     */
    public void release() {
        this.heldPart    = ScrollPart.NONE;
        this.pointerOver = false;
        this.dragging    = false;
    }
}

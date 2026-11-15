package org.fogofwarlabs.sc4j.engine.client.dialog;

/**
 * Names the parts of a scroll bar or a slider that a press can hold ({@link ScrollModel}).
 */
public enum ScrollPart {
    /**
     * No part: the pointer is off the bar, or nothing is held.
     */
    NONE,
    /**
     * The arrow or end before the track: steps the value back by one.
     */
    STEP_BACK,
    /**
     * The arrow or end after the track: steps the value forward by one.
     */
    STEP_FORWARD,
    /**
     * The track before the thumb: moves the value back by a page.
     */
    PAGE_BACK,
    /**
     * The track after the thumb: moves the value forward by a page.
     */
    PAGE_FORWARD,
    /**
     * The thumb: a drag moves it and sets the value.
     */
    THUMB
}

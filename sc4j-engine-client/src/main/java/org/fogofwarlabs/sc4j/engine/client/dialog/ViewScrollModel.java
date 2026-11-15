package org.fogofwarlabs.sc4j.engine.client.dialog;

/**
 * Scrolls a view over taller content in pixels, as a chat log or a list of rows of any height does: the offset stays between the top
 * and the end of the content, and a view at the end follows the end while the content grows, until it scrolls away. A control draws the
 * bar in its own look and gives the model its track length; the thumb's length is the view's share of the track.
 */
public final class ViewScrollModel {
    private float content;
    private float view;
    private float offset;
    private boolean followingEnd;

    /**
     * Creates a {@link ViewScrollModel} instance without content that follows the end.
     */
    public ViewScrollModel() {
        this.content      = 0;
        this.view         = 0;
        this.offset       = 0;
        this.followingEnd = true;
    }

    /**
     * @return the content's height
     */
    public float getContent() {
        return this.content;
    }

    /**
     * @return the view's height
     */
    public float getView() {
        return this.view;
    }

    /**
     * Sets the content and view heights; the offset stays inside them, and at the end while it follows the end.
     *
     * @param content the content's height
     * @param view    the view's height
     */
    public void setContent(float content, float view) {
        this.content = content;
        this.view    = view;

        this.setOffset(this.followingEnd ? Float.MAX_VALUE : this.offset);
    }

    /**
     * @return how far the content is scrolled up
     */
    public float getOffset() {
        return this.offset;
    }

    /**
     * Scrolls to an offset; the view follows the end while the offset is at the end.
     *
     * @param offset the offset, clamped between {@code 0} and the content less the view
     */
    public void setOffset(float offset) {
        float maximum = Math.max(0, this.content - this.view);

        this.offset       = Math.clamp(offset, 0, maximum);
        this.followingEnd = this.offset >= maximum;
    }

    /**
     * @return {@code true} if the content is taller than the view, {@code false} otherwise
     */
    public boolean isScrollable() {
        return this.content > this.view;
    }

    /**
     * @param trackLength   the track's length
     * @param minimumLength the shortest thumb
     * @return the thumb's length: the view's share of the track, at least the minimum
     */
    public float getThumbLength(float trackLength, float minimumLength) {
        return Math.max(minimumLength, trackLength * this.view / this.content);
    }

    /**
     * @param trackLength   the track's length
     * @param minimumLength the shortest thumb
     * @return the thumb's offset from the track's start: the offset's share of the thumb's travel
     */
    public float getThumbOffset(float trackLength, float minimumLength) {
        float travel = trackLength - this.getThumbLength(trackLength, minimumLength);
        float maximum = this.content - this.view;

        return maximum <= 0 ? 0 : travel * this.offset / maximum;
    }


    /**
     * Scrolls by a distance.
     *
     * @param distance the distance, positive toward the end
     */
    public void scrollBy(float distance) {
        this.setOffset(this.offset + distance);
    }

    /**
     * Scrolls to the end of the content and follows it.
     */
    public void scrollToEnd() {
        this.followingEnd = true;

        this.setOffset(Float.MAX_VALUE);
    }

    /**
     * Drags the thumb: the offset is the thumb's share of its travel; nothing while the thumb fills the track.
     *
     * @param thumbOffset   the thumb's offset from the track's start
     * @param trackLength   the track's length
     * @param minimumLength the shortest thumb
     */
    public void dragThumb(float thumbOffset, float trackLength, float minimumLength) {
        float travel = trackLength - this.getThumbLength(trackLength, minimumLength);

        if (travel <= 0) {
            return;
        }

        this.setOffset(thumbOffset / travel * (this.content - this.view));
    }
}

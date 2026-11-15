package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Jumps a camera to stored positions: the engine's view jumps. It keeps camera slots (save the current camera, jump back to it later) and the
 * alert position (the last place that asked for attention, for example a unit under attack, that a jump key returns to), and centres the
 * camera on a position with the top-left corner aligned down to the snap. It moves the same {@link CameraControl} as the camera scrolls and
 * reads the camera's position and shown area when a jump is made. Slot and alert jumps keep their stored positions exactly; the camera keeps
 * itself inside the map.
 * <p>
 * Positions are whole pixels: a camera position is its top-left corner. The alert position is kept by an {@link AlertPosition}, by default
 * in this object; a game whose alert position is game state (scripts read or write it) supplies its own.
 */
public final class ViewJumps {
    /**
     * Keeps the alert position: the camera's top-left position that shows the last alert.
     */
    public interface AlertPosition {
        /**
         * @return the alert camera x in pixels
         */
        int getAlertX();

        /**
         * @return the alert camera y in pixels
         */
        int getAlertY();

        /**
         * Sets the alert position.
         *
         * @param x the alert camera x in pixels
         * @param y the alert camera y in pixels
         */
        void setAlert(int x, int y);
    }

    /**
     * Keeps the alert position in two fields: the default {@link AlertPosition}.
     */
    private static final class StoredAlertPosition implements AlertPosition {
        private int alertX;
        private int alertY;

        private StoredAlertPosition() {
            this.alertX = 0;
            this.alertY = 0;
        }

        /**
         * @return the alert camera x in pixels
         */
        @Override
        public int getAlertX() {
            return this.alertX;
        }

        /**
         * @return the alert camera y in pixels
         */
        @Override
        public int getAlertY() {
            return this.alertY;
        }

        /**
         * Sets the alert position.
         *
         * @param x the alert camera x in pixels
         * @param y the alert camera y in pixels
         */
        @Override
        public void setAlert(int x, int y) {
            this.alertX = x;
            this.alertY = y;
        }
    }

    private final @NotNull CameraControl camera;
    private final @NotNull AlertPosition alert;
    private final int snapMask;

    private final int @NotNull [] slotX;
    private final int @NotNull [] slotY;

    /**
     * Creates a {@link ViewJumps} instance that keeps the alert position itself, at the map origin like every slot.
     *
     * @param camera    the camera
     * @param slotCount the number of camera slots
     * @param snap      the alignment of a centred position in pixels, a power of two
     * @throws IllegalArgumentException if the slot count is negative or the snap is not a positive power of two
     */
    public ViewJumps(@NotNull CameraControl camera, int slotCount, int snap) {
        this(camera, new StoredAlertPosition(), slotCount, snap);
    }

    /**
     * Creates a {@link ViewJumps} instance with its camera slots at the map origin.
     *
     * @param camera    the camera
     * @param alert     the keeper of the alert position
     * @param slotCount the number of camera slots
     * @param snap      the alignment of a centred position in pixels, a power of two
     * @throws IllegalArgumentException if the slot count is negative or the snap is not a positive power of two
     */
    public ViewJumps(@NotNull CameraControl camera, @NotNull AlertPosition alert, int slotCount, int snap) {
        if (slotCount < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid camera slot count {0}", slotCount);
        }

        if (snap <= 0 || Integer.bitCount(snap) != 1) {
            throw Exceptions.newIllegalArgumentException("Invalid view snap {0}", snap);
        }

        this.camera   = camera;
        this.alert    = alert;
        this.snapMask = -snap;

        this.slotX = new int[slotCount];
        this.slotY = new int[slotCount];
    }

    /**
     * @return the number of camera slots
     */
    public int getSlotCount() {
        return this.slotX.length;
    }


    /**
     * Centres the camera on a position, the top-left corner aligned down to the snap.
     *
     * @param x the x in pixels
     * @param y the y in pixels
     */
    public void centerOn(int x, int y) {
        int halfWidth = (int) this.camera.getViewWidth() / 2;
        int halfHeight = (int) this.camera.getViewHeight() / 2;

        this.camera.moveTo((x - halfWidth) & this.snapMask, (y - halfHeight) & this.snapMask);
    }

    /**
     * Stores the alert position of a place: the camera's top-left position that centres it, not below {@code 0}.
     *
     * @param x the x of the place in pixels
     * @param y the y of the place in pixels
     */
    public void storeAlert(int x, int y) {
        int halfWidth = (int) this.camera.getViewWidth() / 2;
        int halfHeight = (int) this.camera.getViewHeight() / 2;

        this.alert.setAlert(x < halfWidth ? 0 : x - halfWidth, y < halfHeight ? 0 : y - halfHeight);
    }

    /**
     * Starts the alert position at the current camera position.
     */
    public void startAlert() {
        this.alert.setAlert((int) this.camera.getX(), (int) this.camera.getY());
    }

    /**
     * Moves the camera to the alert position as stored.
     */
    public void jumpToAlert() {
        this.camera.moveTo(this.alert.getAlertX(), this.alert.getAlertY());
    }

    /**
     * Stores the current camera position in a slot.
     *
     * @param slot the slot from {@code 0}
     * @throws IndexOutOfBoundsException if the slot is not below {@link #getSlotCount}
     */
    public void saveSlot(int slot) {
        Objects.checkIndex(slot, this.slotX.length);

        this.slotX[slot] = (int) this.camera.getX();
        this.slotY[slot] = (int) this.camera.getY();
    }

    /**
     * Moves the camera to the position stored in a slot.
     *
     * @param slot the slot from {@code 0}
     * @throws IndexOutOfBoundsException if the slot is not below {@link #getSlotCount}
     */
    public void jumpToSlot(int slot) {
        Objects.checkIndex(slot, this.slotX.length);

        this.camera.moveTo(this.slotX[slot], this.slotY[slot]);
    }
}

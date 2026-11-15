package org.fogofwarlabs.sc4j.engine.client.audio;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups sounds under one gain, for example music or sound effects; every bus of a mixer is under its master bus. A sound plays at its own gain
 * times its bus gain times the master gain (a sound on the master bus: its own gain times the master gain). Changing a gain updates the sounds
 * that play.
 */
public final class AudioBus {
    private static final float SILENT = 0.0f;
    private static final float FULL = 1.0f;

    /**
     * A sound source on a bus whose voice gains follow the bus gains, closed with its mixer.
     */
    interface Member {
        /**
         * Applies the current bus and master gains to the member's voices.
         */
        void refreshGain();

        /**
         * Closes the member and releases its voices and owned buffers; never throws.
         */
        void close();
    }

    private final @Nullable AudioBus master;
    private final @NotNull List<AudioBus> children;
    private final @NotNull List<Member> members;

    private float gain;

    /**
     * Creates an {@link AudioBus} instance at gain {@code 1}.
     *
     * @param master the master bus, or {@code null} for the master bus itself
     */
    AudioBus(@Nullable AudioBus master) {
        this.master   = master;
        this.children = new ArrayList<>();
        this.members  = new ArrayList<>();

        this.gain = FULL;

        if (master != null) {
            master.children.add(this);
        }
    }

    /**
     * @return {@code true} if this is the mixer's master bus, {@code false} otherwise
     */
    public boolean isMaster() {
        return this.master == null;
    }

    /**
     * @return the gain from {@code 0} to {@code 1}
     */
    public float getGain() {
        return this.gain;
    }

    /**
     * Sets the gain and applies it to the sounds of this bus (of every bus for the master bus); an unchanged gain changes nothing.
     *
     * @param gain the linear gain, clamped to {@code 0} through {@code 1}
     * @throws IllegalStateException if a device operation fails
     */
    public void setGain(float gain) {
        float clamped = Math.clamp(gain, SILENT, FULL);

        if (this.gain == clamped) {
            return;
        }

        this.gain = clamped;
        this.refreshGain();
    }


    /**
     * Returns the gain a sound of this bus plays at.
     *
     * @param gain the sound's own gain
     * @return {@code gain * busGain * masterGain}, with a bus gain of {@code 1} on the master bus
     */
    public float mix(float gain) {
        AudioBus root = this.master;

        return root == null ? gain * FULL * this.gain : gain * this.gain * root.gain;
    }

    /**
     * Adds a sound source whose voices follow the gains of this bus.
     *
     * @param member the source
     */
    void addMember(@NotNull Member member) {
        this.members.add(member);
    }

    /**
     * Removes a source added by {@link #addMember}.
     *
     * @param member the source
     */
    void removeMember(@NotNull Member member) {
        this.members.remove(member);
    }

    private void refreshGain() {
        for (Member member : List.copyOf(this.members)) {
            member.refreshGain();
        }

        for (AudioBus child : this.children) {
            child.refreshGain();
        }
    }
}

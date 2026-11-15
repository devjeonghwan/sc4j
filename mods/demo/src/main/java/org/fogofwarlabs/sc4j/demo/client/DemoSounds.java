package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.demo.content.DemoContent;
import org.fogofwarlabs.sc4j.engine.client.audio.*;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plays the demo's notification and interface sounds from the pack: construction done, unit ready, under attack, a pressed button, a refused
 * action, the acknowledgement of a selected unit or building, a menu button under the pointer (played quietly) and an opened popup. The demo's
 * channel rule: a few channels, the oldest sound gives way, every sound of equal priority. The buffers stay uploaded until the sounds are
 * closed.
 */
public final class DemoSounds implements AutoCloseable {
    /**
     * The sound of a finished building.
     */
    public static final @NotNull String CONSTRUCTION_DONE = "construction-done";
    /**
     * The sound of a trained unit.
     */
    public static final @NotNull String UNIT_READY = "unit-ready";
    /**
     * The sound of a unit or building that takes damage.
     */
    public static final @NotNull String UNDER_ATTACK = "under-attack";
    /**
     * The sound of a pressed HUD button.
     */
    public static final @NotNull String CLICK = "click";
    /**
     * The sound of a refused action.
     */
    public static final @NotNull String REFUSED = "refused";
    /**
     * The sound of a selected unit.
     */
    public static final @NotNull String SELECT = "select";
    /**
     * The sound of a selected building.
     */
    public static final @NotNull String SELECT_BUILDING = "select-building";
    /**
     * The sound of a menu button under the pointer.
     */
    public static final @NotNull String HOVER = "hover";
    /**
     * The sound of an opened popup.
     */
    public static final @NotNull String OPEN = "open";

    private static final int CHANNELS = 4;
    private static final int PRIORITY = 1;
    private static final float GAIN = 1;
    private static final float HOVER_GAIN = 0.4f;
    private static final float CENTER = 0;

    /**
     * Uploads the notification sounds of the pack.
     *
     * @param content the demo content with its pack
     * @param mixer   the client's audio mixer
     * @return the created {@link DemoSounds} instance, closed by the caller
     * @throws IOException if a sound cannot be read
     */
    public static @NotNull DemoSounds open(@NotNull DemoContent content, @NotNull AudioMixer mixer) throws IOException {
        Map<String, AudioBuffer> buffers = new LinkedHashMap<>();

        for (String id : new String[]{CONSTRUCTION_DONE, UNIT_READY, UNDER_ATTACK, CLICK, REFUSED, SELECT, SELECT_BUILDING, HOVER, OPEN}) {
            buffers.put(id, mixer.createBuffer(content.getPack().loadSound(ResourceKey.of(id))));
        }

        return new DemoSounds(mixer.createChannels(mixer.getMasterBus(), CHANNELS, new OldestChannelPolicy()), buffers);
    }

    private final @NotNull SoundChannels channels;
    private final @NotNull Map<@NotNull String, @NotNull AudioBuffer> buffers;
    private final @NotNull SoundCue cue;

    private DemoSounds(@NotNull SoundChannels channels, @NotNull Map<@NotNull String, @NotNull AudioBuffer> buffers) {
        this.channels = channels;
        this.buffers  = buffers;
        this.cue      = SoundCue.of(PRIORITY);
    }

    /**
     * Plays a notification sound.
     *
     * @param id the sound's pack id, one of the constants of this class
     * @return {@code true} if the sound started, {@code false} if no channel took it or the id names no notification
     */
    public boolean play(@NotNull String id) {
        AudioBuffer buffer = this.buffers.get(id);

        return buffer != null && this.channels.play(this.cue, buffer, false, id.equals(HOVER) ? HOVER_GAIN : GAIN, CENTER) != null;
    }

    /**
     * Disposes resources: stops the channels and releases the buffers.
     */
    @Override
    public void close() {
        this.channels.close();

        for (AudioBuffer buffer : this.buffers.values()) {
            buffer.release();
        }

        this.buffers.clear();
    }
}

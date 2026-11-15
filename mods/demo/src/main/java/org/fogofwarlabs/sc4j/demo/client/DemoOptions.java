package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.engine.client.app.SceneClient;
import org.fogofwarlabs.sc4j.engine.client.settings.SettingsGroup;
import org.fogofwarlabs.sc4j.engine.client.settings.SettingsStore;
import org.fogofwarlabs.sc4j.engine.client.world.ContinuousScrollRate;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the demo's options, which the options screen changes: the sound volume, the music volume, the game speed a local game starts at, the camera's
 * scroll speed and the interface size. They live in the group {@value #GROUP} of the demo's own settings file in its storage folder
 * ({@link org.fogofwarlabs.sc4j.engine.mod.ModStorage#getSettingsFile}), never shared with another mod; a missing file or a value outside its choices
 * reads as the default. Applying them sets the client's master volume, music volume and interface size ({@link #apply}); the game scenes read the
 * game and scroll speed when a game starts.
 */
public final class DemoOptions {
    /**
     * The settings group of the options.
     */
    public static final @NotNull String GROUP = "options";
    /**
     * The sound volumes in percent the options offer.
     */
    public static final @NotNull List<@NotNull Integer> SOUND_VOLUMES = List.of(0, 25, 50, 75, 100);
    /**
     * The music volumes in percent the options offer.
     */
    public static final @NotNull List<@NotNull Integer> MUSIC_VOLUMES = List.of(0, 25, 50, 75, 100);
    /**
     * The scroll speeds in percent of the engine's default the options offer.
     */
    public static final @NotNull List<@NotNull Integer> SCROLL_SPEEDS = List.of(50, 100, 150, 200);
    /**
     * The interface sizes in percent of the operating system's scale the options offer.
     */
    public static final @NotNull List<@NotNull Integer> INTERFACE_SIZES = List.of(100, 125, 150, 175, 200);

    private static final @NotNull Logger LOGGER = Logger.of(DemoOptions.class);
    private static final @NotNull String SOUND_VOLUME_KEY = "soundVolume";
    private static final @NotNull String MUSIC_VOLUME_KEY = "musicVolume";
    private static final @NotNull String GAME_SPEED_KEY = "gameSpeed";
    private static final @NotNull String SCROLL_SPEED_KEY = "scrollSpeed";
    private static final @NotNull String INTERFACE_SIZE_KEY = "interfaceSize";
    private static final int DEFAULT_SOUND_VOLUME = 100;
    private static final int DEFAULT_MUSIC_VOLUME = 50;
    private static final int DEFAULT_GAME_SPEED = 1;
    private static final int DEFAULT_SCROLL_SPEED = 100;
    private static final int DEFAULT_INTERFACE_SIZE = 100;
    private static final float PERCENT = 100;

    /**
     * Reads the options from a settings file; a missing file, group or key and a value outside its choices read as the default.
     *
     * @param file the settings file
     * @return the created {@link DemoOptions} instance
     */
    public static @NotNull DemoOptions read(@NotNull Path file) {
        DemoOptions options = new DemoOptions();

        if (!Files.isRegularFile(file)) {
            return options;
        }

        SettingsGroup group;

        try {
            group = SettingsStore.read(file).getGroup(GROUP);
        } catch (IOException exception) {
            LOGGER.warning("OPTIONS NOT READ: {0} ({1})", file, exception.getMessage());

            return options;
        }

        if (group == null) {
            return options;
        }

        options.setSoundVolume(readChoice(group, SOUND_VOLUME_KEY, SOUND_VOLUMES, DEFAULT_SOUND_VOLUME));
        options.setMusicVolume(readChoice(group, MUSIC_VOLUME_KEY, MUSIC_VOLUMES, DEFAULT_MUSIC_VOLUME));
        options.setGameSpeed(readChoice(group, GAME_SPEED_KEY, DemoGameScene.SPEEDS, DEFAULT_GAME_SPEED));
        options.setScrollSpeed(readChoice(group, SCROLL_SPEED_KEY, SCROLL_SPEEDS, DEFAULT_SCROLL_SPEED));
        options.setInterfaceSize(readChoice(group, INTERFACE_SIZE_KEY, INTERFACE_SIZES, DEFAULT_INTERFACE_SIZE));

        return options;
    }

    private static int readChoice(@NotNull SettingsGroup group, @NotNull String key, @NotNull List<@NotNull Integer> choices, int defaultValue) {
        if (!group.hasValue(key)) {
            return defaultValue;
        }

        try {
            int value = group.getInteger(key, Integer.MIN_VALUE, Integer.MAX_VALUE);

            return choices.contains(value) ? value : defaultValue;
        } catch (IllegalArgumentException exception) {
            LOGGER.warning("OPTION IGNORED: {0}", exception.getMessage());

            return defaultValue;
        }
    }


    private int soundVolume;
    private int musicVolume;
    private int gameSpeed;
    private int scrollSpeed;
    private int interfaceSize;

    /**
     * Creates a {@link DemoOptions} instance of the defaults: full sound volume, half music volume, normal game and scroll speed, the operating
     * system's interface size.
     */
    public DemoOptions() {
        this.soundVolume   = DEFAULT_SOUND_VOLUME;
        this.musicVolume   = DEFAULT_MUSIC_VOLUME;
        this.gameSpeed     = DEFAULT_GAME_SPEED;
        this.scrollSpeed   = DEFAULT_SCROLL_SPEED;
        this.interfaceSize = DEFAULT_INTERFACE_SIZE;
    }

    /**
     * @return the sound volume in percent, one of {@link #SOUND_VOLUMES}
     */
    public int getSoundVolume() {
        return this.soundVolume;
    }

    /**
     * Sets the sound volume.
     *
     * @param soundVolume the sound volume in percent, one of {@link #SOUND_VOLUMES}
     */
    public void setSoundVolume(int soundVolume) {
        this.soundVolume = soundVolume;
    }

    /**
     * @return the music volume in percent, one of {@link #MUSIC_VOLUMES}
     */
    public int getMusicVolume() {
        return this.musicVolume;
    }

    /**
     * Sets the music volume.
     *
     * @param musicVolume the music volume in percent, one of {@link #MUSIC_VOLUMES}
     */
    public void setMusicVolume(int musicVolume) {
        this.musicVolume = musicVolume;
    }

    /**
     * @return the game speed a local game starts at, one of {@link DemoGameScene#SPEEDS}
     */
    public int getGameSpeed() {
        return this.gameSpeed;
    }

    /**
     * Sets the game speed a local game starts at.
     *
     * @param gameSpeed the game speed, one of {@link DemoGameScene#SPEEDS}
     */
    public void setGameSpeed(int gameSpeed) {
        this.gameSpeed = gameSpeed;
    }

    /**
     * @return the camera's scroll speed in percent of the engine's default, one of {@link #SCROLL_SPEEDS}
     */
    public int getScrollSpeed() {
        return this.scrollSpeed;
    }

    /**
     * Sets the camera's scroll speed.
     *
     * @param scrollSpeed the scroll speed in percent of the engine's default, one of {@link #SCROLL_SPEEDS}
     */
    public void setScrollSpeed(int scrollSpeed) {
        this.scrollSpeed = scrollSpeed;
    }

    /**
     * @return the interface size in percent of the operating system's scale, one of {@link #INTERFACE_SIZES}
     */
    public int getInterfaceSize() {
        return this.interfaceSize;
    }

    /**
     * Sets the interface size.
     *
     * @param interfaceSize the interface size in percent of the operating system's scale, one of {@link #INTERFACE_SIZES}
     */
    public void setInterfaceSize(int interfaceSize) {
        this.interfaceSize = interfaceSize;
    }


    /**
     * Writes the options into the group {@value #GROUP} of a settings file; the file's other groups and keys stay.
     *
     * @param file the settings file
     * @throws IOException if the file cannot be read or written
     */
    public void write(@NotNull Path file) throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();

        values.put(SOUND_VOLUME_KEY, this.soundVolume);
        values.put(MUSIC_VOLUME_KEY, this.musicVolume);
        values.put(GAME_SPEED_KEY, this.gameSpeed);
        values.put(SCROLL_SPEED_KEY, this.scrollSpeed);
        values.put(INTERFACE_SIZE_KEY, this.interfaceSize);
        SettingsStore.write(file, GROUP, values);
    }

    /**
     * Applies the options that belong to the whole client: the master volume of its audio, the volume of its music under it and its
     * interface size.
     *
     * @param client the client
     */
    public void apply(@NotNull SceneClient client) {
        client.getAudio().getMasterBus().setGain(this.soundVolume / PERCENT);
        client.getMusic().setVolume(this.musicVolume / PERCENT);
        client.setUserScale(this.interfaceSize / PERCENT);
    }

    /**
     * Applies the scroll speed to the scroll rate of a game screen's camera.
     *
     * @param rate the scroll rate
     */
    public void applyTo(@NotNull ContinuousScrollRate rate) {
        rate.setKeySpeed(ContinuousScrollRate.DEFAULT_KEY_SPEED * this.scrollSpeed / PERCENT);
        rate.setEdgeSpeed(ContinuousScrollRate.DEFAULT_EDGE_SPEED * this.scrollSpeed / PERCENT);
    }
}

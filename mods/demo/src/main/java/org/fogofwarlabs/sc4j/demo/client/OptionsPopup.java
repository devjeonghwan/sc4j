package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.engine.client.dialog.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows the demo's options ({@link DemoOptions}) in the {@value #DIALOG} popup from the main menu or the game menu: the sound volume, the music
 * volume, the game speed a local game starts at, the scroll speed and the interface size, each a list of its choices. OK applies the chosen options
 * to the client, tells the owner (for example a running game's camera) and stores them in the demo's settings file; Cancel leaves them as they were.
 */
public final class OptionsPopup implements DemoPopup.Listener {
    /**
     * The pack id of the options dialog.
     */
    public static final @NotNull String DIALOG = "options";
    /**
     * The id of the sound volume list.
     */
    public static final int SOUND_ID = 10;
    /**
     * The id of the game speed list.
     */
    public static final int GAME_SPEED_ID = 11;
    /**
     * The id of the scroll speed list.
     */
    public static final int SCROLL_SPEED_ID = 12;
    /**
     * The id of the interface size list.
     */
    public static final int INTERFACE_SIZE_ID = 13;
    /**
     * The id of the music volume list.
     */
    public static final int MUSIC_ID = 14;
    /**
     * The shown names of the scroll speeds, in the order of {@link DemoOptions#SCROLL_SPEEDS}.
     */
    public static final @NotNull List<@NotNull String> SCROLL_SPEED_NAMES = List.of("Slow", "Normal", "Fast", "Fastest");

    private static final @NotNull Logger LOGGER = Logger.of(OptionsPopup.class);
    private static final @NotNull String OFF = "Off";
    private static final @NotNull String PERCENT_SIGN = "%";
    private static final @NotNull String SPEED_PREFIX = "x";

    /**
     * Opens the options over the client's current scene.
     *
     * @param demo      the demo's shared services
     * @param onApplied called after OK applied the chosen options, for example to set a running game's scroll speed
     * @return the open popup
     * @throws IOException if the dialog cannot be read
     */
    public static @NotNull DemoPopup open(@NotNull DemoClient demo, @NotNull Runnable onApplied) throws IOException {
        DemoOptions options = demo.getOptions();
        DemoPopup popup = DemoPopup.open(demo, DIALOG, new OptionsPopup(demo, onApplied));

        fill(popup, SOUND_ID, describe(DemoOptions.SOUND_VOLUMES, OFF, "", PERCENT_SIGN), DemoOptions.SOUND_VOLUMES.indexOf(options.getSoundVolume()));
        fill(popup, MUSIC_ID, describe(DemoOptions.MUSIC_VOLUMES, OFF, "", PERCENT_SIGN), DemoOptions.MUSIC_VOLUMES.indexOf(options.getMusicVolume()));
        fill(popup, GAME_SPEED_ID, describe(DemoGameScene.SPEEDS, null, SPEED_PREFIX, ""), DemoGameScene.SPEEDS.indexOf(options.getGameSpeed()));
        fill(popup, SCROLL_SPEED_ID, SCROLL_SPEED_NAMES, DemoOptions.SCROLL_SPEEDS.indexOf(options.getScrollSpeed()));
        fill(popup, INTERFACE_SIZE_ID, describe(DemoOptions.INTERFACE_SIZES, null, "", PERCENT_SIGN),
             DemoOptions.INTERFACE_SIZES.indexOf(options.getInterfaceSize()));

        return popup;
    }

    private static @NotNull List<@NotNull String> describe(@NotNull List<@NotNull Integer> values, @Nullable String zeroName,
                                                           @NotNull String prefix, @NotNull String suffix) {
        List<String> names = new ArrayList<>(values.size());

        for (int value : values) {
            names.add(value == 0 && zeroName != null ? zeroName : prefix + value + suffix);
        }

        return names;
    }

    private static void fill(@NotNull DemoPopup popup, int id, @NotNull List<@NotNull String> entries, int selection) {
        ComboBox box = (ComboBox) popup.requireControl(id);

        box.setEntries(entries);
        box.setSelection(Math.max(selection, 0));
    }

    private static int readSelection(@NotNull DemoPopup popup, int id) {
        return Math.max(((ComboBox) popup.requireControl(id)).getSelection(), 0);
    }


    private final @NotNull DemoClient demo;
    private final @NotNull Runnable onApplied;

    private OptionsPopup(@NotNull DemoClient demo, @NotNull Runnable onApplied) {
        this.demo      = demo;
        this.onApplied = onApplied;
    }

    /**
     * Called when a control of the popup is activated.
     * <p>
     * OK applies and stores the chosen options and tells the owner; OK and Cancel close the popup.
     *
     * @param popup the popup
     * @param id    the id of the activated control
     */
    @Override
    public void onActivated(@NotNull DemoPopup popup, int id) {
        if (id == DemoPopup.OK_ID) {
            this.apply(popup);
            this.onApplied.run();
            popup.close();
        } else if (id == DemoPopup.CANCEL_ID) {
            popup.close();
        }
    }

    /**
     * Reads the chosen options from the popup, applies them to the client and stores them.
     */
    private void apply(@NotNull DemoPopup popup) {
        DemoOptions options = this.demo.getOptions();

        options.setSoundVolume(DemoOptions.SOUND_VOLUMES.get(readSelection(popup, SOUND_ID)));
        options.setMusicVolume(DemoOptions.MUSIC_VOLUMES.get(readSelection(popup, MUSIC_ID)));
        options.setGameSpeed(DemoGameScene.SPEEDS.get(readSelection(popup, GAME_SPEED_ID)));
        options.setScrollSpeed(DemoOptions.SCROLL_SPEEDS.get(readSelection(popup, SCROLL_SPEED_ID)));
        options.setInterfaceSize(DemoOptions.INTERFACE_SIZES.get(readSelection(popup, INTERFACE_SIZE_ID)));
        options.apply(this.demo.getClient());

        try {
            options.write(this.demo.getClient().getStorage().getSettingsFile());
        } catch (IOException exception) {
            LOGGER.warning("OPTIONS NOT STORED: {0}", exception.getMessage());
        }
    }
}

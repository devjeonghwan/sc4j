package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.engine.client.game.GameTimeUtils;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.trigger.PlayerOutcome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Shows how a game of the demo ended for the person over the victory or defeat backdrop, with the emblem of the person's faction above:
 * victory or defeat, the game, its game time and where its replay was stored. Continue returns to the main menu.
 */
public final class OutcomeScene extends DemoMenuScene {
    /**
     * The pack id of the outcome's dialog.
     */
    public static final @NotNull String DIALOG = "outcome";
    /**
     * The id of the title.
     */
    public static final int TITLE_ID = 2;
    /**
     * The id of the game's description.
     */
    public static final int GAME_ID = 3;
    /**
     * The id of the game's length.
     */
    public static final int LENGTH_ID = 4;
    /**
     * The id of the replay's file.
     */
    public static final int REPLAY_ID = 5;
    /**
     * The id of the continue button.
     */
    public static final int CONTINUE_ID = 10;

    private static final float EMBLEM_GAP = 8;
    private static final float HALF = 2;

    private final @NotNull MainMenuScene menu;
    private final @NotNull PlayerOutcome outcome;
    private final @Nullable Faction faction;
    private final @NotNull String game;
    private final long loops;
    private final @NotNull String replay;

    /**
     * Creates an {@link OutcomeScene} instance.
     *
     * @param demo    the demo's shared services
     * @param menu    the main menu the continue button returns to
     * @param outcome the person's outcome
     * @param faction the person's faction, whose emblem shows, or {@code null} for none
     * @param game    the game's description
     * @param loops   the game loops the game ran
     * @param replay  the stored replay's file name, or an empty text when none was stored
     */
    public OutcomeScene(@NotNull DemoClient demo, @NotNull MainMenuScene menu, @NotNull PlayerOutcome outcome, @Nullable Faction faction,
                        @NotNull String game, long loops, @NotNull String replay) {
        super(demo, DIALOG);

        this.menu    = menu;
        this.outcome = outcome;
        this.faction = faction;
        this.game    = game;
        this.loops   = loops;
        this.replay  = replay;
    }

    /**
     * @return the person's outcome
     */
    public @NotNull PlayerOutcome getOutcome() {
        return this.outcome;
    }

    /**
     * Called when the scene opened and its dialog is built: fills the dialog's controls. The default fills nothing.
     * <p>
     * Shows the outcome, the game, its length and the replay.
     */
    @Override
    protected void onOpened() {
        this.requireControl(TITLE_ID).setText(this.outcome == PlayerOutcome.VICTORY ? "Victory" : "Defeat");
        this.requireControl(GAME_ID).setText(this.game);
        this.requireControl(LENGTH_ID).setText("Game time " + GameTimeUtils.format(this.loops, DemoGameScene.LOOP_MILLISECONDS));
        this.requireControl(REPLAY_ID).setText(this.replay.isEmpty() ? "No replay stored." : "Replay: " + this.replay);
    }

    /**
     * Returns the screen picture under the dialog. The default is the menu background.
     * <p>
     * A victory shows the victory backdrop, a defeat the defeat backdrop.
     *
     * @return the pack id of the picture
     */
    @Override
    protected @NotNull String getScreenPicture() {
        return this.outcome == PlayerOutcome.VICTORY ? DemoSkin.VICTORY_BACKDROP : DemoSkin.DEFEAT_BACKDROP;
    }

    /**
     * Draws over the screen after the dialog.
     * <p>
     * Draws the emblem of the person's faction centred above the dialog, at most {@value DemoMenuScene#EMBLEM_SIZE} pixels and smaller where
     * the space above is lower.
     *
     * @param surface the surface
     * @param left    the left coordinate of the dialog
     * @param top     the top coordinate of the dialog
     * @param width   the width of the dialog
     * @param height  the height of the dialog
     * @throws IOException if a picture cannot be read
     */
    @Override
    protected void drawOver(@NotNull Canvas surface, float left, float top, float width, float height) throws IOException {
        float size = Math.min(EMBLEM_SIZE, top - EMBLEM_GAP * HALF);

        this.getDemo().getSkin().drawEmblem(surface, this.faction, left + width / HALF, top - EMBLEM_GAP - size / HALF, size);
    }

    /**
     * Called when a control of the dialog is activated.
     * <p>
     * Continue returns to the main menu.
     *
     * @param id the id of the activated control
     */
    @Override
    protected void onActivated(int id) {
        if (id == CONTINUE_ID) {
            this.menu.show();
        }
    }
}

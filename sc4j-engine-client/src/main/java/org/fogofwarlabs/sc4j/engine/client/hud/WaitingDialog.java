package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.skin.HudSkin;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepPeer;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepSession;
import org.fogofwarlabs.sc4j.engine.lockstep.WaitCountdown;
import org.fogofwarlabs.sc4j.engine.lockstep.WaitingPeers;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * Shows the network waiting dialog of a lockstep game in a HUD rectangle: the engine default dialog over the waiting state of the session's
 * all-turns barrier ({@link LockstepSession#getWaitingPeers()}). It is shown while the barrier has overdue participants ({@link #isShown}):
 * the title {@value #TITLE} with the countdown's seconds ({@link WaitingPeers#getCountdownSeconds()}) at its right, the name of each overdue
 * participant under it, and the {@value #DROP_LABEL} button, whose left press drops the overdue participants
 * ({@link LockstepSession#dropWaitingPeers()}). The names come from the mod ({@link #setPeerNames}), by default {@code Player <n>} of the
 * participant's player. The host lays the dialog out around a point ({@link #placeAt}): its size follows its lines.
 * <p>
 * The dialog draws in the skin's dialog box, padded by its padding ({@link HudSkin#getDialogBackground}, {@link HudSkin#getDialogPadding}),
 * and its button in the skin's command button boxes. While it is hidden it has no size and takes no input.
 */
public final class WaitingDialog extends HudBlock {
    /**
     * The title of the dialog.
     */
    public static final @NotNull String TITLE = "Waiting for players";
    /**
     * The label of the drop button.
     */
    public static final @NotNull String DROP_LABEL = "Drop";

    private static final @NotNull String PLAYER_PREFIX = "Player ";
    private static final @NotNull String OBSERVER_NAME = "Observer";
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int NAME_COLOR = 0xE8D8A0FF;
    private static final int COUNTDOWN_COLOR = 0xFF7060FF;
    private static final float MINIMUM_WIDTH = 280;
    private static final float COUNTDOWN_GAP = 16;
    private static final float BUTTON_PADDING = 12;
    private static final float BUTTON_MARGIN = 6;
    private static final float MINIMUM_BUTTON_WIDTH = 96;
    private static final float HALF_DIVISOR = 2;

    private final @NotNull LockstepSession session;
    private final @NotNull Font font;
    private final @NotNull StringBuilder countdownText;

    private @NotNull Function<@NotNull LockstepPeer, @NotNull String> peerNames;

    private float pointerX;
    private float pointerY;

    /**
     * Creates a {@link WaitingDialog} instance that names the participants by their players.
     *
     * @param session the lockstep session whose waiting state the dialog shows
     * @param font    the font of the texts
     */
    public WaitingDialog(@NotNull LockstepSession session, @NotNull Font font) {
        this.session       = session;
        this.font          = font;
        this.countdownText = new StringBuilder();

        this.peerNames = peer -> {
            Player player = session.getPlayer(peer);

            return player != null ? PLAYER_PREFIX + (player.getIndex() + 1) : OBSERVER_NAME;
        };

        this.pointerX = -1;
        this.pointerY = -1;
    }

    /**
     * Sets the shown name of each participant, for example the names of the room's members.
     *
     * @param peerNames the name of a participant
     */
    public void setPeerNames(@NotNull Function<@NotNull LockstepPeer, @NotNull String> peerNames) {
        this.peerNames = peerNames;
    }


    /**
     * Returns whether the block shows something now: the dialog shows while the session's barrier has overdue participants.
     *
     * @return {@code true} if the session has a waiting state with a countdown open, {@code false} otherwise
     */
    @Override
    public boolean isShown() {
        WaitingPeers waiting = this.session.getWaitingPeers();

        return waiting != null && waiting.getCountdownSeconds() != WaitCountdown.CLOSED && !waiting.getOverduePeers().isEmpty();
    }

    /**
     * Lays the dialog out centred on a point, as large as its lines need; a hidden dialog gets no size.
     *
     * @param centerX the logical column of the centre
     * @param centerY the logical row of the centre
     */
    public void placeAt(float centerX, float centerY) {
        WaitingPeers waiting = this.session.getWaitingPeers();
        List<LockstepPeer> overdue = waiting != null && this.isShown() ? waiting.getOverduePeers() : List.of();

        if (overdue.isEmpty()) {
            this.setBounds(centerX, centerY, 0, 0);

            return;
        }

        float padding = this.getSkin().getDialogPadding();
        float contentWidth = this.font.measure(TITLE) + COUNTDOWN_GAP + this.font.measure(this.writeCountdown(waiting));

        for (LockstepPeer peer : overdue) {
            contentWidth = Math.max(contentWidth, this.font.measure(this.peerNames.apply(peer)));
        }

        float width = Math.max(contentWidth + 2 * padding, MINIMUM_WIDTH);
        float height = (1 + overdue.size()) * this.font.getLineHeight() + BUTTON_MARGIN + this.getButtonHeight() + 2 * padding;

        this.setBounds(centerX - width / HALF_DIVISOR, centerY - height / HALF_DIVISOR, width, height);
    }

    /**
     * Checks whether a logical position lies on the drop button of the shown dialog.
     *
     * @param pointerX the logical column
     * @param pointerY the logical row
     * @return {@code true} if the dialog is shown and the position lies on its drop button, {@code false} otherwise
     */
    public boolean isDropButtonAt(float pointerX, float pointerY) {
        float left = this.getButtonX();
        float top = this.getButtonY();

        return this.isShown() && pointerX >= left && pointerX < left + this.getButtonWidth() && pointerY >= top &&
               pointerY < top + this.getButtonHeight();
    }

    /**
     * Draws the block: the dialog box, the title with the countdown, the overdue participants' names and the drop button; nothing while it is
     * hidden.
     *
     * @param canvas the canvas, between its begin and end
     */
    @Override
    public void draw(@NotNull Canvas canvas) {
        WaitingPeers waiting = this.session.getWaitingPeers();

        if (waiting == null || !this.isShown() || this.getWidth() <= 0) {
            return;
        }

        HudSkin skin = this.getSkin();
        float padding = skin.getDialogPadding();
        float left = this.getX() + padding;
        float lineHeight = this.font.getLineHeight();
        float lineY = this.getY() + padding;
        StringBuilder countdown = this.writeCountdown(waiting);

        skin.getDialogBackground().draw(canvas, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        this.font.draw(canvas, TITLE, left, lineY, TITLE_COLOR);
        this.font.draw(canvas, countdown, this.getX() + this.getWidth() - padding - this.font.measure(countdown), lineY, COUNTDOWN_COLOR);

        for (LockstepPeer peer : waiting.getOverduePeers()) {
            lineY += lineHeight;
            this.font.draw(canvas, this.peerNames.apply(peer), left, lineY, NAME_COLOR);
        }

        float buttonX = this.getButtonX();
        float buttonY = this.getButtonY();
        float buttonWidth = this.getButtonWidth();
        float buttonHeight = this.getButtonHeight();
        boolean hovered = this.isDropButtonAt(this.pointerX, this.pointerY);

        skin.getCommandButton().findStyle(true, false, hovered).draw(canvas, buttonX, buttonY, buttonWidth, buttonHeight);
        this.font.draw(canvas, DROP_LABEL,
                       buttonX + (buttonWidth - this.font.measure(DROP_LABEL)) / HALF_DIVISOR, buttonY + BUTTON_MARGIN, TITLE_COLOR);
    }

    /**
     * Handles an input event: while the dialog is shown, a left press on the drop button drops the overdue participants and other presses
     * inside are taken; while it is hidden it takes nothing. A pointer move is remembered for the hovered button and not taken.
     *
     * @param event the event
     * @return {@code true} if the dialog took the event, {@code false} otherwise
     */
    @Override
    public boolean handleInput(@NotNull InputEvent event) {
        if (event.getType() == InputEvent.Type.POINTER_MOVED) {
            this.pointerX = event.getX();
            this.pointerY = event.getY();
        }

        if (!this.isShown()) {
            return false;
        }

        if (event.getType() == InputEvent.Type.BUTTON_PRESSED && event.getButton() == PointerButton.LEFT &&
            this.isDropButtonAt(event.getX(), event.getY())) {
            this.session.dropWaitingPeers();

            return true;
        }

        return super.handleInput(event);
    }

    private @NotNull StringBuilder writeCountdown(@NotNull WaitingPeers waiting) {
        this.countdownText.setLength(0);

        return this.countdownText.append(Math.max(waiting.getCountdownSeconds(), 0));
    }

    private float getButtonWidth() {
        return Math.max(this.font.measure(DROP_LABEL) + 2 * BUTTON_PADDING, MINIMUM_BUTTON_WIDTH);
    }

    private float getButtonHeight() {
        return this.font.getLineHeight() + 2 * BUTTON_MARGIN;
    }

    private float getButtonX() {
        return this.getX() + this.getWidth() - this.getSkin().getDialogPadding() - this.getButtonWidth();
    }

    private float getButtonY() {
        return this.getY() + this.getHeight() - this.getSkin().getDialogPadding() - this.getButtonHeight();
    }
}

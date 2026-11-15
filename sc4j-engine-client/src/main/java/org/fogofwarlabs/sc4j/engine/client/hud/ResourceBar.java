package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.PictureRenderer;
import org.fogofwarlabs.sc4j.engine.client.skin.HudSkin;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Shows the local player's resources in a HUD rectangle: the engine default resource bar. From the right edge leftwards it draws the supply
 * of the player's faction as {@code used/provided} and then each resource kind as {@code name amount}, in the game's resource order, in the box
 * of its {@link HudSkin}; the mod names the kinds and the supply. Where the skin has an icon of the supply or of a kind, the icon stands
 * before the numbers instead of the name. A session without local player (a replay or an observer) shows the entries of the viewed player
 * ({@link #setViewedPlayer}), with that player's name in its team colour at the left edge; without one the bar is empty.
 */
public final class ResourceBar extends HudBlock {
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final float PADDING = 6;
    private static final float ENTRY_GAP = 16;
    private static final float ICON_INSET = 2;
    private static final float ICON_GAP = 4;
    private static final int OPAQUE_ALPHA = 0xFF;

    private final @NotNull GameView view;
    private final @NotNull Font font;
    private final @NotNull Function<ResourceKind, String> resourceNames;
    private final @NotNull String supplyName;

    private final @NotNull StringBuilder text;

    private @Nullable Player viewedPlayer;
    private @NotNull String viewedName;

    /**
     * Creates a {@link ResourceBar} instance.
     *
     * @param view          the game view
     * @param font          the font of the entries
     * @param resourceNames the shown name of each resource kind
     * @param supplyName    the shown name of the supply
     */
    public ResourceBar(@NotNull GameView view, @NotNull Font font,
                       @NotNull Function<ResourceKind, String> resourceNames, @NotNull String supplyName) {
        this.view          = view;
        this.font          = font;
        this.resourceNames = resourceNames;
        this.supplyName    = supplyName;

        this.text = new StringBuilder();

        this.viewedPlayer = null;
        this.viewedName   = "";
    }

    /**
     * @return the player whose entries a session without local player shows, or {@code null} for none
     */
    public @Nullable Player getViewedPlayer() {
        return this.viewedPlayer;
    }

    /**
     * Sets the player whose entries a session without local player shows, for example the player a replay's viewer follows.
     *
     * @param player the player, or {@code null} for an empty bar
     * @param name   the player's shown name, drawn at the bar's left edge
     */
    public void setViewedPlayer(@Nullable Player player, @NotNull String name) {
        this.viewedPlayer = player;
        this.viewedName   = name;
    }

    /**
     * Draws the block: the background and the entries of the local player, or of the viewed player with its name.
     *
     * @param canvas the canvas, between its begin and end
     */
    @Override
    public void draw(@NotNull Canvas canvas) {
        HudSkin skin = this.getSkin();

        skin.getResourceBarBackground().draw(canvas, this.getX(), this.getY(), this.getWidth(), this.getHeight());

        Player localPlayer = this.view.getLocalPlayer();
        Player player = localPlayer != null ? localPlayer : this.viewedPlayer;

        if (player == null) {
            return;
        }

        float textY = this.getY() + (this.getHeight() - this.font.getLineHeight()) / 2;

        if (localPlayer == null) {
            this.font.draw(canvas, this.viewedName, this.getX() + PADDING, textY, this.view.getTeamColor(player) << Byte.SIZE | OPAQUE_ALPHA);
        }

        float right = this.getX() + this.getWidth() - PADDING;
        Faction faction = this.view.getFaction(player);

        if (faction != null) {
            Picture icon = skin.findSupplyIcon(faction);

            this.text.setLength(0);

            if (icon == null) {
                this.text.append(this.supplyName).append(' ');
            }

            this.text.append(this.view.getSupplyUsed(player, faction)).append('/').append(this.view.getSupplyProvided(player, faction));
            right = this.drawEntry(canvas, right, textY, icon);
        }

        List<ResourceKind> kinds = this.view.getResourceKinds();

        for (int index = kinds.size() - 1; index >= 0; index--) {
            ResourceKind kind = kinds.get(index);
            Picture icon = skin.findResourceIcon(kind);

            this.text.setLength(0);

            if (icon == null) {
                this.text.append(this.resourceNames.apply(kind)).append(' ');
            }

            this.text.append(this.view.getResourceAmount(player, kind));
            right = this.drawEntry(canvas, right, textY, icon);
        }
    }

    /**
     * Draws {@link #text} ending at a right edge, after its icon if it has one: a square of the bar's height less the inset at both sides.
     *
     * @param canvas the canvas
     * @param right  the right edge of the entry
     * @param textY  the top of the text line
     * @param icon   the icon of the entry, or {@code null} for none
     * @return the right edge of the next entry to the left
     */
    private float drawEntry(@NotNull Canvas canvas, float right, float textY, @Nullable Picture icon) {
        float left = right - this.font.measure(this.text);

        this.font.draw(canvas, this.text, left, textY, TEXT_COLOR);

        if (icon != null) {
            float iconSize = Math.max(this.getHeight() - 2 * ICON_INSET, 0);

            left -= ICON_GAP + iconSize;
            this.getSkin().drawPicture(canvas, icon, left, this.getY() + ICON_INSET, iconSize, iconSize,
                                       PictureRenderer.NO_TEAM_COLOR, PictureRenderer.OPAQUE_WHITE);
        }

        return left - ENTRY_GAP;
    }
}

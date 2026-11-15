package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.command.CancelAction;
import org.fogofwarlabs.sc4j.engine.client.command.CommandRefusal;
import org.fogofwarlabs.sc4j.engine.client.command.CommandTranslator;
import org.fogofwarlabs.sc4j.engine.client.command.OrderedSelection;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.PictureRenderer;
import org.fogofwarlabs.sc4j.engine.client.skin.ButtonStyle;
import org.fogofwarlabs.sc4j.engine.client.skin.HudSkin;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.client.world.StatusBarUtils;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Shows the production queue of the one selected unit in a HUD rectangle: the engine default queue panel, hosted by the game screen's console
 * ({@link org.fogofwarlabs.sc4j.engine.client.game.GameScreen#setQueuePanel}). It is shown while one unit of the local player is selected (any
 * unit in a replay) that has queue entries or stands under construction ({@link #isShown}), and takes no input otherwise. The slots of the
 * unit's queue capacity stand in rows from the top left, as many rows as give the largest square slots in the panel, the block of rows and the
 * progress bar under it centred in the panel's height; each entry shows its unit type's icon, and the bar, as wide as the rows, shows the
 * progress of the entry in progress. A front entry that waits for supply ({@link UnitView#isWaitingForSupply}) shows its icon dimmed over the
 * waiting bar. A building under construction shows one slot of the same size with its own type, and the bar its construction progress. The
 * hovered filled slot shows a {@link Tooltip} with its unit type's name and, while it waits, the missing supply. A left press on a filled slot
 * cancels it through the {@link CancelAction} ({@link #setCancelAction}): the entry, or the construction.
 * <p>
 * The panel draws no box of its own, so the box under it (the console's queue region) shows through. Filled slots are the skin's command
 * buttons (hovered under the pointer), empty slots its disabled button; the icon is the skin's unit icon, else the unit type's portrait in the
 * owner's team colour, else the unit type's name. Sizes are in the logical units of the canvas, so the pictures' detail versions draw at
 * higher display scales.
 */
public final class ProductionQueuePanel extends HudBlock {
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int PROGRESS_COLOR = 0xE0B040FF;
    private static final int WAITING_BAR_COLOR = 0x802018FF;
    private static final int WAITING_TEXT_COLOR = 0xFF7060FF;
    private static final int DIMMED_COLOR = 0x808080FF;
    private static final float PADDING = 6;
    private static final float SLOT_GAP = 4;
    private static final float ICON_INSET = 3;
    private static final float BAR_HEIGHT = 8;
    private static final float BAR_GAP = 4;

    private static int getFilledCount(@NotNull UnitView unit) {
        return unit.getConstructionProgress() < UnitView.COMPLETE ? 1 : unit.getQueueLength();
    }

    private static int getSlotCount(@NotNull UnitView unit) {
        return unit.getConstructionProgress() < UnitView.COMPLETE ? 1 : Math.max(unit.getQueueCapacity(), unit.getQueueLength());
    }

    /**
     * Returns the number of slots the rows are laid out for: the queue capacity, also for a building under construction, so its one slot has
     * the size of a queue slot.
     */
    private static int getLayoutCount(@NotNull UnitView unit) {
        return Math.max(Math.max(unit.getQueueCapacity(), unit.getQueueLength()), 1);
    }


    private final @NotNull GameView view;
    private final @NotNull OrderedSelection selection;
    private final @NotNull CommandTranslator translator;
    private final @NotNull Font font;
    private final @NotNull Function<UnitType, String> typeNames;
    private final @NotNull String waitingText;
    private final @NotNull Tooltip tooltip;

    private @Nullable CancelAction cancelAction;

    private float pointerX;
    private float pointerY;

    private int columns;
    private int rows;
    private float slotSize;

    /**
     * Creates a {@link ProductionQueuePanel} instance without cancel action.
     *
     * @param view       the game view
     * @param selection  the local player's selection
     * @param translator the translator the cancel commands go through
     * @param font       the font of the names of unit types without picture and of the tooltip
     * @param typeNames  the shown name of each unit type
     * @param supplyName the shown name of the supply, for the tooltip of an entry that waits for it
     */
    public ProductionQueuePanel(@NotNull GameView view, @NotNull OrderedSelection selection, @NotNull CommandTranslator translator,
                                @NotNull Font font, @NotNull Function<UnitType, String> typeNames, @NotNull String supplyName) {
        this.view        = view;
        this.selection   = selection;
        this.translator  = translator;
        this.font        = font;
        this.typeNames   = typeNames;
        this.waitingText = CommandRefusal.MISSING_SUPPLY.describe(typeNames, ResourceKind::getDebugName, supplyName);
        this.tooltip     = new Tooltip(font);

        this.cancelAction = null;

        this.pointerX = -1;
        this.pointerY = -1;

        this.columns  = 1;
        this.rows     = 1;
        this.slotSize = 0;
    }

    /**
     * @return the action a press on a slot cancels it with, or {@code null} if a press cancels nothing
     */
    public @Nullable CancelAction getCancelAction() {
        return this.cancelAction;
    }

    /**
     * Sets the action a press on a slot cancels it with, for example the command card's ({@link StandardCommandCardSource#getCancelAction}).
     *
     * @param cancelAction the action, or {@code null} if a press cancels nothing
     */
    public void setCancelAction(@Nullable CancelAction cancelAction) {
        this.cancelAction = cancelAction;
    }


    /**
     * Returns the unit whose queue the panel shows.
     *
     * @return the view of the one selected unit, or {@code null} if the panel shows nothing
     */
    public @Nullable UnitView findShownUnit() {
        List<Unit> units = this.selection.getUnits();

        if (units.size() != 1) {
            return null;
        }

        UnitView unit = this.view.getUnitView(units.getFirst());
        Player player = this.view.getLocalPlayer();

        if (unit.getType() == null || player != null && unit.getOwner() != player) {
            return null;
        }

        return unit.getConstructionProgress() < UnitView.COMPLETE || unit.getQueueLength() > 0 ? unit : null;
    }

    /**
     * Returns whether the block shows something now: the panel shows a queue while one unit with a queue or under construction is selected
     * ({@link #findShownUnit}).
     *
     * @return {@code true} if the panel shows a queue, {@code false} otherwise
     */
    @Override
    public boolean isShown() {
        return this.findShownUnit() != null;
    }

    /**
     * Returns the filled slot under a logical position.
     *
     * @param pointerX the logical column
     * @param pointerY the logical row
     * @return the slot, or {@code -1} if the panel shows nothing or the position shows no filled slot
     */
    public int findSlotAt(float pointerX, float pointerY) {
        UnitView unit = this.findShownUnit();

        if (unit == null || !this.contains(pointerX, pointerY)) {
            return -1;
        }

        int filled = getFilledCount(unit);

        this.layOut(getLayoutCount(unit));

        for (int slot = 0; slot < filled; slot++) {
            float left = this.getSlotX(slot);
            float top = this.getSlotY(slot);

            if (pointerX >= left && pointerX < left + this.slotSize && pointerY >= top && pointerY < top + this.slotSize) {
                return slot;
            }
        }

        return -1;
    }

    /**
     * Draws the block: the slots of the shown unit's queue and the progress bar under them, or the waiting bar while the front entry waits
     * for supply, then the tooltip of the hovered filled slot; nothing while no queue is shown.
     *
     * @param canvas the canvas, between its begin and end
     */
    @Override
    public void draw(@NotNull Canvas canvas) {
        UnitView unit = this.findShownUnit();

        if (unit == null) {
            return;
        }

        HudSkin skin = this.getSkin();
        ButtonStyle style = skin.getCommandButton();
        boolean construction = unit.getConstructionProgress() < UnitView.COMPLETE;
        boolean waiting = !construction && unit.isWaitingForSupply();
        int filled = getFilledCount(unit);
        int count = getSlotCount(unit);
        int hoveredSlot = -1;

        this.layOut(getLayoutCount(unit));

        float size = this.slotSize;

        for (int slot = 0; slot < count; slot++) {
            float left = this.getSlotX(slot);
            float top = this.getSlotY(slot);

            if (slot >= filled) {
                style.getDisabled().draw(canvas, left, top, size, size);

                continue;
            }

            boolean hovered = this.pointerX >= left && this.pointerX < left + size && this.pointerY >= top && this.pointerY < top + size;
            UnitType type = construction ? unit.getType() : unit.getQueuedType(slot);
            boolean dimmed = waiting && slot == 0;

            style.findStyle(true, false, hovered).draw(canvas, left, top, size, size);

            if (type != null) {
                this.drawEntry(canvas, unit, type, left, top, size, dimmed ? DIMMED_COLOR : PictureRenderer.OPAQUE_WHITE);
            }

            if (hovered) {
                hoveredSlot = slot;
            }
        }

        StatusBarUtils.drawBar(canvas, this.getSlotX(0), this.getGridTop() + this.getGridHeight() + BAR_GAP, this.getGridWidth(), BAR_HEIGHT,
                               construction ? unit.getConstructionProgress() : unit.getQueueProgress(), UnitView.COMPLETE,
                               PROGRESS_COLOR, waiting ? WAITING_BAR_COLOR : StatusBarUtils.EMPTY_COLOR);

        if (hoveredSlot >= 0) {
            this.drawTooltip(canvas, unit, construction, waiting, hoveredSlot);
        }
    }

    /**
     * Draws the tooltip of a filled slot over the panel's top edge, its left side at the slot's left side: the unit type's name and, for a
     * front entry that waits for supply, the missing supply.
     */
    private void drawTooltip(@NotNull Canvas canvas, @NotNull UnitView unit, boolean construction, boolean waiting, int slot) {
        UnitType type = construction ? unit.getType() : unit.getQueuedType(slot);
        Tooltip shown = this.tooltip;

        shown.clear();

        if (type != null) {
            shown.addLine(this.typeNames.apply(type), TEXT_COLOR);
        }

        if (waiting && slot == 0) {
            shown.addLine(this.waitingText, WAITING_TEXT_COLOR);
        }

        shown.draw(canvas, this.getSkin(), this.getSlotX(slot), this.getY());
    }

    /**
     * Draws the picture of one entry fitted into its slot: the unit type's icon, else its portrait in the owner's team colour, else its name;
     * the picture multiplied with a colour, the name drawn in it unless the colour is white, for example dimmed.
     */
    private void drawEntry(@NotNull Canvas canvas, @NotNull UnitView unit, @NotNull UnitType type, float left, float top, float size, int color) {
        HudSkin skin = this.getSkin();
        Picture icon = skin.findUnitIcon(type);
        Picture portrait = icon == null ? skin.findPortrait(type) : null;
        Player owner = unit.getOwner();
        float inner = size - 2 * ICON_INSET;

        if (icon != null) {
            skin.drawPicture(canvas, icon, left + ICON_INSET, top + ICON_INSET, inner, inner, PictureRenderer.NO_TEAM_COLOR, color);
        } else if (portrait != null) {
            skin.drawPicture(canvas, portrait, left + ICON_INSET, top + ICON_INSET, inner, inner,
                             owner != null ? this.view.getTeamColor(owner) : PictureRenderer.NO_TEAM_COLOR, color);
        } else {
            canvas.setClip((int) left, (int) top, (int) size, (int) size);
            this.font.draw(canvas, this.typeNames.apply(type), left + ICON_INSET, top + (size - this.font.getLineHeight()) / 2,
                           color == PictureRenderer.OPAQUE_WHITE ? TEXT_COLOR : color);
            canvas.setClip(0, 0, -1, -1);
        }
    }

    /**
     * Handles an input event: while a queue is shown, a left press on a filled slot cancels it and other presses inside are taken; while none
     * is shown, the panel takes nothing. A pointer move is remembered for the hovered slot and not taken.
     *
     * @param event the event
     * @return {@code true} if the panel took the event, {@code false} otherwise
     */
    @Override
    public boolean handleInput(@NotNull InputEvent event) {
        if (event.getType() == InputEvent.Type.POINTER_MOVED) {
            this.pointerX = event.getX();
            this.pointerY = event.getY();
        }

        UnitView unit = this.findShownUnit();

        if (unit == null) {
            return false;
        }

        if (event.getType() == InputEvent.Type.BUTTON_PRESSED && event.getButton() == PointerButton.LEFT) {
            int slot = this.findSlotAt(event.getX(), event.getY());
            CancelAction action = this.cancelAction;

            if (slot >= 0 && action != null) {
                action.cancelSlot(this.translator, unit.getUnit(), slot);

                return true;
            }
        }

        return super.handleInput(event);
    }

    /**
     * Lays the slots out for a number of slots: the row count, from one row up to one slot per row, whose square slots are the largest that
     * fit the panel's width and its height without the padding and the progress bar.
     */
    private void layOut(int count) {
        float bestSize = -1;
        int bestRows = 1;

        for (int rowCount = 1; rowCount <= count; rowCount++) {
            int columnCount = (count + rowCount - 1) / rowCount;
            float byWidth = (this.getWidth() - 2 * PADDING - (columnCount - 1) * SLOT_GAP) / columnCount;
            float byHeight = (this.getHeight() - 2 * PADDING - BAR_GAP - BAR_HEIGHT - (rowCount - 1) * SLOT_GAP) / rowCount;
            float size = Math.min(byWidth, byHeight);

            if (size > bestSize) {
                bestSize = size;
                bestRows = rowCount;
            }
        }

        this.columns  = (count + bestRows - 1) / bestRows;
        this.rows     = bestRows;
        this.slotSize = Math.max(bestSize, 0);
    }

    private float getGridWidth() {
        return this.columns * this.slotSize + (this.columns - 1) * SLOT_GAP;
    }

    private float getGridHeight() {
        return this.rows * this.slotSize + (this.rows - 1) * SLOT_GAP;
    }

    /**
     * Returns the top of the rows of slots: the rows and the progress bar under them centred in the panel's height, at least the padding
     * below the panel's top.
     */
    private float getGridTop() {
        return this.getY() + Math.max((this.getHeight() - this.getGridHeight() - BAR_GAP - BAR_HEIGHT) / 2, PADDING);
    }

    private float getSlotX(int slot) {
        return this.getX() + PADDING + slot % this.columns * (this.slotSize + SLOT_GAP);
    }

    private float getSlotY(int slot) {
        return this.getGridTop() + (float) (slot / this.columns) * (this.slotSize + SLOT_GAP);
    }
}

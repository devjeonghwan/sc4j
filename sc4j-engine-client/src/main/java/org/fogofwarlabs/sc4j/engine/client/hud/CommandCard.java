package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.command.CommandAction;
import org.fogofwarlabs.sc4j.engine.client.command.CommandRefusal;
import org.fogofwarlabs.sc4j.engine.client.command.CommandTranslator;
import org.fogofwarlabs.sc4j.engine.client.input.*;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.PictureRenderer;
import org.fogofwarlabs.sc4j.engine.client.skin.ButtonStyle;
import org.fogofwarlabs.sc4j.engine.client.skin.HudSkin;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Shows the command buttons of the selection in a HUD rectangle: the engine default command card. The buttons fill a grid of a fixed column count row
 * by row; each shows its label, and the hovered button shows a {@link Tooltip} in the skin's tooltip box over the card's top edge, above the
 * button: its label, the cost of its action ({@link CommandAction#getCost}, {@link CommandAction#getSupplyCost}) and, while the action is
 * refused ({@link CommandAction#findRefusal}), why, or else what its result will wait for ({@link CommandAction#findWarning}).
 * A left press on a button runs its action through the {@link CommandTranslator} (Shift held queues it), the same as its hotkey, which
 * {@link #setButtons} binds in the translator's key bindings; the translator refuses a refused action. The pending targeted action's button is
 * highlighted, and every button is dimmed while nothing is selected.
 * <p>
 * The card draws in the box of its {@link HudSkin} and each button in the skin's button boxes: disabled while nothing is selected or its action
 * is refused, pressed while its targeted action waits, hovered under the pointer. A button whose action has an icon in the skin
 * ({@link HudSkin#findButtonIcon}) shows the icon fitted into it, dimmed while disabled, instead of the label. Hotkeys work but are not shown.
 */
public final class CommandCard extends HudBlock {
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int COST_COLOR = 0xE8D8A0FF;
    private static final int REFUSAL_COLOR = 0xFF7060FF;
    private static final int WARNING_COLOR = 0xFFC040FF;
    private static final int DIMMED_TEXT_COLOR = 0x808080FF;
    private static final float PADDING = 6;
    private static final float BUTTON_GAP = 4;
    private static final float ICON_INSET = 3;
    private static final @NotNull String COST_SEPARATOR = ", ";

    private final @NotNull CommandTranslator translator;
    private final @NotNull Font font;
    private final @NotNull Function<UnitType, String> typeNames;
    private final @NotNull Function<ResourceKind, String> resourceNames;
    private final @NotNull String supplyName;
    private final int columns;
    private final int rows;

    private final @NotNull List<@NotNull CommandButton> buttons;
    private final @NotNull StringBuilder costText;
    private final @NotNull Tooltip tooltip;

    private float pointerX;
    private float pointerY;

    /**
     * Creates a {@link CommandCard} instance without buttons.
     *
     * @param translator    the translator the buttons run their actions through
     * @param font          the font of the labels and the tooltip
     * @param typeNames     the shown name of each unit type, for the refusals that name one
     * @param resourceNames the shown name of each resource kind, for the costs and the refusals
     * @param supplyName    the shown name of the supply
     * @param columns       the number of button columns
     * @param rows          the number of button rows
     * @throws IllegalArgumentException if a count is not positive
     */
    public CommandCard(@NotNull CommandTranslator translator,
                       @NotNull Font font,
                       @NotNull Function<UnitType, String> typeNames,
                       @NotNull Function<ResourceKind, String> resourceNames,
                       @NotNull String supplyName,
                       int columns,
                       int rows) {
        if (columns <= 0 || rows <= 0) {
            throw Exceptions.newIllegalArgumentException("Invalid command card grid {0}x{1}", columns, rows);
        }

        this.translator    = translator;
        this.font          = font;
        this.typeNames     = typeNames;
        this.resourceNames = resourceNames;
        this.supplyName    = supplyName;
        this.columns       = columns;
        this.rows          = rows;

        this.buttons  = new ArrayList<>();
        this.costText = new StringBuilder();
        this.tooltip  = new Tooltip(font);

        this.pointerX = -1;
        this.pointerY = -1;
    }

    /**
     * @return a copy of the buttons in grid order
     */
    public @NotNull List<@NotNull CommandButton> getButtons() {
        return List.copyOf(this.buttons);
    }

    /**
     * Sets the buttons, at most the grid's cells: removes the hotkey bindings of the replaced buttons that still bind their actions, and binds
     * each new hotkey without modifiers to its action in the translator's key bindings.
     *
     * @param buttons the buttons in grid order
     * @throws IllegalArgumentException if there are more buttons than cells
     */
    public void setButtons(@NotNull List<@NotNull CommandButton> buttons) {
        if (buttons.size() > this.columns * this.rows) {
            throw Exceptions.newIllegalArgumentException("{0} buttons do not fit a {1}x{2} command card", buttons.size(), this.columns, this.rows);
        }

        KeyBindingTable<CommandAction> bindings = this.translator.getBindings();

        for (CommandButton replaced : this.buttons) {
            Key hotkey = replaced.getHotkey();
            KeyBinding<CommandAction> binding = hotkey != null ? bindings.find(hotkey, 0, false) : null;

            if (hotkey != null && binding != null && binding.getAction() == replaced.getAction()) {
                bindings.unbind(hotkey, 0);
            }
        }

        this.buttons.clear();
        this.buttons.addAll(buttons);

        for (CommandButton button : buttons) {
            Key hotkey = button.getHotkey();

            if (hotkey != null) {
                this.translator.getBindings().bind(hotkey, 0, button.getAction(), false);
            }
        }
    }


    /**
     * Returns the button under a framebuffer position.
     *
     * @param pointerX the framebuffer column
     * @param pointerY the framebuffer row
     * @return the button, or {@code null} if the position shows no button
     */
    public @Nullable CommandButton findButtonAt(float pointerX, float pointerY) {
        if (!this.contains(pointerX, pointerY)) {
            return null;
        }

        for (int index = 0; index < this.buttons.size(); index++) {
            float x = this.getButtonX(index);
            float y = this.getButtonY(index);

            if (pointerX >= x && pointerX < x + this.getButtonWidth() && pointerY >= y && pointerY < y + this.getButtonHeight()) {
                return this.buttons.get(index);
            }
        }

        return null;
    }

    /**
     * Draws the block: the background and every button with its icon or its label cut at the button's edges, then the tooltip of the
     * hovered button over the card's top edge, above the button.
     *
     * @param canvas the canvas, between its begin and end
     */
    @Override
    public void draw(@NotNull Canvas canvas) {
        HudSkin skin = this.getSkin();
        ButtonStyle style = skin.getCommandButton();

        skin.getCommandCardBackground().draw(canvas, this.getX(), this.getY(), this.getWidth(), this.getHeight());

        boolean selected = !this.translator.getSelection().isEmpty();
        float width = this.getButtonWidth();
        float height = this.getButtonHeight();
        CommandButton hoveredButton = null;
        CommandRefusal hoveredRefusal = null;
        float hoveredX = 0;

        for (int index = 0; index < this.buttons.size(); index++) {
            CommandButton button = this.buttons.get(index);
            float x = this.getButtonX(index);
            float y = this.getButtonY(index);
            CommandRefusal refusal = selected ? button.getAction().findRefusal(this.translator) : null;
            boolean enabled = selected && refusal == null;
            int color = enabled ? TEXT_COLOR : DIMMED_TEXT_COLOR;
            boolean hovered = this.pointerX >= x && this.pointerX < x + width && this.pointerY >= y && this.pointerY < y + height;
            Picture icon = skin.findButtonIcon(button.getAction());

            if (hovered) {
                hoveredButton  = button;
                hoveredRefusal = refusal;
                hoveredX       = x;
            }

            style.findStyle(enabled, this.translator.getPendingAction() == button.getAction(), hovered).draw(canvas, x, y, width, height);
            canvas.setClip((int) x, (int) y, (int) width, (int) height);

            if (icon != null) {
                skin.drawPicture(canvas, icon, x + ICON_INSET, y + ICON_INSET, width - 2 * ICON_INSET, height - 2 * ICON_INSET,
                                 PictureRenderer.NO_TEAM_COLOR, enabled ? PictureRenderer.OPAQUE_WHITE : DIMMED_TEXT_COLOR);
            } else {
                this.font.draw(canvas, button.getLabel(), x + PADDING, y + (height - this.font.getLineHeight()) / 2, color);
            }

            canvas.setClip(0, 0, -1, -1);
        }

        if (hoveredButton != null) {
            this.drawTooltip(canvas, hoveredButton, hoveredRefusal, hoveredX);
        }
    }

    /**
     * Draws the tooltip of a button over the card's top edge, its left side at the button's left side: the label, the action's cost if it has
     * one, and the refusal if the action is refused, else the warning if its result will wait.
     */
    private void drawTooltip(@NotNull Canvas canvas, @NotNull CommandButton button, @Nullable CommandRefusal refusal, float buttonX) {
        Tooltip shown = this.tooltip;

        shown.clear();
        shown.addLine(button.getLabel(), TEXT_COLOR);
        shown.addLine(this.writeCost(button.getAction()), COST_COLOR);

        CommandRefusal warning = refusal == null && !this.translator.getSelection().isEmpty()
                                 ? button.getAction().findWarning(this.translator)
                                 : null;

        if (refusal != null) {
            shown.addLine(refusal.describe(this.typeNames, this.resourceNames, this.supplyName), REFUSAL_COLOR);
        } else if (warning != null) {
            shown.addLine(warning.describe(this.typeNames, this.resourceNames, this.supplyName), WARNING_COLOR);
        }

        shown.draw(canvas, this.getSkin(), buttonX, this.getY());
    }

    /**
     * Writes the cost of an action, for example {@code 150 Gold, 2 Supply}, into the cost text; an action without cost writes nothing.
     */
    private @NotNull StringBuilder writeCost(@NotNull CommandAction action) {
        ResourceCost cost = action.getCost();
        StringBuilder text = this.costText;

        text.setLength(0);

        for (int index = 0; index < cost.getCount(); index++) {
            if (!text.isEmpty()) {
                text.append(COST_SEPARATOR);
            }

            text.append(cost.getAmount(index)).append(' ').append(this.resourceNames.apply(cost.getKind(index)));
        }

        if (action.getSupplyCost() > 0) {
            if (!text.isEmpty()) {
                text.append(COST_SEPARATOR);
            }

            text.append(action.getSupplyCost()).append(' ').append(this.supplyName);
        }

        return text;
    }

    /**
     * Handles an input event: a left press on a button runs its action, queued with Shift held; other presses inside are taken. A pointer
     * move is remembered for the hovered button and not taken.
     *
     * @param event the event
     * @return {@code true} if the card took the event, {@code false} otherwise
     */
    @Override
    public boolean handleInput(@NotNull InputEvent event) {
        if (event.getType() == InputEvent.Type.POINTER_MOVED) {
            this.pointerX = event.getX();
            this.pointerY = event.getY();
        }

        if (event.getType() == InputEvent.Type.BUTTON_PRESSED && event.getButton() == PointerButton.LEFT) {
            CommandButton button = this.findButtonAt(event.getX(), event.getY());

            if (button != null) {
                this.translator.run(button.getAction(), event.isModifierDown(KeyModifier.SHIFT));

                return true;
            }
        }

        return super.handleInput(event);
    }

    private float getButtonWidth() {
        return (this.getWidth() - 2 * PADDING - (this.columns - 1) * BUTTON_GAP) / this.columns;
    }

    private float getButtonHeight() {
        return (this.getHeight() - 2 * PADDING - (this.rows - 1) * BUTTON_GAP) / this.rows;
    }

    private float getButtonX(int index) {
        return this.getX() + PADDING + index % this.columns * (this.getButtonWidth() + BUTTON_GAP);
    }

    private float getButtonY(int index) {
        return this.getY() + PADDING + (float) (index / this.columns) * (this.getButtonHeight() + BUTTON_GAP);
    }
}

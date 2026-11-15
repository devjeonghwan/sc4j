package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.command.OrderedSelection;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.KeyModifier;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.PictureRenderer;
import org.fogofwarlabs.sc4j.engine.client.skin.BoxStyle;
import org.fogofwarlabs.sc4j.engine.client.skin.HudSkin;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.text.TextCutUtils;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.client.world.StatusBarUtils;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Shows the selected units in a HUD rectangle: the engine default selection panel. One selected unit shows its type name across the top,
 * its portrait below it at the left, and at the right its hit points as text and a bar, each damage layer as its name and points and a
 * bar, its armour, its weapon damage with the unit's kill count ({@link UnitView#getKillCount}) and, while it stands under construction,
 * the construction progress as text and a bar; a unit without hit points (such as a resource container that cannot be attacked) shows no
 * stats. Beside the stats when the panel is wide enough, else under them, follows what the unit does: its current order's text from the
 * {@link OrderTexts} ({@link #setOrderTexts}), with the type name of the order's target unit when the text names it, and the resources it
 * carries ({@code Carrying 8 Gold}) or, for a resource container, holds ({@code 1500 Gold}). Several show grouped by unit type, the types in
 * the order of their first selected unit and each type's units in selection order. When the panel holds every unit as a card, a portrait at
 * least as large as a cell with a column of texts beside it at least {@value #CARD_TEXT_WIDTH} logical units wide, each unit shows as a card
 * in the fewest rows that fit: its portrait as high as the row and beside it its unit type's name, its hit point bar, its hit points and its
 * current order, as many as the row's height holds, each text cut at the card's edge ({@link TextCutUtils}). Otherwise the units show as a
 * grid of cells, each with the unit's portrait in its owner's team colour and its hit point bar; the cells take the skin's slot size, smaller
 * when the selection needs more cells than the panel holds at that size, and the hovered cell shows a {@link Tooltip} over the panel's top
 * edge with its unit type's name and hit points. Hit point bars take the colour of their share ({@link StatusBarUtils#findHitPointColor}). A
 * left press on a cell or card selects that unit alone, with Shift held it removes the unit from the selection.
 * <p>
 * With nothing selected the panel shows the forces of its player ({@link #setOverviewPlayer}, by default the local player): one cell or card per
 * unit type the player owns, in the order of the type's first unit in the game, with the type's portrait and the number of the player's units of
 * it (on a card under the type's name; on a cell at its lower right, the hovered cell's tooltip naming the type); a left press on one selects the
 * player's units of that type, up to the selection's capacity.
 * <p>
 * The panel draws in the box of its {@link HudSkin}, the cells as the skin's slots (team colour squares without a slot box). The portrait of
 * one unit takes at most a third of the panel's width; the texts and bars of one unit take at most 240 logical units across, however wide the
 * panel is, and everything is cut at the panel's edge.
 */
public final class SelectionPanel extends HudBlock {
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int STAT_COLOR = 0xC8D2DCFF;
    private static final int PROGRESS_COLOR = 0xE0B040FF;
    private static final int ACTIVITY_COLOR = 0xF0DC96FF;
    private static final int CELL_ALPHA = 0xC0;
    private static final float PADDING = 6;
    private static final float CELL_GAP = 4;
    private static final float SMALLEST_CELL = 16;
    private static final float PORTRAIT_INSET = 2;
    private static final float BAR_HEIGHT = 6;
    private static final float CELL_BAR_HEIGHT = 4;
    private static final float LINE_GAP = 2;
    private static final float DETAILS_WIDTH = 240;
    private static final float PORTRAIT_SHARE = 3;
    /**
     * The least room right of the stats in logical units that takes the activity lines beside the stats instead of under them.
     */
    private static final float ACTIVITY_WIDTH = 160;
    /**
     * The least width in logical units of the texts beside a card's portrait.
     */
    private static final float CARD_TEXT_WIDTH = 120;
    private static final int PERCENT = 100;
    /**
     * The count of a cell that shows one selected unit rather than a unit type of the overview.
     */
    private static final int NO_COUNT = -1;
    private static final int OVERVIEW_TYPES = 16;
    private static final @NotNull String ARMOR = "Armor ";
    private static final @NotNull String DAMAGE = "Damage ";
    private static final @NotNull String KILLS = "   Kills ";
    private static final @NotNull String BUILDING = "Building ";
    private static final @NotNull String CARRYING = "Carrying ";

    private final @NotNull GameView view;
    private final @NotNull OrderedSelection selection;
    private final @NotNull Font font;
    private final @NotNull Function<UnitType, String> typeNames;
    private final @NotNull Function<ResourceKind, String> resourceNames;
    private final @NotNull StringBuilder text;
    private final @NotNull Tooltip tooltip;
    private final @NotNull List<@NotNull Unit> cells;

    private final @NotNull List<@NotNull Unit> overview;
    private final @NotNull List<@NotNull Unit> overviewSelection;
    private int @NotNull [] overviewCounts;

    private @Nullable OrderTexts orderTexts;
    private @Nullable Player overviewPlayer;
    private float pointerX;
    private float pointerY;

    private float cellSize;
    private float cellWidth;
    private int columnCount;

    /**
     * Creates a {@link SelectionPanel} instance without order texts.
     *
     * @param view          the game view
     * @param selection     the local player's selection
     * @param font          the font of the texts
     * @param typeNames     the shown name of each unit type
     * @param resourceNames the shown name of each resource kind
     */
    public SelectionPanel(@NotNull GameView view, @NotNull OrderedSelection selection, @NotNull Font font,
                          @NotNull Function<UnitType, String> typeNames, @NotNull Function<ResourceKind, String> resourceNames) {
        this.view          = view;
        this.selection     = selection;
        this.font          = font;
        this.typeNames     = typeNames;
        this.resourceNames = resourceNames;
        this.text          = new StringBuilder();
        this.tooltip       = new Tooltip(font);
        this.cells         = new ArrayList<>();

        this.overview          = new ArrayList<>();
        this.overviewSelection = new ArrayList<>();
        this.overviewCounts    = new int[OVERVIEW_TYPES];

        this.orderTexts     = null;
        this.overviewPlayer = null;
        this.pointerX       = -1;
        this.pointerY       = -1;

        this.cellSize    = 0;
        this.cellWidth   = 0;
        this.columnCount = 1;
    }

    /**
     * @return the texts of the current orders, or {@code null} if the panel shows no order
     */
    public @Nullable OrderTexts getOrderTexts() {
        return this.orderTexts;
    }

    /**
     * Sets the texts of the current order of one selected unit.
     *
     * @param orderTexts the order texts, or {@code null} to show no order
     */
    public void setOrderTexts(@Nullable OrderTexts orderTexts) {
        this.orderTexts = orderTexts;
    }

    /**
     * @return the player whose forces the panel shows while nothing is selected, or {@code null} for the local player
     */
    public @Nullable Player getOverviewPlayer() {
        return this.overviewPlayer;
    }

    /**
     * Sets the player whose forces the panel shows while nothing is selected, for example the viewed player of a replay.
     *
     * @param player the player, or {@code null} for the local player
     */
    public void setOverviewPlayer(@Nullable Player player) {
        this.overviewPlayer = player;
    }

    /**
     * Returns the selected unit of the cell under a framebuffer position.
     *
     * @param pointerX the framebuffer column
     * @param pointerY the framebuffer row
     * @return the unit, or {@code null} if the position shows no cell or card (or one unit is selected)
     */
    public @Nullable Unit findUnitAt(float pointerX, float pointerY) {
        List<Unit> units = this.arrangeCells();

        return units.size() > 1 ? this.findCellAt(units, pointerX, pointerY) : null;
    }

    /**
     * Returns the unit of the cell under a framebuffer position among the panel's cells laid out for their number.
     */
    private @Nullable Unit findCellAt(@NotNull List<@NotNull Unit> units, float pointerX, float pointerY) {
        if (units.isEmpty() || !this.contains(pointerX, pointerY)) {
            return null;
        }

        this.layOutCells(units.size());

        float pitchX = this.cellWidth + CELL_GAP;
        float pitchY = this.cellSize + CELL_GAP;
        int columns = this.columnCount;
        int column = (int) Math.floor((pointerX - this.getX() - PADDING) / pitchX);
        int row = (int) Math.floor((pointerY - this.getY() - PADDING) / pitchY);

        if (column < 0 || column >= columns || row < 0 || pointerX - this.getX() - PADDING - column * pitchX >= this.cellWidth ||
            pointerY - this.getY() - PADDING - row * pitchY >= this.cellSize) {
            return null;
        }

        int index = row * columns + column;

        return index < units.size() ? units.get(index) : null;
    }

    /**
     * Draws the block: the background and the one unit's details, the cards or the cells (the overview of the player's forces while nothing is
     * selected), then the tooltip of the hovered cell.
     *
     * @param canvas the canvas, between its begin and end
     */
    @Override
    public void draw(@NotNull Canvas canvas) {
        HudSkin skin = this.getSkin();

        skin.getSelectionPanelBackground().draw(canvas, this.getX(), this.getY(), this.getWidth(), this.getHeight());

        List<Unit> units = this.selection.getUnits();

        if (units.isEmpty()) {
            this.drawOverview(canvas);

            return;
        }

        canvas.setClip((int) this.getX(), (int) this.getY(), (int) this.getWidth(), (int) this.getHeight());

        if (units.size() == 1) {
            this.drawDetails(canvas, this.view.getUnitView(units.getFirst()));
        } else {
            this.drawCells(canvas, this.arrangeCells(), null);
        }

        canvas.setClip(0, 0, -1, -1);

        Unit hovered = this.findUnitAt(this.pointerX, this.pointerY);

        if (hovered != null && this.cellWidth == this.cellSize) {
            this.drawTooltip(canvas, this.view.getUnitView(hovered));
        }
    }

    /**
     * Draws the overview of the player's forces while nothing is selected, then the tooltip of a hovered cell: the unit type's name and the number
     * of the player's units of it.
     */
    private void drawOverview(@NotNull Canvas canvas) {
        List<Unit> types = this.collectOverview();

        if (types.isEmpty()) {
            return;
        }

        canvas.setClip((int) this.getX(), (int) this.getY(), (int) this.getWidth(), (int) this.getHeight());
        this.drawCells(canvas, types, this.overviewCounts);
        canvas.setClip(0, 0, -1, -1);

        Unit hovered = this.findCellAt(types, this.pointerX, this.pointerY);
        UnitType type = hovered != null ? this.view.getUnitView(hovered).getType() : null;

        if (type != null && this.cellWidth == this.cellSize) {
            Tooltip shown = this.tooltip;

            shown.clear();
            shown.addLine(this.typeNames.apply(type), TEXT_COLOR);
            this.text.setLength(0);
            this.text.append(this.overviewCounts[types.indexOf(hovered)]);
            shown.addLine(this.text, STAT_COLOR);
            shown.draw(canvas, this.getSkin(), this.pointerX, this.getY());
        }
    }

    /**
     * Collects the overview of the player's forces: the first unit of each unit type the player owns, in the order of the game's units, and in
     * {@link #overviewCounts} the number of the player's units of each.
     *
     * @return the first unit of each type, a list the panel reuses; empty without player
     */
    private @NotNull List<@NotNull Unit> collectOverview() {
        Player player = this.overviewPlayer != null ? this.overviewPlayer : this.view.getLocalPlayer();
        List<Unit> types = this.overview;

        types.clear();

        if (player == null) {
            return types;
        }

        List<Unit> units = this.view.getUnits();

        for (int index = 0; index < units.size(); index++) {
            Unit unit = units.get(index);
            UnitView unitView = this.view.getUnitView(unit);
            UnitType type = unitView.getType();

            if (unitView.getOwner() != player || type == null) {
                continue;
            }

            int typeIndex = this.findOverviewType(type);

            if (typeIndex >= 0) {
                this.overviewCounts[typeIndex]++;
            } else {
                if (types.size() == this.overviewCounts.length) {
                    this.overviewCounts = Arrays.copyOf(this.overviewCounts, this.overviewCounts.length * 2);
                }

                this.overviewCounts[types.size()] = 1;
                types.add(unit);
            }
        }

        return types;
    }

    private int findOverviewType(@NotNull UnitType type) {
        for (int index = 0; index < this.overview.size(); index++) {
            if (this.view.getUnitView(this.overview.get(index)).getType() == type) {
                return index;
            }
        }

        return -1;
    }

    /**
     * Draws the tooltip of a hovered cell over the panel's top edge, its left side at the pointer: the unit type's name and the hit points.
     */
    private void drawTooltip(@NotNull Canvas canvas, @NotNull UnitView unitView) {
        UnitType type = unitView.getType();
        Tooltip shown = this.tooltip;

        shown.clear();

        if (type != null) {
            shown.addLine(this.typeNames.apply(type), TEXT_COLOR);
        }

        this.text.setLength(0);
        this.text.append(unitView.getHitPoints()).append('/').append(unitView.getMaximumHitPoints());
        shown.addLine(this.text, STAT_COLOR);
        shown.draw(canvas, this.getSkin(), this.pointerX, this.getY());
    }

    /**
     * Arranges the selected units into the panel's cells: grouped by unit type, the types in the order of their first selected unit and each
     * type's units in selection order.
     *
     * @return the units of the cells in their order, a list the panel reuses
     */
    private @NotNull List<@NotNull Unit> arrangeCells() {
        List<Unit> units = this.selection.getUnits();
        List<Unit> arranged = this.cells;

        arranged.clear();

        for (int index = 0; index < units.size(); index++) {
            Unit unit = units.get(index);

            if (arranged.contains(unit)) {
                continue;
            }

            UnitType type = this.view.getUnitView(unit).getType();

            for (int other = index; other < units.size(); other++) {
                Unit candidate = units.get(other);

                if (candidate == unit || this.view.getUnitView(candidate).getType() == type && !arranged.contains(candidate)) {
                    arranged.add(candidate);
                }
            }
        }

        return arranged;
    }

    /**
     * Lays the cells out for a number of selected units: cards in the fewest rows in which every unit gets a card whose portrait is at least the
     * cell size and whose texts get at least {@value #CARD_TEXT_WIDTH} logical units, else square cells of {@link #getCellSize}.
     */
    private void layOutCells(int count) {
        float size = this.getCellSize(count);
        float innerWidth = this.getWidth() - 2 * PADDING;
        float innerHeight = this.getHeight() - 2 * PADDING;

        this.cellSize    = size;
        this.cellWidth   = size;
        this.columnCount = this.getColumnCount(size);

        for (int rows = 1; rows <= count; rows++) {
            float height = (innerHeight - (rows - 1) * CELL_GAP) / rows;

            if (height < size) {
                return;
            }

            int columns = (count + rows - 1) / rows;
            float width = Math.min((innerWidth - (columns - 1) * CELL_GAP) / columns, height + PADDING + DETAILS_WIDTH);

            if (width - height - PADDING >= CARD_TEXT_WIDTH) {
                this.cellSize    = height;
                this.cellWidth   = width;
                this.columnCount = columns;

                return;
            }
        }
    }

    /**
     * Draws the cells or cards of units: selected units with their hit points and orders, or with counts the overview's unit types.
     */
    private void drawCells(@NotNull Canvas canvas, @NotNull List<@NotNull Unit> units, int @Nullable [] counts) {
        this.layOutCells(units.size());

        float cellSize = this.cellSize;
        float cellWidth = this.cellWidth;
        float pitchX = cellWidth + CELL_GAP;
        float pitchY = cellSize + CELL_GAP;
        int columns = this.columnCount;

        for (int index = 0; index < units.size(); index++) {
            UnitView unitView = this.view.getUnitView(units.get(index));
            float x = this.getX() + PADDING + index % columns * pitchX;
            float y = this.getY() + PADDING + (float) (index / columns) * pitchY;

            if (y + cellSize > this.getY() + this.getHeight()) {
                break;
            }

            int count = counts != null ? counts[index] : NO_COUNT;

            this.drawSlot(canvas, unitView, x, y, cellSize);

            if (cellWidth > cellSize) {
                this.drawCardTexts(canvas, unitView, count, x + cellSize + PADDING, y, cellWidth - cellSize - PADDING, y + cellSize);
            } else if (count != NO_COUNT) {
                this.text.setLength(0);
                this.text.append(count);
                this.font.draw(canvas, this.text, x + cellSize - PORTRAIT_INSET - this.font.measure(this.text),
                               y + cellSize - PORTRAIT_INSET - this.font.getLineHeight(), TEXT_COLOR);
            } else {
                StatusBarUtils.drawBar(canvas, x, y + cellSize - CELL_BAR_HEIGHT, cellSize, CELL_BAR_HEIGHT,
                                       unitView.getHitPoints(), unitView.getMaximumHitPoints(),
                                       StatusBarUtils.findHitPointColor(unitView.getHitPoints(), unitView.getMaximumHitPoints()),
                                       StatusBarUtils.EMPTY_COLOR);
            }
        }
    }

    /**
     * Draws the texts of a card beside its portrait from the top down, as many as fit above the card's bottom: the unit type's name, then for an
     * overview's unit type the number of the player's units of it, else the hit point bar, the hit points and the current order, each text cut
     * at the card's edge.
     */
    private void drawCardTexts(@NotNull Canvas canvas, @NotNull UnitView unitView, int count, float x, float y, float width, float bottom) {
        UnitType type = unitView.getType();
        int hitPoints = unitView.getHitPoints();
        int maximumHitPoints = unitView.getMaximumHitPoints();
        float lineHeight = this.font.getLineHeight() + LINE_GAP;
        float lineY = y;

        if (type != null) {
            this.text.setLength(0);
            this.text.append(this.typeNames.apply(type));
            TextCutUtils.cut(this.font, this.text, width);
            this.font.draw(canvas, this.text, x, lineY, TEXT_COLOR);
            lineY += lineHeight;
        }

        if (count != NO_COUNT) {
            if (lineY + this.font.getLineHeight() <= bottom) {
                this.text.setLength(0);
                this.text.append(count);
                this.font.draw(canvas, this.text, x, lineY, STAT_COLOR);
            }

            return;
        }

        if (maximumHitPoints > 0 && lineY + BAR_HEIGHT <= bottom) {
            StatusBarUtils.drawBar(canvas, x, lineY, width, BAR_HEIGHT, hitPoints, maximumHitPoints,
                                   StatusBarUtils.findHitPointColor(hitPoints, maximumHitPoints), StatusBarUtils.EMPTY_COLOR);
            lineY += BAR_HEIGHT + LINE_GAP;

            if (lineY + this.font.getLineHeight() <= bottom) {
                this.text.setLength(0);
                this.text.append(hitPoints).append('/').append(maximumHitPoints);
                this.font.draw(canvas, this.text, x, lineY, STAT_COLOR);
                lineY += lineHeight;
            }
        }

        if (lineY + this.font.getLineHeight() <= bottom && this.appendOrderText(unitView)) {
            TextCutUtils.cut(this.font, this.text, width);
            this.font.draw(canvas, this.text, x, lineY, ACTIVITY_COLOR);
        }
    }

    /**
     * Draws the slot of a unit: the skin's slot, or without one a square in the owner's team colour, and over it the unit type's portrait in
     * the owner's team colour when the skin has one.
     */
    private void drawSlot(@NotNull Canvas canvas, @NotNull UnitView unitView, float x, float y, float size) {
        HudSkin skin = this.getSkin();
        BoxStyle slot = skin.getSelectionSlot();
        Player owner = unitView.getOwner();
        UnitType type = unitView.getType();
        Picture portrait = type != null ? skin.findPortrait(type) : null;
        int teamColor = owner != null ? this.view.getTeamColor(owner) : PictureRenderer.NO_TEAM_COLOR;

        if (slot != null) {
            slot.draw(canvas, x, y, size, size);
        } else if (owner != null) {
            canvas.fillRectangle(x, y, size, size, teamColor << Byte.SIZE | CELL_ALPHA);
        }

        if (portrait != null) {
            skin.drawPicture(canvas, portrait, x + PORTRAIT_INSET, y + PORTRAIT_INSET, size - 2 * PORTRAIT_INSET, size - 2 * PORTRAIT_INSET,
                             teamColor, PictureRenderer.OPAQUE_WHITE);
        }
    }

    /**
     * Draws the details of one unit: the name across the top, the portrait below it at the left and the stats at its right.
     */
    private void drawDetails(@NotNull Canvas canvas, @NotNull UnitView unitView) {
        UnitType type = unitView.getType();
        float lineHeight = this.font.getLineHeight() + LINE_GAP;
        float x = this.getX() + PADDING;
        float y = this.getY() + PADDING;
        float right = this.getX() + this.getWidth() - PADDING;

        if (type != null) {
            this.font.draw(canvas, this.typeNames.apply(type), x, y, TEXT_COLOR);
        }

        y += lineHeight;

        if (type != null && this.getSkin().findPortrait(type) != null) {
            float size = Math.max(Math.min(this.getY() + this.getHeight() - PADDING - y, (right - x) / PORTRAIT_SHARE), 0);

            this.drawSlot(canvas, unitView, x, y, size);
            x += size + PADDING;
        }

        float width = Math.max(Math.min(right - x, DETAILS_WIDTH), 0);
        float top = y;
        int hitPoints = unitView.getHitPoints();
        int maximumHitPoints = unitView.getMaximumHitPoints();

        if (maximumHitPoints <= 0) {
            this.drawActivity(canvas, unitView, x, y);

            return;
        }

        this.text.setLength(0);
        this.text.append(hitPoints).append('/').append(maximumHitPoints);
        this.font.draw(canvas, this.text, x, y, TEXT_COLOR);
        y += this.font.getLineHeight();
        StatusBarUtils.drawBar(canvas, x, y, width, BAR_HEIGHT, hitPoints, maximumHitPoints,
                               StatusBarUtils.findHitPointColor(hitPoints, maximumHitPoints), StatusBarUtils.EMPTY_COLOR);
        y += BAR_HEIGHT + LINE_GAP;

        for (int layer = 0; layer < unitView.getDamageLayerCount(); layer++) {
            int layerPoints = unitView.getDamageLayerPoints(layer);
            int maximumLayerPoints = unitView.getMaximumDamageLayerPoints(layer);

            if (maximumLayerPoints <= 0) {
                continue;
            }

            this.text.setLength(0);
            this.text.append(unitView.getDamageLayerName(layer)).append(' ').append(layerPoints).append('/').append(maximumLayerPoints);
            this.font.draw(canvas, this.text, x, y, TEXT_COLOR);
            y += this.font.getLineHeight();
            StatusBarUtils.drawBar(canvas, x, y, width, BAR_HEIGHT, layerPoints, maximumLayerPoints, StatusBarUtils.DAMAGE_LAYER_COLOR,
                                   StatusBarUtils.EMPTY_COLOR);
            y += BAR_HEIGHT + LINE_GAP;
        }

        if (unitView.getConstructionProgress() < UnitView.COMPLETE) {
            this.text.setLength(0);
            this.text.append(BUILDING).append(unitView.getConstructionProgress() * PERCENT / UnitView.COMPLETE).append('%');
            this.font.draw(canvas, this.text, x, y, STAT_COLOR);
            y += this.font.getLineHeight();
            StatusBarUtils.drawBar(canvas, x, y, width, BAR_HEIGHT, unitView.getConstructionProgress(), UnitView.COMPLETE, PROGRESS_COLOR,
                                   StatusBarUtils.EMPTY_COLOR);

            return;
        }

        this.text.setLength(0);
        this.text.append(ARMOR).append(unitView.getArmor());
        this.font.draw(canvas, this.text, x, y, STAT_COLOR);
        y += lineHeight;

        if (unitView.getWeaponDamage() > 0) {
            this.text.setLength(0);
            this.text.append(DAMAGE).append(unitView.getWeaponDamage()).append(KILLS).append(unitView.getKillCount());
            this.font.draw(canvas, this.text, x, y, STAT_COLOR);
            y += lineHeight;
        }

        float besideX = x + width + 2 * PADDING;

        if (right - besideX >= ACTIVITY_WIDTH) {
            this.drawActivity(canvas, unitView, besideX, top);
        } else {
            this.drawActivity(canvas, unitView, x, y);
        }
    }

    /**
     * Draws what one unit does from a position down: its current order's text, then the resources it carries or holds.
     */
    private void drawActivity(@NotNull Canvas canvas, @NotNull UnitView unitView, float x, float y) {
        float lineY = y;

        if (this.appendOrderText(unitView)) {
            this.font.draw(canvas, this.text, x, lineY, ACTIVITY_COLOR);
            lineY += this.font.getLineHeight() + LINE_GAP;
        }

        ResourceKind carriedKind = unitView.getCarriedKind();
        ResourceKind heldKind = unitView.getResourceKind();

        this.text.setLength(0);

        if (carriedKind != null && unitView.getCarriedAmount() > 0) {
            this.text.append(CARRYING).append(unitView.getCarriedAmount()).append(' ').append(this.resourceNames.apply(carriedKind));
        } else if (heldKind != null && unitView.getResourceAmount() > 0) {
            this.text.append(unitView.getResourceAmount()).append(' ').append(this.resourceNames.apply(heldKind));
        }

        if (!this.text.isEmpty()) {
            this.font.draw(canvas, this.text, x, lineY, STAT_COLOR);
        }
    }

    /**
     * Puts the text of a unit's current order into the panel's text: the order's text from the {@link OrderTexts}, followed by the type name of
     * the order's target unit when the text names it.
     *
     * @return {@code true} if the unit's order has a text, {@code false} otherwise (the panel's text is then unchanged)
     */
    private boolean appendOrderText(@NotNull UnitView unitView) {
        OrderTexts texts = this.orderTexts;
        OrderType orderType = unitView.getOrderType();
        String orderText = texts != null ? texts.findText(orderType) : null;

        if (texts == null || orderType == null || orderText == null) {
            return false;
        }

        Unit target = unitView.getOrderTarget();
        UnitType targetType = target != null && texts.isTargetNamed(orderType) ? this.view.getUnitView(target).getType() : null;

        this.text.setLength(0);
        this.text.append(orderText);

        if (targetType != null) {
            this.text.append(' ').append(this.typeNames.apply(targetType));
        }

        return true;
    }

    /**
     * Handles an input event: a left press on a cell or card selects its unit alone, or removes it with Shift held; while nothing is selected a
     * press on a unit type of the overview selects the player's units of it; other presses inside are taken. A pointer move is remembered for
     * the hovered cell and not taken.
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

        if (event.getType() == InputEvent.Type.BUTTON_PRESSED && event.getButton() == PointerButton.LEFT) {
            if (this.selection.isEmpty()) {
                Unit first = this.findCellAt(this.collectOverview(), event.getX(), event.getY());

                if (first != null) {
                    this.selectType(first);

                    return true;
                }
            }

            Unit unit = this.findUnitAt(event.getX(), event.getY());

            if (unit != null) {
                if (event.isModifierDown(KeyModifier.SHIFT)) {
                    this.selection.remove(unit);
                } else {
                    this.selection.select(unit);
                }

                return true;
            }
        }

        return super.handleInput(event);
    }

    /**
     * Selects the units of a unit's owner and type in the order of the game's units, up to the selection's capacity.
     */
    private void selectType(@NotNull Unit first) {
        UnitView firstView = this.view.getUnitView(first);
        List<Unit> units = this.view.getUnits();
        List<Unit> selected = this.overviewSelection;

        selected.clear();

        for (int index = 0; index < units.size(); index++) {
            UnitView unitView = this.view.getUnitView(units.get(index));

            if (unitView.getOwner() == firstView.getOwner() && unitView.getType() == firstView.getType()) {
                selected.add(units.get(index));
            }
        }

        this.selection.select(selected);
    }

    /**
     * Returns the side of a cell for a number of selected units: the skin's slot size if the panel holds that many cells of it, else the
     * largest size, at least {@value #SMALLEST_CELL}, at which it holds them.
     */
    private float getCellSize(int count) {
        float size = this.getSkin().getSlotSize();

        while (size > SMALLEST_CELL && this.getColumnCount(size) * this.getRowCount(size) < count) {
            size--;
        }

        return size;
    }

    private int getColumnCount(float cellSize) {
        return Math.max((int) ((this.getWidth() - 2 * PADDING + CELL_GAP) / (cellSize + CELL_GAP)), 1);
    }

    private int getRowCount(float cellSize) {
        return Math.max((int) ((this.getHeight() - 2 * PADDING + CELL_GAP) / (cellSize + CELL_GAP)), 1);
    }
}

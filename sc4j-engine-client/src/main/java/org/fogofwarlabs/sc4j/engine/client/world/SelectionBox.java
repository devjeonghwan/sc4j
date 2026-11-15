package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.command.OrderedSelection;
import org.fogofwarlabs.sc4j.engine.client.command.SelectionFilter;
import org.fogofwarlabs.sc4j.engine.client.command.SelectionRules;
import org.fogofwarlabs.sc4j.engine.client.command.StandardSelectionRules;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.KeyModifier;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.QuadrilateralUtils;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Selects units with the pointer in the world view, the engine default of RTS selection, by a game's {@link SelectionRules} through a
 * {@link SelectionFilter} (by default {@link StandardSelectionRules}):
 * <ul>
 * <li>a left press and release without dragging selects the unit the player sees there alone if the rules list it; with Shift held it removes
 * a selected unit, or adds one when the filter merges it into the selection;</li>
 * <li>a double press selects the clicked unit first, then the shown units of its kind ({@link SelectionFilter#filterType});</li>
 * <li>a left drag beyond {@link #DRAG_DISTANCE} pixels draws a box; on release the filter chooses from the units that overlap it
 * ({@link SelectionFilter#filterBox}: the units that join a multiple selection, else one other unit alone), Shift held merges them into the
 * selection; a box without such units keeps the selection.</li>
 * </ul>
 * Each unit stands for its main unit. The box is in framebuffer pixels while it is drawn ({@link #isDragging}, {@link #getLeft} and the other
 * edges). A unit overlaps the box when the box of its footprint outline on the screen does, so the box selects what it shows in every
 * projection.
 */
public final class SelectionBox {
    /**
     * The pointer distance in framebuffer pixels from which a left press becomes a box.
     */
    public static final float DRAG_DISTANCE = 4;

    private final @NotNull GameView view;
    private final @NotNull WorldPicker picker;
    private final @NotNull OrderedSelection selection;
    private final @NotNull SelectionRules<Unit> rules;
    private final @NotNull SelectionFilter<Unit> filter;

    private final @NotNull List<@NotNull Unit> found;
    private final float @NotNull [] corners;

    private boolean pressed;
    private boolean dragging;
    private float startX;
    private float startY;
    private float endX;
    private float endY;

    /**
     * Creates a {@link SelectionBox} instance that selects by the {@link StandardSelectionRules}.
     *
     * @param view      the game view
     * @param picker    the picker of the world view, which converts framebuffer positions to the world
     * @param selection the local player's selection
     */
    public SelectionBox(@NotNull GameView view, @NotNull WorldPicker picker, @NotNull OrderedSelection selection) {
        this(view, picker, selection, new StandardSelectionRules(view));
    }

    /**
     * Creates a {@link SelectionBox} instance.
     *
     * @param view      the game view
     * @param picker    the picker of the world view, which converts framebuffer positions to the world
     * @param selection the local player's selection
     * @param rules     the selection rules of the game
     */
    public SelectionBox(@NotNull GameView view, @NotNull WorldPicker picker, @NotNull OrderedSelection selection,
                        @NotNull SelectionRules<Unit> rules) {
        this.view      = view;
        this.picker    = picker;
        this.selection = selection;
        this.rules     = rules;
        this.filter    = new SelectionFilter<>(selection.getCapacity(), rules);

        this.found   = new ArrayList<>();
        this.corners = new float[WorldProjection.OUTLINE_LENGTH];

        this.pressed  = false;
        this.dragging = false;
        this.startX   = 0;
        this.startY   = 0;
        this.endX     = 0;
        this.endY     = 0;
    }

    /**
     * @return {@code true} if a box is being dragged, {@code false} otherwise
     */
    public boolean isDragging() {
        return this.dragging;
    }

    /**
     * @return the framebuffer column of the box's left edge
     */
    public float getLeft() {
        return Math.min(this.startX, this.endX);
    }

    /**
     * @return the framebuffer row of the box's top edge
     */
    public float getTop() {
        return Math.min(this.startY, this.endY);
    }

    /**
     * @return the framebuffer column of the box's right edge
     */
    public float getRight() {
        return Math.max(this.startX, this.endX);
    }

    /**
     * @return the framebuffer row of the box's bottom edge
     */
    public float getBottom() {
        return Math.max(this.startY, this.endY);
    }


    /**
     * Handles an input event: left presses in the world view, pointer moves while pressed and the left release.
     *
     * @param event the event
     * @return {@code true} if the event pressed, dragged or released the selection, {@code false} otherwise
     */
    public boolean handleInput(@NotNull InputEvent event) {
        switch (event.getType()) {
            case BUTTON_PRESSED -> {
                if (event.getButton() != PointerButton.LEFT || !this.picker.contains(event.getX(), event.getY())) {
                    return false;
                }

                if (event.isDoublePress()) {
                    this.selectType(event.getX(), event.getY());

                    return true;
                }

                this.pressed  = true;
                this.dragging = false;
                this.startX   = event.getX();
                this.startY   = event.getY();
                this.endX     = event.getX();
                this.endY     = event.getY();

                return true;
            }
            case POINTER_MOVED -> {
                if (!this.pressed) {
                    return false;
                }

                this.endX     = event.getX();
                this.endY     = event.getY();
                this.dragging = this.dragging ||
                                Math.abs(this.endX - this.startX) >= DRAG_DISTANCE || Math.abs(this.endY - this.startY) >= DRAG_DISTANCE;

                return true;
            }
            case BUTTON_RELEASED -> {
                if (event.getButton() != PointerButton.LEFT || !this.pressed) {
                    return false;
                }

                boolean adding = event.isModifierDown(KeyModifier.SHIFT);

                if (this.dragging) {
                    this.selectBox(adding);
                } else {
                    this.selectPoint(this.startX, this.startY, adding);
                }

                this.pressed  = false;
                this.dragging = false;

                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Drops a pressed or dragged box without selecting.
     */
    public void cancel() {
        this.pressed  = false;
        this.dragging = false;
    }

    private void selectPoint(float x, float y, boolean toggling) {
        Unit unit = this.findListedUnit(x, y);

        if (unit == null) {
            if (!toggling) {
                this.selection.clear();
            }

            return;
        }

        if (!toggling || this.selection.isEmpty()) {
            this.selection.select(unit);

            return;
        }

        if (!this.selection.remove(unit)) {
            this.selection.select(this.filter.merge(List.copyOf(this.selection.getUnits()), List.of(unit)));
        }
    }

    private void selectBox(boolean adding) {
        this.findShownUnits(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());

        List<Unit> chosen = this.filter.filterBox(this.found);

        if (chosen.isEmpty()) {
            return;
        }

        if (adding && !this.selection.isEmpty()) {
            this.selection.select(this.filter.merge(List.copyOf(this.selection.getUnits()), chosen));
        } else {
            this.selection.select(chosen);
        }
    }

    private void selectType(float x, float y) {
        Unit unit = this.findListedUnit(x, y);

        if (unit == null) {
            return;
        }

        this.findShownUnits(0, 0, this.picker.getScreenWidth() - 1, this.picker.getScreenHeight() - 1);
        this.selection.select(this.filter.filterType(this.found, unit));
    }

    /**
     * Finds the main unit of the unit the player sees at a framebuffer position, if the rules list it.
     */
    private @Nullable Unit findListedUnit(float x, float y) {
        Unit picked = this.picker.pickUnit(x, y);
        Unit unit = picked != null ? this.rules.getMainUnit(picked) : null;

        return unit != null && this.rules.isListable(unit) ? unit : null;
    }

    /**
     * Collects into {@link #found} the units shown in a framebuffer rectangle, in the view's search order: the units of the world box around
     * the rectangle's four corners are the candidates; a candidate stays if the screen box of its footprint outline overlaps the rectangle.
     */
    private void findShownUnits(float left, float top, float right, float bottom) {
        int topLeftX = this.picker.toWorldX(left, top);
        int topLeftY = this.picker.toWorldY(left, top);
        int topRightX = this.picker.toWorldX(right, top);
        int topRightY = this.picker.toWorldY(right, top);
        int bottomRightX = this.picker.toWorldX(right, bottom);
        int bottomRightY = this.picker.toWorldY(right, bottom);
        int bottomLeftX = this.picker.toWorldX(left, bottom);
        int bottomLeftY = this.picker.toWorldY(left, bottom);

        this.found.clear();
        this.view.findUnits(Math.min(Math.min(topLeftX, topRightX), Math.min(bottomRightX, bottomLeftX)),
                            Math.min(Math.min(topLeftY, topRightY), Math.min(bottomRightY, bottomLeftY)),
                            Math.max(Math.max(topLeftX, topRightX), Math.max(bottomRightX, bottomLeftX)),
                            Math.max(Math.max(topLeftY, topRightY), Math.max(bottomRightY, bottomLeftY)),
                            this.found);
        this.found.removeIf(unit -> !this.isShownInside(this.view.getUnitView(unit), left, top, right, bottom));
    }

    private boolean isShownInside(@NotNull UnitView unitView, float left, float top, float right, float bottom) {
        this.picker.outlineOnScreen(unitView.getLeft(), unitView.getTop(), unitView.getRight() + 1, unitView.getBottom() + 1, this.corners);

        return QuadrilateralUtils.overlaps(this.corners, left, top, right, bottom);
    }
}

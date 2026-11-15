package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes the command card button of one order type for a {@link StandardCommandCardSource}: the order, the order of a target point without
 * a unit if it differs (an attack button orders attack-move there), its label and hotkey, whether it waits for a target, and which units show
 * it: units that are not buildings, or units of a trait (for example gathering for workers).
 */
public final class OrderButton {
    private final @NotNull OrderType orderType;
    private final @Nullable OrderType pointOrderType;
    private final @NotNull String label;
    private final @Nullable Key hotkey;
    private final boolean targeted;
    private final @Nullable UnitTrait requiredTrait;

    /**
     * Creates an {@link OrderButton} instance shown for every selected unit that is not a building.
     *
     * @param orderType the order type
     * @param label     the shown label
     * @param hotkey    the hotkey, or {@code null} for none
     * @param targeted  whether the order waits for a target the player points at
     */
    public OrderButton(@NotNull OrderType orderType, @NotNull String label, @Nullable Key hotkey, boolean targeted) {
        this(orderType, label, hotkey, targeted, null);
    }

    /**
     * Creates an {@link OrderButton} instance with the same order for every target.
     *
     * @param orderType     the order type
     * @param label         the shown label
     * @param hotkey        the hotkey, or {@code null} for none
     * @param targeted      whether the order waits for a target the player points at
     * @param requiredTrait the trait a selected unit needs to show the button, or {@code null} for every unit that is not a building
     */
    public OrderButton(@NotNull OrderType orderType,
                       @NotNull String label,
                       @Nullable Key hotkey,
                       boolean targeted,
                       @Nullable UnitTrait requiredTrait) {
        this(orderType, null, label, hotkey, targeted, requiredTrait);
    }

    /**
     * Creates an {@link OrderButton} instance.
     *
     * @param orderType      the order type
     * @param pointOrderType the order type of a target point without a unit, or {@code null} for the order type
     * @param label          the shown label
     * @param hotkey         the hotkey, or {@code null} for none
     * @param targeted       whether the order waits for a target the player points at
     * @param requiredTrait  the trait a selected unit needs to show the button, or {@code null} for every unit that is not a building
     */
    public OrderButton(@NotNull OrderType orderType,
                       @Nullable OrderType pointOrderType,
                       @NotNull String label,
                       @Nullable Key hotkey,
                       boolean targeted,
                       @Nullable UnitTrait requiredTrait) {
        this.orderType      = orderType;
        this.pointOrderType = pointOrderType;
        this.label          = label;
        this.hotkey         = hotkey;
        this.targeted       = targeted;
        this.requiredTrait  = requiredTrait;
    }

    /**
     * @return the order type
     */
    public @NotNull OrderType getOrderType() {
        return this.orderType;
    }

    /**
     * @return the order type of a target point without a unit, or {@code null} if it is the order type
     */
    public @Nullable OrderType getPointOrderType() {
        return this.pointOrderType;
    }

    /**
     * @return the shown label
     */
    public @NotNull String getLabel() {
        return this.label;
    }

    /**
     * @return the hotkey, or {@code null} for none
     */
    public @Nullable Key getHotkey() {
        return this.hotkey;
    }

    /**
     * @return {@code true} if the order waits for a target the player points at, {@code false} otherwise
     */
    public boolean isTargeted() {
        return this.targeted;
    }

    /**
     * @return the trait a selected unit needs to show the button, or {@code null} for every unit that is not a building
     */
    public @Nullable UnitTrait getRequiredTrait() {
        return this.requiredTrait;
    }
}

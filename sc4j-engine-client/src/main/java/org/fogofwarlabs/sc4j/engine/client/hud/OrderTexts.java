package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the text the selection panel shows for a unit's current order, for example {@code Moving}: per order type a text, and whether the
 * target unit's type name follows it ({@code Attacking} + {@code Grubber}). An order type without a text shows nothing, such as an idle order.
 */
public final class OrderTexts {
    private final @NotNull Map<@NotNull OrderType, @NotNull String> texts;
    private final @NotNull Map<@NotNull OrderType, @NotNull Boolean> targetNamed;

    /**
     * Creates an {@link OrderTexts} instance without texts.
     */
    public OrderTexts() {
        this.texts       = new HashMap<>();
        this.targetNamed = new HashMap<>();
    }

    /**
     * Sets the text of an order type.
     *
     * @param orderType   the order type
     * @param text        the shown text, for example {@code Moving}
     * @param targetNamed whether the type name of the order's target unit follows the text
     */
    public void setText(@NotNull OrderType orderType, @NotNull String text, boolean targetNamed) {
        this.texts.put(orderType, text);
        this.targetNamed.put(orderType, targetNamed);
    }


    /**
     * Finds the text of an order type.
     *
     * @param orderType the order type, or {@code null} for a unit without order
     * @return the text, or {@code null} if the order type shows none
     */
    public @Nullable String findText(@Nullable OrderType orderType) {
        return orderType != null ? this.texts.get(orderType) : null;
    }

    /**
     * Answers whether the type name of an order's target unit follows the order type's text.
     *
     * @param orderType the order type
     * @return {@code true} if the target's name follows the text, {@code false} otherwise or if the order type shows no text
     */
    public boolean isTargetNamed(@NotNull OrderType orderType) {
        return this.targetNamed.getOrDefault(orderType, false);
    }
}

package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Says why a {@link CommandAction} cannot run now ({@link CommandAction#findRefusal}): a missing requirement (with the unit type the player
 * lacks, when the requirement names one), a missing resource, missing supply or a full queue. The command card shows a refused button
 * disabled with the refusal in its tooltip, and the translator does not run a refused action ({@link CommandTranslator#run}).
 */
public final class CommandRefusal {
    /**
     * The refusal of an action whose supply the player lacks.
     */
    public static final @NotNull CommandRefusal MISSING_SUPPLY = new CommandRefusal(Reason.MISSING_SUPPLY, null, null);
    /**
     * The refusal of an action whose producers' queues are full.
     */
    public static final @NotNull CommandRefusal QUEUE_FULL = new CommandRefusal(Reason.QUEUE_FULL, null, null);
    /**
     * The refusal of an action whose requirement is not met and names no unit type.
     */
    public static final @NotNull CommandRefusal NOT_AVAILABLE = new CommandRefusal(Reason.MISSING_REQUIREMENT, null, null);

    private static final @NotNull String NEEDS = "Needs ";
    private static final @NotNull String NOT_ENOUGH = "Not enough ";
    private static final @NotNull String NOT_AVAILABLE_TEXT = "Not available yet";
    private static final @NotNull String QUEUE_FULL_TEXT = "Queue is full";

    /**
     * Names why an action is refused.
     */
    public enum Reason {
        /**
         * A requirement of the action is not met.
         */
        MISSING_REQUIREMENT,
        /**
         * The player lacks a resource of the action's cost.
         */
        MISSING_RESOURCE,
        /**
         * The player lacks the supply of the action's unit.
         */
        MISSING_SUPPLY,
        /**
         * The queues of the producers are full.
         */
        QUEUE_FULL
    }

    /**
     * Creates a {@link CommandRefusal} instance of a requirement that needs completed units of a unit type.
     *
     * @param unitType the unit type the player lacks
     * @return the created {@link CommandRefusal} instance
     */
    public static @NotNull CommandRefusal ofMissingUnit(@NotNull UnitType unitType) {
        return new CommandRefusal(Reason.MISSING_REQUIREMENT, unitType, null);
    }

    /**
     * Creates a {@link CommandRefusal} instance of a resource the player lacks.
     *
     * @param resourceKind the resource kind
     * @return the created {@link CommandRefusal} instance
     */
    public static @NotNull CommandRefusal ofMissingResource(@NotNull ResourceKind resourceKind) {
        return new CommandRefusal(Reason.MISSING_RESOURCE, null, resourceKind);
    }

    private final @NotNull Reason reason;
    private final @Nullable UnitType unitType;
    private final @Nullable ResourceKind resourceKind;

    private CommandRefusal(@NotNull Reason reason, @Nullable UnitType unitType, @Nullable ResourceKind resourceKind) {
        this.reason       = reason;
        this.unitType     = unitType;
        this.resourceKind = resourceKind;
    }

    /**
     * @return why the action is refused
     */
    public @NotNull Reason getReason() {
        return this.reason;
    }

    /**
     * @return the unit type the player lacks completed units of, or {@code null} if the refusal names none
     */
    public @Nullable UnitType getUnitType() {
        return this.unitType;
    }

    /**
     * @return the resource kind the player lacks, or {@code null} if the refusal is not about a resource
     */
    public @Nullable ResourceKind getResourceKind() {
        return this.resourceKind;
    }


    /**
     * Describes the refusal in a short line without a final period, for example {@code Not enough Gold} or {@code Needs Barracks}.
     *
     * @param typeNames     the shown name of each unit type
     * @param resourceNames the shown name of each resource kind
     * @param supplyName    the shown name of the supply
     * @return the line
     */
    public @NotNull String describe(@NotNull Function<UnitType, String> typeNames, @NotNull Function<ResourceKind, String> resourceNames,
                                    @NotNull String supplyName) {
        UnitType missingType = this.unitType;
        ResourceKind missingResource = this.resourceKind;

        return switch (this.reason) {
            case MISSING_REQUIREMENT -> missingType != null ? NEEDS + typeNames.apply(missingType) : NOT_AVAILABLE_TEXT;
            case MISSING_RESOURCE -> NOT_ENOUGH + (missingResource != null ? resourceNames.apply(missingResource) : "");
            case MISSING_SUPPLY -> NOT_ENOUGH + supplyName;
            case QUEUE_FULL -> QUEUE_FULL_TEXT;
        };
    }
}

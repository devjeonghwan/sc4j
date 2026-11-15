package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.economy.EconomySystem;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the generic conditions of the standard trigger system: their type ids and their tests. A condition whose player selector names no
 * player of the game does not hold.
 * <ul>
 *     <li>{@link #ALWAYS} holds; {@link #NEVER} does not.</li>
 *     <li>{@link #ELAPSED_FRAMES}: the game's frame counter compares with the amount.</li>
 *     <li>{@link #UNIT_COUNT}: the number of the player's units of the unit type and trait (any if absent) in the area (the whole map if absent)
 *     compares with the amount ({@link StandardTriggerContext#countUnits}).</li>
 *     <li>{@link #OPPONENTS_DEFEATED}: every opponent of the player lost ({@link StandardTriggerContext#isEveryOpponentDefeated}).</li>
 *     <li>{@link #TIMER}: the frames the timer still counts down compare with the amount.</li>
 *     <li>{@link #RESOURCE_AMOUNT}: the player's amount of the resource kind ({@link EconomySystem#getAmount}) compares with the amount; it does
 *     not hold without resource kind or economy system.</li>
 * </ul>
 */
public final class StandardTriggerConditionUtils {
    /**
     * The type id of the condition that always holds.
     */
    public static final int ALWAYS = 0;
    /**
     * The type id of the condition that never holds.
     */
    public static final int NEVER = 1;
    /**
     * The type id of the condition on the elapsed frames.
     */
    public static final int ELAPSED_FRAMES = 2;
    /**
     * The type id of the condition on the number of a player's units.
     */
    public static final int UNIT_COUNT = 3;
    /**
     * The type id of the condition that every opponent of a player lost.
     */
    public static final int OPPONENTS_DEFEATED = 4;
    /**
     * The type id of the condition on a timer.
     */
    public static final int TIMER = 5;
    /**
     * The type id of the condition on a player's amount of a resource.
     */
    public static final int RESOURCE_AMOUNT = 6;
    /**
     * The number of standard condition type ids; a mod's own conditions take ids from here on.
     */
    public static final int TYPE_COUNT = 7;

    /**
     * Registers the standard conditions in a registry of at least {@link #TYPE_COUNT} types.
     *
     * @param registry the registry
     * @throws IllegalArgumentException if the registry is smaller
     * @throws IllegalStateException    if the registry is frozen
     */
    public static void register(@NotNull TriggerRegistry<TriggerCondition<StandardTriggerContext, ConditionRecord>> registry) {
        registry.register(ALWAYS, (context, condition) -> true);
        registry.register(NEVER, (context, condition) -> false);
        registry.register(ELAPSED_FRAMES, (context, condition) -> condition.getComparison().test(context.getGame().getClock().getFrame(),
                                                                                                 condition.getAmount()));
        registry.register(UNIT_COUNT, StandardTriggerConditionUtils::testUnitCount);
        registry.register(OPPONENTS_DEFEATED, StandardTriggerConditionUtils::testOpponentsDefeated);
        registry.register(TIMER, (context, condition) -> condition.getComparison().test(context.getTimer(condition.getTimer()), condition.getAmount()));
        registry.register(RESOURCE_AMOUNT, StandardTriggerConditionUtils::testResourceAmount);
    }

    private static boolean testUnitCount(@NotNull StandardTriggerContext context, @NotNull ConditionRecord condition) {
        Player player = condition.getPlayer().resolve(context);

        return player != null &&
               condition.getComparison().test(context.countUnits(player, condition.getUnitType(), condition.getUnitTrait(), condition.getArea()),
                                              condition.getAmount());
    }

    private static boolean testOpponentsDefeated(@NotNull StandardTriggerContext context, @NotNull ConditionRecord condition) {
        Player player = condition.getPlayer().resolve(context);

        return player != null && context.isEveryOpponentDefeated(player);
    }

    private static boolean testResourceAmount(@NotNull StandardTriggerContext context, @NotNull ConditionRecord condition) {
        Player player = condition.getPlayer().resolve(context);
        ResourceKind kind = condition.getResourceKind();
        Game game = context.getGame();

        return player != null && kind != null && game.hasSystem(EconomySystem.KEY) &&
               condition.getComparison().test(game.getEconomy().getAmount(player, kind), condition.getAmount());
    }

    private StandardTriggerConditionUtils() {
    }
}

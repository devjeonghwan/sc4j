package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.lifecycle.UnitAccountingUtils;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the generic actions of the standard trigger system: their type ids and their effects. An action whose player selector names no
 * player of the game does nothing and finishes.
 * <ul>
 *     <li>{@link #VICTORY} and {@link #DEFEAT} decide about the player ({@link StandardTriggerContext#decide}).</li>
 *     <li>{@link #DISPLAY_MESSAGE} displays the text to the player ({@link StandardTriggerContext#displayMessage}).</li>
 *     <li>{@link #SET_TIMER} sets the timer to the amount.</li>
 *     <li>{@link #WAIT} waits the amount in frames: the trigger stops at the action and the action finishes in the run after the wait.</li>
 *     <li>{@link #REPEAT_TRIGGER} makes the running trigger run again each time its conditions hold.</li>
 *     <li>{@link #CREATE_UNITS} creates the amount of units of the unit type for the player at the free positions nearest to the center of the
 *     area ({@link StandardProductionSystem#createUnit}); without unit type, area or standard production system it does nothing.</li>
 *     <li>{@link #REMOVE_UNITS} takes the player's units of the unit type (any if absent) in the area (the whole map if absent) out of the game,
 *     not as deaths ({@link UnitAccountingUtils#removeUnit}), in slot order, at most the amount of them ({@link ActionRecord#ALL_UNITS} for
 *     all).</li>
 *     <li>{@link #GIVE_UNITS} gives such units to the target player ({@link UnitAccountingUtils#transferUnit}).</li>
 * </ul>
 */
public final class StandardTriggerActionUtils {
    /**
     * The type id of the action that makes the player win.
     */
    public static final int VICTORY = 0;
    /**
     * The type id of the action that makes the player lose.
     */
    public static final int DEFEAT = 1;
    /**
     * The type id of the action that displays a message.
     */
    public static final int DISPLAY_MESSAGE = 2;
    /**
     * The type id of the action that sets a timer.
     */
    public static final int SET_TIMER = 3;
    /**
     * The type id of the action that waits.
     */
    public static final int WAIT = 4;
    /**
     * The type id of the action that makes the running trigger repeat.
     */
    public static final int REPEAT_TRIGGER = 5;
    /**
     * The type id of the action that creates units.
     */
    public static final int CREATE_UNITS = 6;
    /**
     * The type id of the action that takes units out of the game.
     */
    public static final int REMOVE_UNITS = 7;
    /**
     * The type id of the action that gives units to another player.
     */
    public static final int GIVE_UNITS = 8;
    /**
     * The number of standard action type ids; a mod's own actions take ids from here on.
     */
    public static final int TYPE_COUNT = 9;

    /**
     * The instruction limit of the animation programs a unit action starts.
     */
    private static final int INSTRUCTION_LIMIT = 0;

    /**
     * Registers the standard actions in a registry of at least {@link #TYPE_COUNT} types.
     *
     * @param registry the registry
     * @throws IllegalArgumentException if the registry is smaller
     * @throws IllegalStateException    if the registry is frozen
     */
    public static void register(@NotNull TriggerRegistry<TriggerAction<StandardTriggerContext, RunningTrigger, ActionRecord>> registry) {
        registry.register(VICTORY, (context, trigger, action) -> decide(context, action, PlayerOutcome.VICTORY));
        registry.register(DEFEAT, (context, trigger, action) -> decide(context, action, PlayerOutcome.DEFEAT));
        registry.register(DISPLAY_MESSAGE, StandardTriggerActionUtils::displayMessage);
        registry.register(SET_TIMER, (context, trigger, action) -> {
            context.setTimer(action.getTimer(), action.getAmount());

            return true;
        });
        registry.register(WAIT, StandardTriggerActionUtils::waitFrames);
        registry.register(REPEAT_TRIGGER, (context, trigger, action) -> {
            trigger.setRepeating(true);

            return true;
        });
        registry.register(CREATE_UNITS, StandardTriggerActionUtils::createUnits);
        registry.register(REMOVE_UNITS, (context, trigger, action) -> changeUnits(context, action, false));
        registry.register(GIVE_UNITS, (context, trigger, action) -> changeUnits(context, action, true));
    }

    private static boolean decide(@NotNull StandardTriggerContext context, @NotNull ActionRecord action, @NotNull PlayerOutcome outcome) {
        Player player = action.getPlayer().resolve(context);

        if (player != null) {
            context.decide(player, outcome);
        }

        return true;
    }

    private static boolean displayMessage(@NotNull StandardTriggerContext context, @NotNull RunningTrigger trigger, @NotNull ActionRecord action) {
        Player player = action.getPlayer().resolve(context);

        if (player != null) {
            context.displayMessage(player, action.getText());
        }

        return true;
    }

    private static boolean waitFrames(@NotNull StandardTriggerContext context, @NotNull RunningTrigger trigger, @NotNull ActionRecord action) {
        if (!trigger.isWaiting()) {
            trigger.setWait(true, action.getAmount());
        }

        if (trigger.getWaitRemaining() > 0) {
            return false;
        }

        trigger.setWait(false, 0);

        return true;
    }

    private static boolean createUnits(@NotNull StandardTriggerContext context, @NotNull RunningTrigger trigger, @NotNull ActionRecord action) {
        Player player = action.getPlayer().resolve(context);
        StandardProductionSystem production = context.getProduction();
        UnitType type = action.getUnitType();
        TriggerArea area = action.getArea();

        if (player == null || production == null || type == null || area == null) {
            return true;
        }

        for (int count = 0; count < action.getAmount(); count++) {
            production.createUnit(player, type, area.getCenterX(), area.getCenterY(), INSTRUCTION_LIMIT);
        }

        return true;
    }

    /**
     * Removes or gives away the matching units of the action's player, in slot order, at most the amount of them.
     */
    private static boolean changeUnits(@NotNull StandardTriggerContext context, @NotNull ActionRecord action, boolean give) {
        Player player = action.getPlayer().resolve(context);
        Player targetPlayer = give ? action.getTargetPlayer().resolve(context) : null;

        if (player == null || give && targetPlayer == null) {
            return true;
        }

        Game game = context.getGame();
        EntityPool<Unit> units = game.getUnits();
        SlotList used = units.getUsedSlots();
        int limit = action.getAmount();
        int changed = 0;
        int index = used.getFirst();

        while (index != SlotList.NONE && (limit == ActionRecord.ALL_UNITS || changed < limit)) {
            int next = used.getNext(index);
            Unit unit = units.get(index);

            if (context.matches(unit, player, action.getUnitType(), null, action.getArea())) {
                if (targetPlayer != null) {
                    UnitAccountingUtils.transferUnit(game, unit, targetPlayer, INSTRUCTION_LIMIT);
                } else {
                    UnitAccountingUtils.removeUnit(game, unit, false);
                }

                changed++;
            }

            index = next;
        }

        return true;
    }

    private StandardTriggerActionUtils() {
    }
}

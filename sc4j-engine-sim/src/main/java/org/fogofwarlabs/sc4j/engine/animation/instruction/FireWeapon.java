package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.animation.AnimationContext;
import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.fogofwarlabs.sc4j.engine.animation.AnimationStep;
import org.fogofwarlabs.sc4j.engine.animation.InstructionBudget;
import org.jetbrains.annotations.NotNull;

/**
 * Fires a weapon of the unit of the animated object ({@link org.fogofwarlabs.sc4j.engine.animation.AnimationEffects#fireWeapon})
 * and goes on.
 */
public final class FireWeapon extends AnimationInstruction {
    private final int weaponSlot;

    /**
     * Creates a {@link FireWeapon} instance.
     *
     * @param weaponSlot the weapon slot of the unit, at least {@code 0}
     * @throws IllegalArgumentException if the weapon slot is negative
     */
    public FireWeapon(int weaponSlot) {
        if (weaponSlot < 0) {
            throw Exceptions.newIllegalArgumentException("The weapon slot must not be negative: {0}", weaponSlot);
        }

        this.weaponSlot = weaponSlot;
    }

    /**
     * @return the weapon slot of the unit, {@code 0} for its first weapon
     */
    public int getWeaponSlot() {
        return this.weaponSlot;
    }


    /**
     * Runs this instruction for the animated object of the context.
     * <p>
     * Fires the weapon and continues at the next position.
     *
     * @param context the running animation of one object
     * @return how the program continues
     */
    @Override
    public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
        InstructionBudget budget = context.getInstructionBudget();

        context.getEffects().fireWeapon(context.getImage(), this.weaponSlot, budget == null ? Integer.MAX_VALUE : budget.getRemaining());

        return AnimationStep.CONTINUE;
    }
}

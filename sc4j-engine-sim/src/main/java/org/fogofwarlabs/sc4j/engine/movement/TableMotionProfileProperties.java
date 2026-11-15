package org.fogofwarlabs.sc4j.engine.movement;

import org.fogofwarlabs.sc4j.engine.content.table.MotionProfileTable;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.entity.MovingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Reads the motion profile values of a unit or projectile from the game's motion profile table by its motion profile
 * ({@link MovingEntity#getMotionProfile()}): the engine default {@link MotionProfileProperties}. An object without a motion profile has top
 * speed, acceleration and stopping distance {@code 0} and no accelerating turn.
 */
public final class TableMotionProfileProperties implements MotionProfileProperties<MovingEntity> {
    private final @NotNull MotionProfileTable motionProfiles;

    /**
     * Creates a {@link TableMotionProfileProperties} instance over a motion profile table.
     *
     * @param motionProfiles the motion profile table of the game
     */
    public TableMotionProfileProperties(@NotNull MotionProfileTable motionProfiles) {
        this.motionProfiles = motionProfiles;
    }

    /**
     * @param object the moving object
     * @return the top speed of the object's motion profile, fixed-point with eight fraction bits
     */
    @Override
    public int getTopSpeed(@NotNull MovingEntity object) {
        MotionProfile motionProfile = object.getMotionProfile();

        return motionProfile != null ? this.motionProfiles.getTopSpeed(motionProfile) : 0;
    }

    /**
     * @param object the moving object
     * @return the acceleration of the object's motion profile, fixed-point with eight fraction bits
     */
    @Override
    public int getAcceleration(@NotNull MovingEntity object) {
        MotionProfile motionProfile = object.getMotionProfile();

        return motionProfile != null ? this.motionProfiles.getAcceleration(motionProfile) : 0;
    }

    /**
     * @param object the moving object
     * @return the distance the object's motion profile needs to stop from its top speed, fixed-point with eight fraction bits
     */
    @Override
    public int getStoppingDistance(@NotNull MovingEntity object) {
        MotionProfile motionProfile = object.getMotionProfile();

        return motionProfile != null ? this.motionProfiles.getStoppingDistance(motionProfile) : 0;
    }

    /**
     * @param object the moving object
     * @return {@code true} if the turn rate of the object grows by one each frame it turns, {@code false} otherwise
     */
    @Override
    public boolean isAcceleratingTurn(@NotNull MovingEntity object) {
        MotionProfile motionProfile = object.getMotionProfile();

        return motionProfile != null && this.motionProfiles.isAcceleratingTurn(motionProfile);
    }
}

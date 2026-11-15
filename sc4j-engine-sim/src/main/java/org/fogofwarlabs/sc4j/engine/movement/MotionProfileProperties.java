package org.fogofwarlabs.sc4j.engine.movement;

import org.jetbrains.annotations.NotNull;

/**
 * Reads the motion profile values of a moving object that a motion model ({@link MotionModel}) needs: the type's top speed, acceleration and
 * stopping distance from top speed, and whether its turn rate grows while it turns. The engine default {@link TableMotionProfileProperties}
 * reads the motion profile table by the motion profile of an engine {@link org.fogofwarlabs.sc4j.engine.entity.Unit}; a mod whose objects
 * store their motion profile otherwise supplies its own.
 * <p>
 * Determinism contract: a read depends on the object and the game content only, changes nothing and allocates nothing.
 *
 * @param <T> the class of the moving objects
 */
public interface MotionProfileProperties<T> {
    /**
     * @param object the moving object
     * @return the top speed of the object's motion profile, fixed-point with eight fraction bits
     */
    int getTopSpeed(@NotNull T object);

    /**
     * @param object the moving object
     * @return the acceleration of the object's motion profile, fixed-point with eight fraction bits
     */
    int getAcceleration(@NotNull T object);

    /**
     * @param object the moving object
     * @return the distance the object's motion profile needs to stop from its top speed, fixed-point with eight fraction bits
     */
    int getStoppingDistance(@NotNull T object);

    /**
     * @param object the moving object
     * @return {@code true} if the turn rate of the object grows by one each frame it turns, {@code false} otherwise
     */
    boolean isAcceleratingTurn(@NotNull T object);
}

/**
 * Runs animation programs: {@link org.fogofwarlabs.sc4j.engine.animation.system.AnimationState} holds the running
 * animation of every animated object in arrays (set, kind, program, position, wait timer, call stack, running flag),
 * {@link org.fogofwarlabs.sc4j.engine.animation.system.AnimationRunner} executes the instructions on it with one reused
 * context, and {@link org.fogofwarlabs.sc4j.engine.animation.system.AnimationSystem} with its engine default
 * {@link org.fogofwarlabs.sc4j.engine.animation.system.StandardAnimationSystem} animates the engine image pool.
 * <p>
 * A mod that pools its own images keeps an {@code AnimationState} sized to its pool and an {@code AnimationRunner} with
 * its own {@link org.fogofwarlabs.sc4j.engine.animation.AnimationTarget}, and updates its images in its own frame order.
 * <p>
 * Running is part of the simulation: it allocates nothing, iterates in index or pool order and draws random values only
 * through the game generator.
 */
package org.fogofwarlabs.sc4j.engine.animation.system;

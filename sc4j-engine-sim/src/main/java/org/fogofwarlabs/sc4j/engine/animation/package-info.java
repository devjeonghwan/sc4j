/**
 * Defines the animation model: an {@link org.fogofwarlabs.sc4j.engine.animation.AnimationLibrary} holds the
 * {@link org.fogofwarlabs.sc4j.engine.animation.AnimationSet} of each image type, which names the entry of an
 * {@link org.fogofwarlabs.sc4j.engine.animation.AnimationProgram} for each
 * {@link org.fogofwarlabs.sc4j.engine.animation.AnimationKind} (idle, walk, death, ...). A program is a flat array of
 * {@link org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction} objects with its labels resolved to positions.
 * <p>
 * The model is immutable and created once, when the resources are loaded; every game shares it. The running state of
 * each animated object (program, position, wait timer, call stack) is not part of the model: it lives in arrays of the
 * animation system ({@code animation.system}), and an instruction reaches it only through its
 * {@link org.fogofwarlabs.sc4j.engine.animation.AnimationContext}.
 * <p>
 * The instruction set is open. The engine instructions are in {@code animation.instruction}; a mod adds its own by
 * extending {@link org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction} (and implementing
 * {@link org.fogofwarlabs.sc4j.engine.animation.BranchInstruction} for one that continues at a fixed position), and puts
 * the objects into its programs like the engine instructions. Such an instruction reads and writes the animation state
 * only through the context, its own game state through the {@link org.fogofwarlabs.sc4j.engine.animation.AnimationTarget}
 * and {@link org.fogofwarlabs.sc4j.engine.animation.AnimationEffects} its mod gave the animation system, and keeps no
 * state of its own.
 */
package org.fogofwarlabs.sc4j.engine.animation;

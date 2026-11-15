/**
 * Holds the engine animation instructions. Each is an immutable object created once, when the resources are loaded:
 * <ul>
 *     <li>program flow: {@link org.fogofwarlabs.sc4j.engine.animation.instruction.Wait},
 *     {@link org.fogofwarlabs.sc4j.engine.animation.instruction.WaitRandom},
 *     {@link org.fogofwarlabs.sc4j.engine.animation.instruction.Jump},
 *     {@link org.fogofwarlabs.sc4j.engine.animation.instruction.RandomJump},
 *     {@link org.fogofwarlabs.sc4j.engine.animation.instruction.Call},
 *     {@link org.fogofwarlabs.sc4j.engine.animation.instruction.Return},
 *     {@link org.fogofwarlabs.sc4j.engine.animation.instruction.End};</li>
 *     <li>the picture: {@link org.fogofwarlabs.sc4j.engine.animation.instruction.ShowFrame};</li>
 *     <li>requests to other systems, through {@link org.fogofwarlabs.sc4j.engine.animation.AnimationEffects}:
 *     {@link org.fogofwarlabs.sc4j.engine.animation.instruction.PlaySound},
 *     {@link org.fogofwarlabs.sc4j.engine.animation.instruction.SpawnOverlay},
 *     {@link org.fogofwarlabs.sc4j.engine.animation.instruction.FireWeapon},
 *     {@link org.fogofwarlabs.sc4j.engine.animation.instruction.Move}.</li>
 * </ul>
 * Random draws: {@code WaitRandom} and {@code RandomJump} draw exactly one value each time they run, from the animation
 * source of the context; no other engine instruction draws. A mod whose instructions need other formulas or other
 * call semantics adds its own instruction classes instead of changing these.
 */
package org.fogofwarlabs.sc4j.engine.animation.instruction;

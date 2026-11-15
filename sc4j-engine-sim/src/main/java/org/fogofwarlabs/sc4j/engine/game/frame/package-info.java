/**
 * Defines the frame: the fixed simulation step and the parts of it the engine enforces.
 * <p>
 * A mod sets the ordered {@link org.fogofwarlabs.sc4j.engine.game.frame.FramePhase} list; the
 * {@link org.fogofwarlabs.sc4j.engine.game.frame.FrameRunner} runs it inside the engine frame skeleton:
 * <ol>
 *     <li>The random generator becomes active.</li>
 *     <li>The command application point: the {@link org.fogofwarlabs.sc4j.engine.game.frame.CommandSource} applies the
 *     commands due at this frame, the only external input of the simulation.</li>
 *     <li>If the game is not paused, the frame counter of the {@link org.fogofwarlabs.sc4j.engine.game.frame.FrameClock}
 *     advances and the running phases run in list order; if it is paused, only the phases that run while paused run.</li>
 *     <li>The random generator becomes inactive, also when a phase throws.</li>
 *     <li>The sync hash point: after a frame that advanced the counter, every {@code n}-th frame computes the state hash
 *     for the {@link org.fogofwarlabs.sc4j.engine.game.frame.SyncHashListener}.</li>
 * </ol>
 * Setup frames run the setup phases inside an active random range without commands, counter or hash, for worlds that
 * settle before the first frame.
 */
package org.fogofwarlabs.sc4j.engine.game.frame;

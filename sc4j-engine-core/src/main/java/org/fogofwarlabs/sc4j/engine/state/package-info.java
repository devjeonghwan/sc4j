/**
 * Defines the state hash: one {@code 64}-bit value over the simulation state that two runs of the same game compare to
 * find a desynchronization.
 * <p>
 * {@link org.fogofwarlabs.sc4j.engine.state.StateHash} is the documented mixing function and
 * {@link org.fogofwarlabs.sc4j.engine.state.StateHashContributor} is what every part of the simulation state implements.
 * The game fixes the order of the contributions ({@code Game.computeStateHash}); a contributor mixes its values in a
 * fixed order of its own and never in a hash-dependent iteration order.
 */
package org.fogofwarlabs.sc4j.engine.state;

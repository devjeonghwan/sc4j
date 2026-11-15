/**
 * Holds the integer arithmetic of the simulation: fixed-point operations on {@code int} values and integer helpers.
 * <p>
 * The simulation uses no floating point, so every machine computes the same values. The helpers are static operations
 * on primitives: they allocate nothing and inline on the hot path.
 */
package org.fogofwarlabs.sc4j.engine.math;

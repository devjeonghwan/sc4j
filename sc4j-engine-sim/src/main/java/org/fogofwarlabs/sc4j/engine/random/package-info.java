/**
 * Defines the simulation's random numbers: {@link org.fogofwarlabs.sc4j.engine.random.DeterministicRandom}, its saved
 * state {@link org.fogofwarlabs.sc4j.engine.random.RandomState} and the engine default generator
 * {@link org.fogofwarlabs.sc4j.engine.random.PcgRandom}.
 * <p>
 * Every random value of a game comes from the game's one generator. A mod that must reproduce another generator
 * registers its own implementation through a {@link org.fogofwarlabs.sc4j.engine.random.RandomFactory}
 * ({@code GameDefinition.Builder.setRandomFactory}); without one, the game uses {@link org.fogofwarlabs.sc4j.engine.random.PcgRandom}.
 */
package org.fogofwarlabs.sc4j.engine.random;

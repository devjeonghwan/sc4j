/**
 * Defines the game systems: the replaceable parts of the simulation and how a game finds them.
 * <p>
 * A mod registers a factory per {@link org.fogofwarlabs.sc4j.engine.game.system.SystemKey} in a
 * {@link org.fogofwarlabs.sc4j.engine.game.system.SystemRegistry}; registering a key again replaces the factory at its
 * position, which is how a mod replaces an engine default system as a whole. When a game is created, every factory
 * creates its system once, in registration order, and the systems are attached in the same order
 * ({@link org.fogofwarlabs.sc4j.engine.game.system.SystemSet}). A system resolves the other systems and the columns it
 * needs in {@link org.fogofwarlabs.sc4j.engine.game.system.GameSystem#attach} and keeps them in fields; nothing looks a
 * system up per frame.
 * <p>
 * The replaceable engine systems have their interfaces and keys in their own packages ({@code engine.movement},
 * {@code engine.path}, {@code engine.collision}, {@code engine.spatial}, {@code engine.visibility},
 * {@code engine.animation.system}, {@code engine.combat}, {@code engine.economy}, {@code engine.production},
 * {@code engine.content.requirement}, {@code engine.order}); the game resolves each into its own field when it is
 * created ({@link org.fogofwarlabs.sc4j.engine.game.Game#getMovement} and the other getters). Systems only a mod has
 * use their own keys and {@link org.fogofwarlabs.sc4j.engine.game.Game#getModSystem}. Smaller differences inside a
 * system are hooks ({@code engine.hook}).
 */
package org.fogofwarlabs.sc4j.engine.game.system;

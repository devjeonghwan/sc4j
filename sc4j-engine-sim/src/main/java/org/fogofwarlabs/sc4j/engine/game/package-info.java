/**
 * Defines the game: {@link org.fogofwarlabs.sc4j.engine.game.Game}, the composition root of one running simulation, the
 * {@link org.fogofwarlabs.sc4j.engine.game.GameDefinition} a mod registers for all its games and the
 * {@link org.fogofwarlabs.sc4j.engine.game.GameSetup} of one game (map, player slots, seed, options, limits).
 * <p>
 * A game holds the parts and exposes them; the rules live in the systems ({@code game.system}) and frame phases
 * ({@code game.frame}). The per-game content tables join the game with the content model.
 */
package org.fogofwarlabs.sc4j.engine.game;

/**
 * Holds the content model of the engine: what kinds of things a game has and their per-game values.
 * <p>
 * A mod declares one identity constant per type ({@code content.type}), registers it with its definition
 * ({@code content.definition}) in registration order through {@code ModRegistration.getContent()}
 * ({@code content.registry}), and every game copies the definitions into writable struct-of-arrays tables
 * ({@code content.table}) that the simulation, map settings, triggers and saved games change. Factions
 * ({@code content.faction}) and resource kinds ({@code content.economy}) are game-invariant; upgrades and techs act through
 * stat modifiers ({@code content.modifier}); requirements are a replaceable system with a declarative engine default
 * ({@code content.requirement}). Indices follow registration order, so they are the same on every machine; a mod that
 * must match external numbers keeps its own mapping.
 */
package org.fogofwarlabs.sc4j.engine.content;

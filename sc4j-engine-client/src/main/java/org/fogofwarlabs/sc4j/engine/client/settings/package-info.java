/**
 * Defines the engine settings store: a mod's own settings file of groups and keys
 * ({@link org.fogofwarlabs.sc4j.engine.client.settings.SettingsStore}), read in file order with typed reads per group
 * ({@link org.fogofwarlabs.sc4j.engine.client.settings.SettingsGroup}) and written by replacing only the given keys, keeping unknown groups and keys,
 * atomically where the file system allows. Each mod has one store; mods never share settings.
 */
package org.fogofwarlabs.sc4j.engine.client.settings;

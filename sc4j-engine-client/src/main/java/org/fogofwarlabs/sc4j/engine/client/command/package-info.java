/**
 * Holds the engine input to command framework of a local player: the {@link org.fogofwarlabs.sc4j.engine.client.command.Selection} (the engine
 * default {@link org.fogofwarlabs.sc4j.engine.client.command.OrderedSelection}) and its
 * {@link org.fogofwarlabs.sc4j.engine.client.command.ControlGroups}, the
 * {@link org.fogofwarlabs.sc4j.engine.client.command.SelectionFilter} that chooses selected units by a game's
 * {@link org.fogofwarlabs.sc4j.engine.client.command.SelectionRules} (the engine default
 * {@link org.fogofwarlabs.sc4j.engine.client.command.StandardSelectionRules}), actions that submit commands for the selected units
 * ({@link org.fogofwarlabs.sc4j.engine.client.command.CommandAction}, the engine order default
 * {@link org.fogofwarlabs.sc4j.engine.client.command.OrderAction}), targets the player points at
 * ({@link org.fogofwarlabs.sc4j.engine.client.command.CommandTarget}, {@link org.fogofwarlabs.sc4j.engine.client.command.TargetPicker}) and
 * the {@link org.fogofwarlabs.sc4j.engine.client.command.CommandTranslator} that turns key bindings and pointer presses into commands for a
 * {@link org.fogofwarlabs.sc4j.engine.command.CommandSink}, with the targeted action waiting in a
 * {@link org.fogofwarlabs.sc4j.engine.client.command.TargetingMode}.
 * <p>
 * The client reaches the simulation only through these commands.
 */
package org.fogofwarlabs.sc4j.engine.client.command;

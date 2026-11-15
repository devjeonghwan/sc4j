/**
 * Holds the engine console command framework: {@link org.fogofwarlabs.sc4j.engine.client.console.ConsoleCommand} (a slash command declared with
 * its forms, help sections, requirements and examples, whose detailed help is generated from the same declaration) and
 * {@link org.fogofwarlabs.sc4j.engine.client.console.ConsoleCommandRegistry} (the commands of one client with the built-in {@code /help},
 * {@code /?} and {@code /more}, and paged replies), and the engine default {@link org.fogofwarlabs.sc4j.engine.client.console.Console} (a
 * typed line and message lines that runs the commands).
 * <p>
 * Client-only extras of a mod (camera, debug and replay controls) are console commands with full help, not key bindings.
 */
package org.fogofwarlabs.sc4j.engine.client.console;

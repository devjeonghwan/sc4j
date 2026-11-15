/**
 * Runs one mod in the runtime sequence (log, manifest, settings, class loader, entry, initialize, register, identity,
 * application) and shuts down in reverse order.
 * <p>
 * For a mod with its own application loop, the session runs the application the mod registered.
 */
package org.fogofwarlabs.sc4j.runtime.session;

/**
 * Loads a mod jar in its own class loader, with the engine-side packages protected, and creates its entry class.
 * <p>
 * Internal ({@code @ApiStatus.Internal}): not part of the engine API ({@code docs/api-policy.md}); it may change or disappear in any version
 * without notice. The runtime uses it.
 */
@ApiStatus.Internal
package org.fogofwarlabs.sc4j.engine.mod.loading;

import org.jetbrains.annotations.ApiStatus;

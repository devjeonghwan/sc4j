/**
 * Defines how the engine obtains the resources of a mod: the {@link org.fogofwarlabs.sc4j.engine.resource.ResourceProvider}
 * a mod registers, the {@link org.fogofwarlabs.sc4j.engine.resource.ResourceKey} that names a resource and the failure
 * contract ({@link org.fogofwarlabs.sc4j.engine.resource.ResourceNotFoundException}).
 * <p>
 * The engine reads only the engine resource model: sprite sheets and palettes ({@code resource.graphics}), sound clips
 * ({@code resource.audio}), string tables and font faces ({@code resource.text}), dialog definitions
 * ({@code resource.dialog}) and the animation library ({@code engine.animation}). How a mod stores its data and turns it
 * into this model is the mod's own business; the engine standard data pack ({@code engine.pack}) is one such provider.
 * <p>
 * Resources are loaded outside the simulation, before or between games. A loaded model object is immutable, so every
 * game, scene and renderer may share it.
 */
package org.fogofwarlabs.sc4j.engine.resource;

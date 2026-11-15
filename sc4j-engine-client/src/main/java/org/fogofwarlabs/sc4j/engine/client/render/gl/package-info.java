/**
 * Holds the OpenGL 3.3 core profile defaults of the renderer abstraction: the graphics device
 * ({@link org.fogofwarlabs.sc4j.engine.client.render.gl.GlGraphicsDevice}) with its shader programs, the canvas
 * ({@link org.fogofwarlabs.sc4j.engine.client.render.gl.GlCanvas}) with its textures, the palette and team colour sprite batch
 * ({@link org.fogofwarlabs.sc4j.engine.client.render.gl.GlSpriteBatch}) and the shader helper that the renderers share.
 * <p>
 * Internal ({@code @ApiStatus.Internal}): not part of the engine API ({@code docs/api-policy.md}); it may change or disappear in any version
 * without notice. Mods create their GPU resources through {@link org.fogofwarlabs.sc4j.engine.client.render.GraphicsDevice}.
 */
@ApiStatus.Internal
package org.fogofwarlabs.sc4j.engine.client.render.gl;

import org.jetbrains.annotations.ApiStatus;

/**
 * Defines the renderer abstraction of the engine client: the 2D {@link org.fogofwarlabs.sc4j.engine.client.render.Canvas} of
 * interface drawing with its {@link org.fogofwarlabs.sc4j.engine.client.render.Texture}s, the GPU resources of the current rendering
 * context ({@link org.fogofwarlabs.sc4j.engine.client.render.GraphicsDevice}: the
 * {@link org.fogofwarlabs.sc4j.engine.client.render.GpuCanvas}, {@link org.fogofwarlabs.sc4j.engine.client.render.ShaderProgram}s and
 * color target selection of a mod's own render passes) with the distance field
 * {@link org.fogofwarlabs.sc4j.engine.client.render.GlyphAtlas}, the {@link org.fogofwarlabs.sc4j.engine.client.render.SpriteBatch} that
 * draws sprite sheets of the resource model with a palette and team colours, and the world view
 * ({@link org.fogofwarlabs.sc4j.engine.client.render.WorldView}) as ordered {@link org.fogofwarlabs.sc4j.engine.client.render.WorldLayer}s
 * over a {@link org.fogofwarlabs.sc4j.engine.client.render.WorldViewport}. The OpenGL defaults are in
 * {@link org.fogofwarlabs.sc4j.engine.client.render.gl}.
 */
package org.fogofwarlabs.sc4j.engine.client.render;

/**
 * Defines the engine's display scale model: an interface is laid out in logical units, and one logical unit covers
 * {@link org.fogofwarlabs.sc4j.engine.client.display.DisplayScale#getScale} physical pixels (the operating system's scale of the
 * window's monitor times the user's interface scale of the mod). {@link org.fogofwarlabs.sc4j.engine.client.display.UiSurface} is the
 * logical surface of one frame over the framebuffer, and {@link org.fogofwarlabs.sc4j.engine.client.display.LetterboxFit} fits an
 * interface of a fixed design size into it.
 */
package org.fogofwarlabs.sc4j.engine.client.display;

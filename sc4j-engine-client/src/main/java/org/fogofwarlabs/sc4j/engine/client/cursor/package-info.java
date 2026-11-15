/**
 * Holds the engine cursor API: the {@link org.fogofwarlabs.sc4j.engine.client.cursor.CursorContext} of the pointer, the
 * {@link org.fogofwarlabs.sc4j.engine.client.cursor.CursorSet} of cursor pictures (each picture's anchor its hot spot), the
 * {@link org.fogofwarlabs.sc4j.engine.client.cursor.CursorController} that shows the picture of the current context, and the
 * {@link org.fogofwarlabs.sc4j.engine.client.cursor.CursorSurface} that shows it on a window
 * ({@link org.fogofwarlabs.sc4j.engine.client.host.HostWindow#createCursorSurface}) or nowhere
 * ({@link org.fogofwarlabs.sc4j.engine.client.cursor.HeadlessCursorSurface}).
 */
package org.fogofwarlabs.sc4j.engine.client.cursor;

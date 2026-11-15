/**
 * Holds the engine default HUD building blocks of an RTS client ({@link org.fogofwarlabs.sc4j.engine.client.hud.HudBlock}): the
 * {@link org.fogofwarlabs.sc4j.engine.client.hud.ResourceBar}, the {@link org.fogofwarlabs.sc4j.engine.client.hud.Minimap} (terrain, units, fog,
 * {@link org.fogofwarlabs.sc4j.engine.client.hud.MinimapPings} and the camera box, in the look of a
 * {@link org.fogofwarlabs.sc4j.engine.client.hud.MinimapStyle}, by default
 * {@link org.fogofwarlabs.sc4j.engine.client.hud.StandardMinimapStyle}), the {@link org.fogofwarlabs.sc4j.engine.client.hud.SelectionPanel},
 * the {@link org.fogofwarlabs.sc4j.engine.client.hud.ProductionQueuePanel} and the {@link org.fogofwarlabs.sc4j.engine.client.hud.CommandCard} of
 * {@link org.fogofwarlabs.sc4j.engine.client.hud.CommandButton}s. They draw on a
 * {@link org.fogofwarlabs.sc4j.engine.client.render.Canvas}, read the game through a {@link org.fogofwarlabs.sc4j.engine.client.view.GameView}
 * and issue commands through the {@link org.fogofwarlabs.sc4j.engine.client.command.CommandTranslator}.
 */
package org.fogofwarlabs.sc4j.engine.client.hud;

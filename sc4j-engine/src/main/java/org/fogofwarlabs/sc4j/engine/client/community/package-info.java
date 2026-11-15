/**
 * Holds the engine's default parts of the Beacon.Link community screens, so a game built only on the engine gets the same channels, chat,
 * friends and room list as any mod: the channel chat over the community session with its slash commands and log entries
 * ({@link org.fogofwarlabs.sc4j.engine.client.community.ChannelChat}, {@link org.fogofwarlabs.sc4j.engine.client.community.ChatEntry}), the
 * room list filter ({@link org.fogofwarlabs.sc4j.engine.client.community.RoomFilter}), their English and Korean texts
 * ({@link org.fogofwarlabs.sc4j.engine.client.community.CommunityText}), and two dialog controls drawn with a
 * {@link org.fogofwarlabs.sc4j.engine.client.dialog.DialogTheme}: a chat log
 * ({@link org.fogofwarlabs.sc4j.engine.client.community.ChatLogBox}) and a table of players, channels or rooms with latency bars
 * ({@link org.fogofwarlabs.sc4j.engine.client.community.TableBox}). A mod with its own look keeps its drawing and uses the headless parts.
 */
package org.fogofwarlabs.sc4j.engine.client.community;

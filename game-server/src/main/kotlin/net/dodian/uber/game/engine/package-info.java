/**
 * Core server engine and orchestration layer.
 * <p>
 * This package contains the "central nervous system" of the server, including:
 * <ul>
 *   <li>The 600ms tick-based {@code GameLoopService}</li>
 *   <li>Network ingress/egress management and packet mailboxes</li>
 *   <li>Task scheduling, entity processing, and game phase orchestration</li>
 *   <li>System-level services like collision building and cache bootstrapping</li>
 * </ul>
 */
package net.dodian.uber.game.engine;

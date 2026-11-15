/**
 * Defines the lockstep core: the participants of a session ({@link org.fogofwarlabs.sc4j.engine.lockstep.LockstepPeer}), the transport of
 * their turns with its all-turns barrier ({@link org.fogofwarlabs.sc4j.engine.lockstep.LockstepTransport}), the turn exchange that keeps the
 * latency's turns in transit ({@link org.fogofwarlabs.sc4j.engine.lockstep.TurnExchange}), desync detection
 * ({@link org.fogofwarlabs.sc4j.engine.lockstep.DesyncMonitor}), departures ({@link org.fogofwarlabs.sc4j.engine.lockstep.PeerDeparture}), the
 * waiting state of the barrier ({@link org.fogofwarlabs.sc4j.engine.lockstep.WaitingPeers}) with its waiting dialog countdown
 * ({@link org.fogofwarlabs.sc4j.engine.lockstep.WaitCountdown}), the turn rate of a network session
 * ({@link org.fogofwarlabs.sc4j.engine.lockstep.TurnRate}), and the engine default session with its command frames and state hash exchange
 * ({@link org.fogofwarlabs.sc4j.engine.lockstep.LockstepSession}) over the in-memory transport or any other.
 * <p>
 * A mod with its own turn protocol uses the transport, the exchange and the monitor with its own turn format and sync check; the engine
 * session gives a mod without one a complete lockstep game. Every mod reads the waiting state, the turn rate and the drop button through the
 * same transport model.
 */
package org.fogofwarlabs.sc4j.engine.lockstep;

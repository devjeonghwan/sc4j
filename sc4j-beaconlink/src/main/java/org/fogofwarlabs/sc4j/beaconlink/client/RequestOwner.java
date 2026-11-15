package org.fogofwarlabs.sc4j.beaconlink.client;

import org.fogofwarlabs.sc4j.beaconlink.protocol.message.Envelope;
import org.jetbrains.annotations.NotNull;

/**
 * Receives the outcome of the requests it sent through the client: a node session (registrations, node lists), a hosted room (publications,
 * withdrawals) or a room search (listings).
 */
interface RequestOwner {
    /**
     * Called when a node answered a request: the reply came from the node's address, with the node's key, inside the replay window and with
     * a valid signature.
     *
     * @param node      the node
     * @param reply     the reply; its request number is the request's
     * @param nowMillis the protocol clock time
     */
    void onReply(@NotNull NodeSession node, @NotNull Envelope reply, long nowMillis);

    /**
     * Called when a node did not answer a request after every attempt, or answered with another key than the expected one.
     *
     * @param node          the node
     * @param requestNumber the request number
     * @param nowMillis     the protocol clock time
     */
    void onNoAnswer(@NotNull NodeSession node, long requestNumber, long nowMillis);
}

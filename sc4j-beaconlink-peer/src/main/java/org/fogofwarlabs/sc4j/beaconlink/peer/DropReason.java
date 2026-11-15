package org.fogofwarlabs.sc4j.beaconlink.peer;

/**
 * Lists why a peer dropped a datagram without handling it. A dropped datagram is never answered.
 */
public enum DropReason {
    /**
     * The datagram is longer than a protocol datagram.
     */
    OVERSIZE,
    /**
     * The source address is over its rate.
     */
    ADDRESS_RATE,
    /**
     * The datagram is not a well formed envelope.
     */
    MALFORMED,
    /**
     * The datagram is a reply that only clients receive.
     */
    UNEXPECTED_REPLY,
    /**
     * The sender key is the peer's own key.
     */
    OWN_KEY,
    /**
     * The timestamp is outside the replay window.
     */
    OUTSIDE_WINDOW,
    /**
     * The sender key is over its rate.
     */
    KEY_RATE,
    /**
     * The signature is not valid: an unsigned or forged datagram.
     */
    FORGED,
    /**
     * The sender's nonce was seen inside the window.
     */
    REPLAYED,
    /**
     * The nonce cache holds the most nonces of the source address.
     */
    REPLAY_SOURCE_FULL,
    /**
     * The nonce cache is full.
     */
    REPLAY_CACHE_FULL,
    /**
     * The answer to a sender that has not proved its address would be longer than the request: a ping, a registration, a hello, a lookup
     * request or a dial-back.
     */
    UNPADDED,
    /**
     * A peer message that does not come from a peer known at its address, or a reply to no outstanding request.
     */
    NOT_A_PEER,
    /**
     * A peer's card whose proof nonce is not valid for its key.
     */
    INVALID_PROOF,
    /**
     * An entry of a lookup answer that is not used: no valid proof, not an IPv4 address, the own key, or another kind of address than the
     * answering peer's (counted per entry, the answer itself is handled).
     */
    INVALID_ENTRY,
    /**
     * A probe that answers no waiting dial-back of the peer.
     */
    UNREQUESTED_PROBE,
    /**
     * A topic message for a topic the peer is not in, or from a peer that is neither its mesh neighbour in the topic nor a member it named
     * records to.
     */
    NOT_IN_TOPIC,
    /**
     * A record that is not used: of another topic, with an invalid signature, expired or over its author's rate (counted per record, the
     * message itself is handled).
     */
    INVALID_RECORD,
    /**
     * A round trip sample whose answer carried a coordinate the peer's settled coordinate rejects as an outlier (counted per sample, the
     * answer itself is handled).
     */
    COORDINATE_OUTLIER,
    /**
     * The source address, the sender key or the pair of both is graylisted after too many penalties.
     */
    GRAYLISTED,
    /**
     * A handler failed on the datagram: a defect of the peer, logged with the datagram's source.
     */
    DEFECT
}

package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

/**
 * Lists why {@link Envelope#decode} rejected a datagram.
 */
public enum DecodeFailure {
    /**
     * The datagram is shorter than an envelope without body.
     */
    TOO_SHORT,
    /**
     * The datagram is longer than {@value Envelope#MAXIMUM_DATAGRAM_LENGTH} bytes.
     */
    TOO_LONG,
    /**
     * The datagram does not start with the protocol magic.
     */
    BAD_MAGIC,
    /**
     * The protocol version is not {@value Envelope#PROTOCOL_VERSION}.
     */
    UNSUPPORTED_VERSION,
    /**
     * No message kind has the datagram's kind code.
     */
    UNKNOWN_KIND,
    /**
     * Unknown flag bits are set, or the reply flag does not fit the kind.
     */
    BAD_FLAGS,
    /**
     * The body length or a body field is out of range, or the body has bytes left over.
     */
    MALFORMED_BODY,
    /**
     * The padding holds a non-zero byte or does not fit.
     */
    BAD_PADDING,
    /**
     * Bytes follow the signature, or the signature is cut.
     */
    BAD_SIGNATURE_FIELD
}

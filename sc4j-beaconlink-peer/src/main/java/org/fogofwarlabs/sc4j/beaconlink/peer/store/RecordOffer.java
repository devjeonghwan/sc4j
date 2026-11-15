package org.fogofwarlabs.sc4j.beaconlink.peer.store;

import org.fogofwarlabs.sc4j.beaconlink.protocol.message.ResultStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Lists the outcomes of an offered record of a topic (a room advertisement, a presence or a chat line: {@link RoomStore#offer},
 * {@link CommunityStore#offer} and the topic checks before them) with the result status a peer answers for each.
 */
public enum RecordOffer {
    /**
     * The record is stored: a new room, an update or a withdrawal, a new presence or a new chat line.
     */
    ACCEPTED(ResultStatus.ACCEPTED),
    /**
     * The store holds the same room or the same player's presence with the same or a higher sequence.
     */
    STALE(ResultStatus.STALE),
    /**
     * The record has expired.
     */
    EXPIRED(ResultStatus.INVALID),
    /**
     * The record expires later than the largest lifetime of its kind allows, or a chat line is dated in the future.
     */
    LIFETIME_TOO_LONG(ResultStatus.INVALID),
    /**
     * The author's signature is not valid.
     */
    INVALID_SIGNATURE(ResultStatus.INVALID),
    /**
     * The author's stamp is not valid for the stamp difficulty.
     */
    INVALID_STAMP(ResultStatus.INVALID),
    /**
     * The record does not belong in its topic: a room outside a mod topic, or a presence in another player's topic.
     */
    WRONG_TOPIC(ResultStatus.INVALID),
    /**
     * The store holds the most records of the kind allowed, and the record does not take the place of an older one.
     */
    FULL(ResultStatus.LIMIT_REACHED),
    /**
     * The host has the most rooms allowed, of its key or of the connection that holds the room.
     */
    HOST_LIMIT(ResultStatus.LIMIT_REACHED),
    /**
     * The author published more records of the kind lately than its rate allows.
     */
    AUTHOR_RATE(ResultStatus.LIMIT_REACHED),
    /**
     * The peer takes part in the most topics allowed and cannot join the record's topic.
     */
    TOPIC_LIMIT(ResultStatus.LIMIT_REACHED);

    private final @NotNull ResultStatus status;

    /**
     * Creates a {@link RecordOffer} instance.
     *
     * @param status the result status a node answers
     */
    RecordOffer(@NotNull ResultStatus status) {
        this.status = status;
    }

    /**
     * @return the result status a node answers for the outcome
     */
    public @NotNull ResultStatus getStatus() {
        return this.status;
    }
}

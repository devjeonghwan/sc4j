package org.fogofwarlabs.sc4j.beaconlink.peer.account;

import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.PlayerNameUtils;

/**
 * Lists why an account could not be created, changed, imported or used; the launcher shows each with a short text.
 */
public enum AccountProblem {
    /**
     * The name is not a valid player name ({@link PlayerNameUtils}): empty, longer than {@value PlayerNameUtils#MAXIMUM_LENGTH} UTF-8 bytes or
     * holding a character a name cannot have.
     */
    NAME_INVALID,
    /**
     * An account with the same key is already in the list.
     */
    ALREADY_ADDED,
    /**
     * A process uses the account (a game or the launcher), or the account is being deleted.
     */
    IN_USE,
    /**
     * The file is no account backup of this version, or it is damaged.
     */
    NOT_AN_ACCOUNT_FILE,
    /**
     * The backup is protected by a password and none was given.
     */
    PASSWORD_REQUIRED,
    /**
     * The password does not open the backup.
     */
    WRONG_PASSWORD
}

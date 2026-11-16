package org.fogofwarlabs.sc4j.launcher.release;

/**
 * Lists why reading a release or one of its files failed, for a short message to the player.
 */
public enum ReleaseProblem {
    /**
     * The text names no GitHub repository.
     */
    INVALID_REPOSITORY,
    /**
     * The repository does not exist or has no release.
     */
    NOT_FOUND,
    /**
     * GitHub cannot be reached or answered with an error.
     */
    UNAVAILABLE,
    /**
     * GitHub's answer is not what a release or a file looks like.
     */
    INVALID_RESPONSE,
    /**
     * The release lacks the files the launcher needs.
     */
    ASSET_MISSING,
    /**
     * A file is larger than the launcher reads.
     */
    TOO_LARGE,
    /**
     * A downloaded file does not match its checksum file.
     */
    CHECKSUM_MISMATCH,
    /**
     * A downloaded user package is broken or not an SC4J package.
     */
    INVALID_PACKAGE
}

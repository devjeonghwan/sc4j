/**
 * Defines the files that several Beacon.Link processes of one computer share ({@link org.fogofwarlabs.sc4j.beaconlink.peer.file.SharedFile}):
 * each process merges its change into the locked file in place, and reads it under a shared lock.
 */
package org.fogofwarlabs.sc4j.beaconlink.peer.file;

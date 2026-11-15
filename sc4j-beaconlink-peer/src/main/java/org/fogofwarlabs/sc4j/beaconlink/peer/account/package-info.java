/**
 * Defines the player accounts of an install ({@link org.fogofwarlabs.sc4j.beaconlink.peer.account.AccountStore}): one folder per player key
 * with its name, stamp, friends and blocked players ({@link org.fogofwarlabs.sc4j.beaconlink.peer.account.Account}), the shared lock every
 * process holds on an account it uses ({@link org.fogofwarlabs.sc4j.beaconlink.peer.account.AccountLock}) and the backup file that carries an
 * account to another install, optionally protected by a password ({@link org.fogofwarlabs.sc4j.beaconlink.peer.account.AccountBackup}).
 */
package org.fogofwarlabs.sc4j.beaconlink.peer.account;

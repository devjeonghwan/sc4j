package org.fogofwarlabs.sc4j.beaconlink.peer.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the peer configuration builder: every set value is the value the built configuration gives.
 */
final class PeerConfigurationTest {
    @Test
    void build_should_keep_every_set_value() {
        PeerConfiguration configuration = new PeerConfiguration.Builder().setMaximumRegistrationSeconds(11)
                                                                         .setMaximumConnectionsPerKey(12)
                                                                         .setMaximumRoomsPerConnection(13)
                                                                         .setMaximumRoomLifetimeMillis(14_000L)
                                                                         .setReplayWindowMillis(15_000L)
                                                                         .setReplayCapacity(16)
                                                                         .setHellosPerRound(17)
                                                                         .setMaximumPerWideNetworkInLookup(18)
                                                                         .setMaximumFailures(19)
                                                                         .setLookupParallelism(20)
                                                                         .setLookupPaths(21)
                                                                         .setMaximumLookupMillis(22_000L)
                                                                         .setMaximumLookups(23)
                                                                         .setRefreshIntervalMillis(24_000L)
                                                                         .setMaximumTopics(25)
                                                                         .setMaximumTopicMembers(26)
                                                                         .setTopicLifetimeMillis(27_000L)
                                                                         .setAnnounceIntervalMillis(28_000L)
                                                                         .setMaximumSwarms(29)
                                                                         .setMeshDegree(30)
                                                                         .setMinimumMeshDegree(31)
                                                                         .setLazyFanout(32)
                                                                         .setMeshKeepaliveMillis(33_000L)
                                                                         .setTopicIdleMillis(34_000L)
                                                                         .setTopicListWaitMillis(35_000L)
                                                                         .setAuthorBurst(36)
                                                                         .setAuthorRate(37)
                                                                         .setPenaltyLimit(38)
                                                                         .setPenaltyMemoryMillis(39_000L)
                                                                         .setGraylistMillis(40_000L)
                                                                         .setMaximumPresencesPerTopic(41)
                                                                         .setChatBacklog(42)
                                                                         .setMaximumPresenceLifetimeMillis(43_000L)
                                                                         .setPresenceBurst(44)
                                                                         .setPresenceIntervalMillis(45_000L)
                                                                         .setMaximumSubscribersPerTopic(46)
                                                                         .setSubscriptionLifetimeMillis(47_000L)
                                                                         .setMaintenanceIntervalMillis(48_000L)
                                                                         .setMaximumContactedAddresses(49)
                                                                         .build();

        Assertions.assertEquals(11, configuration.getMaximumRegistrationSeconds());
        Assertions.assertEquals(12, configuration.getMaximumConnectionsPerKey());
        Assertions.assertEquals(13, configuration.getMaximumRoomsPerConnection());
        Assertions.assertEquals(14_000L, configuration.getMaximumRoomLifetimeMillis());
        Assertions.assertEquals(15_000L, configuration.getReplayWindowMillis());
        Assertions.assertEquals(16, configuration.getReplayCapacity());
        Assertions.assertEquals(17, configuration.getHellosPerRound());
        Assertions.assertEquals(18, configuration.getMaximumPerWideNetworkInLookup());
        Assertions.assertEquals(19, configuration.getMaximumFailures());
        Assertions.assertEquals(20, configuration.getLookupParallelism());
        Assertions.assertEquals(21, configuration.getLookupPaths());
        Assertions.assertEquals(22_000L, configuration.getMaximumLookupMillis());
        Assertions.assertEquals(23, configuration.getMaximumLookups());
        Assertions.assertEquals(24_000L, configuration.getRefreshIntervalMillis());
        Assertions.assertEquals(25, configuration.getMaximumTopics());
        Assertions.assertEquals(26, configuration.getMaximumTopicMembers());
        Assertions.assertEquals(27_000L, configuration.getTopicLifetimeMillis());
        Assertions.assertEquals(28_000L, configuration.getAnnounceIntervalMillis());
        Assertions.assertEquals(29, configuration.getMaximumSwarms());
        Assertions.assertEquals(30, configuration.getMeshDegree());
        Assertions.assertEquals(31, configuration.getMinimumMeshDegree());
        Assertions.assertEquals(32, configuration.getLazyFanout());
        Assertions.assertEquals(33_000L, configuration.getMeshKeepaliveMillis());
        Assertions.assertEquals(34_000L, configuration.getTopicIdleMillis());
        Assertions.assertEquals(35_000L, configuration.getTopicListWaitMillis());
        Assertions.assertEquals(36, configuration.getAuthorBurst());
        Assertions.assertEquals(37, configuration.getAuthorRate());
        Assertions.assertEquals(38, configuration.getPenaltyLimit());
        Assertions.assertEquals(39_000L, configuration.getPenaltyMemoryMillis());
        Assertions.assertEquals(40_000L, configuration.getGraylistMillis());
        Assertions.assertEquals(41, configuration.getMaximumPresencesPerTopic());
        Assertions.assertEquals(42, configuration.getChatBacklog());
        Assertions.assertEquals(43_000L, configuration.getMaximumPresenceLifetimeMillis());
        Assertions.assertEquals(44, configuration.getPresenceBurst());
        Assertions.assertEquals(45_000L, configuration.getPresenceIntervalMillis());
        Assertions.assertEquals(46, configuration.getMaximumSubscribersPerTopic());
        Assertions.assertEquals(47_000L, configuration.getSubscriptionLifetimeMillis());
        Assertions.assertEquals(48_000L, configuration.getMaintenanceIntervalMillis());
        Assertions.assertEquals(49, configuration.getMaximumContactedAddresses());
    }
}

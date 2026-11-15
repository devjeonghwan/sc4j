package org.fogofwarlabs.sc4j.beaconlink.peer.topic;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.fogofwarlabs.sc4j.beaconlink.protocol.message.TopicMember;
import org.fogofwarlabs.sc4j.beaconlink.protocol.topic.TopicKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Holds the announcements a peer stores as one of the full peers closest to a topic: who is in the topic, for a limited time. It is bounded
 * in topics and in members per topic, so that a popular topic stays small: a new member of a full topic replaces the older announcement of
 * two random ones (fresh members win), and a new topic is refused while every topic slot holds a member.
 */
public final class TopicDirectory {
    /**
     * Holds one announcement: the member and the moment it expires.
     */
    private static final class Announcement {
        private final @NotNull TopicMember member;
        private final long expiryMillis;

        private Announcement(@NotNull TopicMember member, long expiryMillis) {
            this.member       = member;
            this.expiryMillis = expiryMillis;
        }
    }

    /**
     * Shuffles a list in place (Fisher-Yates) with the given source.
     *
     * @param list   the list
     * @param random the source of the swaps
     * @param <T>    the element type
     */
    static <T> void shuffle(@NotNull List<T> list, @NotNull RandomGenerator random) {
        for (int index = list.size() - 1; index > 0; index--) {
            int other = random.nextInt(index + 1);
            T value = list.get(index);

            list.set(index, list.get(other));
            list.set(other, value);
        }
    }


    private final int maximumTopics;
    private final int maximumMembers;
    private final long lifetimeMillis;
    private final @NotNull RandomGenerator random;

    private final @NotNull Map<@NotNull TopicKey, @NotNull LinkedHashMap<@NotNull IdentityKey, @NotNull Announcement>> topics;

    /**
     * Creates a {@link TopicDirectory} instance.
     *
     * @param maximumTopics  the largest number of topics
     * @param maximumMembers the largest number of members of one topic
     * @param lifetimeMillis the time an announcement lives
     * @param random         the source of the replacement choices
     * @throws IllegalArgumentException if a value is not positive
     */
    public TopicDirectory(int maximumTopics, int maximumMembers, long lifetimeMillis, @NotNull RandomGenerator random) {
        if (maximumTopics <= 0 || maximumMembers <= 0 || lifetimeMillis <= 0L) {
            throw Exceptions.newIllegalArgumentException("The topic capacity {0}, the member capacity {1} and the lifetime {2} must be positive.",
                                                         maximumTopics, maximumMembers, lifetimeMillis);
        }

        this.maximumTopics  = maximumTopics;
        this.maximumMembers = maximumMembers;
        this.lifetimeMillis = lifetimeMillis;
        this.random         = random;

        this.topics = new HashMap<>();
    }

    /**
     * @return the time an announcement lives, in milliseconds
     */
    public long getLifetimeMillis() {
        return this.lifetimeMillis;
    }


    /**
     * Stores an announcement, or renews the member's earlier one.
     *
     * @param topic     the topic
     * @param member    the member, at its observed address
     * @param nowMillis the current time
     * @return {@code true} if the announcement is stored, {@code false} otherwise (the directory holds the most topics)
     */
    public boolean announce(@NotNull TopicKey topic, @NotNull TopicMember member, long nowMillis) {
        LinkedHashMap<IdentityKey, Announcement> members = this.topics.get(topic);

        if (members == null) {
            if (this.topics.size() >= this.maximumTopics) {
                this.removeExpired(nowMillis);

                if (this.topics.size() >= this.maximumTopics) {
                    return false;
                }
            }

            members = new LinkedHashMap<>();
            this.topics.put(topic, members);
        }

        IdentityKey key = member.getContact().getKey();

        if (!members.containsKey(key) && members.size() >= this.maximumMembers) {
            List<IdentityKey> keys = new ArrayList<>(members.keySet());
            IdentityKey first = keys.get(this.random.nextInt(keys.size()));
            IdentityKey second = keys.get(this.random.nextInt(keys.size()));

            members.remove(members.get(first).expiryMillis <= members.get(second).expiryMillis ? first : second);
        }

        members.remove(key);
        members.put(key, new Announcement(member, nowMillis + this.lifetimeMillis));

        return true;
    }

    /**
     * Finds members of a topic: a random sample, members that strangers reach first.
     *
     * @param topic     the topic
     * @param excluded  the key that is left out (the asker's), or {@code null}
     * @param count     the largest number of members
     * @param nowMillis the current time
     * @return the members
     */
    public @NotNull List<@NotNull TopicMember> findMembers(@NotNull TopicKey topic, @Nullable IdentityKey excluded, int count, long nowMillis) {
        LinkedHashMap<IdentityKey, Announcement> members = this.topics.get(topic);
        List<TopicMember> reachable = new ArrayList<>();
        List<TopicMember> leaves = new ArrayList<>();

        if (members == null) {
            return reachable;
        }

        for (Announcement announcement : members.values()) {
            TopicMember member = announcement.member;

            if (announcement.expiryMillis > nowMillis && !member.getContact().getKey().equals(excluded)) {
                (member.isReachable() ? reachable : leaves).add(member);
            }
        }

        shuffle(reachable, this.random);
        shuffle(leaves, this.random);
        reachable.addAll(leaves);

        return new ArrayList<>(reachable.subList(0, Math.min(count, reachable.size())));
    }

    /**
     * Removes every announcement of a key, for example of a graylisted peer.
     *
     * @param key the member's key
     */
    public void remove(@NotNull IdentityKey key) {
        for (LinkedHashMap<IdentityKey, Announcement> members : this.topics.values()) {
            members.remove(key);
        }

        this.topics.values().removeIf(Map::isEmpty);
    }

    /**
     * Removes the expired announcements and the topics without any.
     *
     * @param nowMillis the current time
     */
    public void removeExpired(long nowMillis) {
        for (LinkedHashMap<IdentityKey, Announcement> members : this.topics.values()) {
            members.values().removeIf(announcement -> announcement.expiryMillis <= nowMillis);
        }

        this.topics.values().removeIf(Map::isEmpty);
    }
}

package org.fogofwarlabs.sc4j.engine.net.beaconlink.community;

import org.fogofwarlabs.sc4j.engine.net.beaconlink.LinkPlayer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests a chat line: its author, name, time and the tagged form it is shown in.
 */
final class ChatLineTest {
    private static final long TIMESTAMP_MILLIS = 1_800_000_000_000L;

    @Test
    void chat_line_keeps_its_author_name_and_time_and_tags_the_name() {
        LinkPlayer author = LinkPlayer.of(new byte[LinkPlayer.KEY_LENGTH]);
        ChatLine line = new ChatLine(author, "Alice", ChatLineKind.EMOTE, "waves", TIMESTAMP_MILLIS);

        Assertions.assertEquals(author, line.getAuthor());
        Assertions.assertEquals("Alice", line.getAuthorName());
        Assertions.assertEquals("Alice#" + author.getTag(), line.getTaggedAuthorName());
        Assertions.assertEquals(TIMESTAMP_MILLIS, line.getTimestampMillis());
        Assertions.assertEquals("Alice#" + author.getTag() + " waves", line.toString());
    }
}

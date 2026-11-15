package org.fogofwarlabs.sc4j.engine.state;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the documented state hash function; its values are an engine contract, so the expected value is fixed here.
 */
final class StateHashTest {
    @Test
    void mix_should_follow_the_documented_function() {
        StateHash hash = new StateHash();

        Assertions.assertEquals(StateHash.OFFSET_BASIS, hash.getValue());
        Assertions.assertEquals(0xa769ab7d50b1b123L, hash.mix(0x12345678).mix(-1).getValue());
        Assertions.assertEquals(StateHash.OFFSET_BASIS, hash.reset().getValue());
    }

    @Test
    void mixed_values_change_the_hash_by_value_and_order() {
        long forward = new StateHash().mix(1).mix(2).getValue();
        long backward = new StateHash().mix(2).mix(1).getValue();

        Assertions.assertEquals(forward, new StateHash().mix(1).mix(2).getValue());
        Assertions.assertNotEquals(forward, backward);
        Assertions.assertNotEquals(new StateHash().mixBoolean(false).getValue(), new StateHash().mixBoolean(true).getValue());
    }

    @Test
    void mix_long_and_mix_array_should_mix_their_parts_in_order() {
        long value = 0x0123456789abcdefL;

        Assertions.assertEquals(new StateHash().mix(0x89abcdef).mix(0x01234567).getValue(), new StateHash().mixLong(value).getValue());
        Assertions.assertEquals(new StateHash().mix(2).mix(7).mix(9).getValue(), new StateHash().mixArray(new int[]{7, 9}).getValue());
    }
}

package org.fogofwarlabs.sc4j.engine.resource.graphics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Tests the block sizes, level checks and copies of compressed textures.
 */
final class CompressedTextureTest {
    @Test
    void stored_size_covers_partial_blocks() {
        Assertions.assertEquals(8, CompressionFormat.BC1.getStoredSize(1, 1));
        Assertions.assertEquals(2 * 2 * 16, CompressionFormat.BC3.getStoredSize(5, 8));
        Assertions.assertEquals(16, CompressionFormat.BC7.getStoredSize(4, 4));
    }

    @Test
    void constructor_should_check_level_sizes_and_copy_the_blocks() {
        byte[] level = new byte[16];

        level[3] = 7;

        CompressedTexture texture = new CompressedTexture(CompressionFormat.BC1, 8, 4, List.of(level, new byte[8]));

        level[3] = 0;

        ByteBuffer buffer = ByteBuffer.allocate(texture.getLevelSize(0));

        texture.copyLevel(0, buffer);

        Assertions.assertEquals(2, texture.getLevelCount());
        Assertions.assertEquals(8, texture.getLevelSize(1));
        Assertions.assertEquals(7, buffer.get(3));
        Assertions.assertFalse(buffer.hasRemaining());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CompressedTexture(CompressionFormat.BC3, 8, 4, List.of(new byte[16])));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CompressedTexture(CompressionFormat.BC1, 8, 4, List.of()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CompressedTexture(CompressionFormat.BC1, 0, 4, List.of(new byte[8])));
    }
}

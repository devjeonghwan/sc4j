package org.fogofwarlabs.sc4j.engine.resource.graphics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the frames and the named layers of texture sheets.
 */
final class TextureSheetTest {
    @Test
    void texture_sheet_finds_layers_by_name_in_their_order() {
        CompressedTexture color = new CompressedTexture(CompressionFormat.BC3, 4, 4, List.of(new byte[16]));
        CompressedTexture mask = new CompressedTexture(CompressionFormat.BC1, 4, 4, List.of(new byte[8]));
        Map<String, CompressedTexture> layers = new LinkedHashMap<>();

        layers.put("mask", mask);
        layers.put("color", color);

        TextureSheet sheet = new TextureSheet(8, 8, 2, List.of(new TextureFrame(1, 2, 3, 4, 0, 0, 5)), layers);

        Assertions.assertSame(color, sheet.findLayer("color"));
        Assertions.assertNull(sheet.findLayer("normal"));
        Assertions.assertEquals(List.of("mask", "color"), List.copyOf(sheet.getLayerNames()));
        Assertions.assertEquals(5, sheet.getFrame(0).getFlags());
        Assertions.assertEquals(2, sheet.getUnitsPerTexel());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TextureSheet(8, 8, 0, List.of(), Map.of()));
    }
}

package org.fogofwarlabs.sc4j.engine.client.skin;

import org.fogofwarlabs.sc4j.engine.client.render.PictureRenderer;
import org.fogofwarlabs.sc4j.engine.client.render.SliceFill;
import org.fogofwarlabs.sc4j.engine.client.render.image.ImageCanvas;
import org.fogofwarlabs.sc4j.engine.pack.StandardPack;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests the pictures and the slice fill a picture box style keeps.
 */
final class PictureBoxStyleTest {
    @Test
    void getters_should_return_the_pictures_and_the_slice_fill(@TempDir @NotNull Path folder) throws IOException {
        Picture fill = new Picture(1, 1, PixelLayer.ofRgba(new int[1]));
        Picture picture = new Picture(1, 1, PixelLayer.ofRgba(new int[1]));

        Files.writeString(folder.resolve("pack.json"), "{\"format\": 1}", StandardCharsets.UTF_8);

        try (StandardPack pack = StandardPack.open(folder);
             PictureRenderer renderer = new PictureRenderer(new ImageCanvas(1, 1), pack)) {
            PictureBoxStyle stretched = new PictureBoxStyle(renderer, picture);
            PictureBoxStyle tiled = new PictureBoxStyle(renderer, fill, picture, SliceFill.TILE, PictureRenderer.OPAQUE_WHITE);

            Assertions.assertNull(stretched.getFill());
            Assertions.assertSame(picture, stretched.getPicture());
            Assertions.assertEquals(SliceFill.STRETCH, stretched.getSliceFill());
            Assertions.assertSame(fill, tiled.getFill());
            Assertions.assertEquals(SliceFill.TILE, tiled.getSliceFill());
        }
    }
}

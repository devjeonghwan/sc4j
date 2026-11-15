package org.fogofwarlabs.sc4j.demo.assets;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.json.JsonObjectReader;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exports the demo's SVG font drawings to the PNG pictures of its standard data pack at build time: every font the pack manifest lists. The
 * drawing of font {@code id} is {@code <svg>/fonts/<id>.svg}; it is painted whole as the font's one picture, named by the hand-written font
 * definition. A detail face {@code <id>-<n>x} of a font (the font at {@code n} times its size) is the drawing of font {@code id} painted
 * {@code n} times as large. The sprite sheets, the tileset and the other pictures of the pack are PNG files of the pack itself. The output is
 * the same bytes on every run.
 */
public final class DemoArtExport {
    /**
     * The number of arguments: the pack folder, the drawing folder and the output pack folder.
     */
    private static final int ARGUMENT_COUNT = 3;
    /**
     * The argument positions of the pack folder, the drawing folder and the output pack folder.
     */
    private static final int PACK_FOLDER_INDEX = 0;
    private static final int DRAWING_FOLDER_INDEX = 1;
    private static final int OUTPUT_FOLDER_INDEX = 2;
    /**
     * The font id of a detail face: the font id and the multiple, for example {@code body-2x}.
     */
    private static final @NotNull Pattern DETAIL_FONT_ID = Pattern.compile("(.+)-([0-9]+)x");
    /**
     * The groups of {@link #DETAIL_FONT_ID}: the font id of the drawing and the multiple.
     */
    private static final int DRAWING_ID_GROUP = 1;
    private static final int MULTIPLE_GROUP = 2;

    /**
     * Exports every font of the pack.
     *
     * @param arguments the pack folder with {@code pack.json} and the definitions, the drawing folder and the output pack folder
     * @throws IOException if a file cannot be read or written
     */
    public static void main(@NotNull String @NotNull [] arguments) throws IOException {
        if (arguments.length != ARGUMENT_COUNT) {
            throw Exceptions.newIllegalArgumentException("Usage: DemoArtExport <pack folder> <drawing folder> <output pack folder>");
        }

        Path pack = Path.of(arguments[PACK_FOLDER_INDEX]);
        Path drawings = Path.of(arguments[DRAWING_FOLDER_INDEX]);
        Path output = Path.of(arguments[OUTPUT_FOLDER_INDEX]);
        JsonObjectReader manifest = JsonObjectReader.parse(Files.readAllBytes(pack.resolve("pack.json")), "pack.json");

        for (Map.Entry<String, String> entry : manifest.getTextMap("fonts").entrySet()) {
            Matcher detail = DETAIL_FONT_ID.matcher(entry.getKey());
            String drawingId = detail.matches() ? detail.group(DRAWING_ID_GROUP) : entry.getKey();
            int multiple = detail.matches() ? Integer.parseInt(detail.group(MULTIPLE_GROUP)) : 1;

            exportPicture(pack, entry.getValue(), drawings.resolve("fonts").resolve(drawingId + ".svg"), multiple, output);
        }
    }

    private static void exportPicture(@NotNull Path pack, @NotNull String definitionPath, @NotNull Path drawing, int multiple, @NotNull Path output) throws IOException {
        JsonObjectReader definition = JsonObjectReader.parse(Files.readAllBytes(pack.resolve(definitionPath)), definitionPath);
        Document document = readDrawing(drawing);
        Element root = document.getDocumentElement();
        int width = Integer.parseInt(root.getAttribute("width"));
        int height = Integer.parseInt(root.getAttribute("height"));
        GraphicsNode node = buildNode(document);
        BufferedImage sheet = new BufferedImage(width * multiple, height * multiple, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();

        try {
            graphics.scale(multiple, multiple);
            graphics.setRenderingHint(RenderingHintsKeyExt.KEY_BUFFERED_IMAGE, new WeakReference<>(sheet));
            node.paint(graphics);
        } finally {
            graphics.dispose();
        }

        writePicture(sheet, output, definitionPath, definition.getText("image"));
    }

    private static @NotNull Document readDrawing(@NotNull Path drawing) throws IOException {
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());

        try (Reader reader = Files.newBufferedReader(drawing, StandardCharsets.UTF_8)) {
            return factory.createDocument(drawing.toUri().toString(), reader);
        }
    }

    private static @NotNull GraphicsNode buildNode(@NotNull Document document) {
        UserAgentAdapter agent = new UserAgentAdapter();
        BridgeContext context = new BridgeContext(agent, new DocumentLoader(agent));

        context.setDynamicState(BridgeContext.STATIC);

        try {
            return new GVTBuilder().build(context, document);
        } finally {
            context.dispose();
        }
    }

    private static void writePicture(@NotNull BufferedImage picture, @NotNull Path output, @NotNull String definitionPath, @NotNull String name) throws IOException {
        int separator = definitionPath.lastIndexOf('/');
        Path file = output.resolve((separator < 0 ? "" : definitionPath.substring(0, separator + 1)) + name);

        Files.createDirectories(file.getParent());

        if (!ImageIO.write(picture, "png", file.toFile())) {
            throw Exceptions.newIOException("No PNG writer for {0}.", file);
        }
    }

    private DemoArtExport() {
    }
}

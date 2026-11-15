package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.json.JsonObjectReader;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.fogofwarlabs.sc4j.engine.resource.dialog.ControlKind;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogDefinition;
import org.fogofwarlabs.sc4j.engine.resource.dialog.TextAlignment;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * Reads a dialog definition of a pack into a {@link DialogDefinition}: the dialog's bounds ({@code x}, {@code y}, {@code width},
 * {@code height}) and its {@code controls}. A control has an {@code id} (unique in the dialog), a {@code kind} (the {@link ControlKind} name in
 * lower case with hyphens, for example {@code "list-box"}), its bounds relative to its parent, and optionally a {@code text}, its
 * {@code align}ment ({@code "left"}, {@code "center"} or {@code "right"}, by default left), an {@code image} resource id, the mod's
 * {@code attributes} (an object of strings, such as {@code "hotkey"}, {@code "default"} or {@code "cancel"}) and its {@code children}. An
 * unknown field, kind or alignment, or an id used twice, is an error naming its place in the file.
 */
final class PackDialogUtils {
    private static final @NotNull String X_FIELD = "x";
    private static final @NotNull String Y_FIELD = "y";
    private static final @NotNull String WIDTH_FIELD = "width";
    private static final @NotNull String HEIGHT_FIELD = "height";
    private static final @NotNull String CONTROLS_FIELD = "controls";
    private static final @NotNull String ID_FIELD = "id";
    private static final @NotNull String KIND_FIELD = "kind";
    private static final @NotNull String TEXT_FIELD = "text";
    private static final @NotNull String ALIGN_FIELD = "align";
    private static final @NotNull String IMAGE_FIELD = "image";
    private static final @NotNull String ATTRIBUTES_FIELD = "attributes";
    private static final @NotNull String CHILDREN_FIELD = "children";

    private static final @NotNull Set<@NotNull String> DIALOG_FIELDS = Set.of(X_FIELD, Y_FIELD, WIDTH_FIELD, HEIGHT_FIELD, CONTROLS_FIELD);
    private static final @NotNull Set<@NotNull String> CONTROL_FIELDS = Set.of(ID_FIELD, KIND_FIELD, X_FIELD, Y_FIELD, WIDTH_FIELD, HEIGHT_FIELD,
                                                                               TEXT_FIELD, ALIGN_FIELD, IMAGE_FIELD, ATTRIBUTES_FIELD,
                                                                               CHILDREN_FIELD);
    private static final char NAME_SEPARATOR = '-';
    private static final char CONSTANT_SEPARATOR = '_';

    /**
     * Reads a dialog definition.
     *
     * @param source         the pack files
     * @param name           the dialog's id, its name for diagnostics
     * @param definitionPath the pack path of the dialog definition
     * @return the dialog definition
     * @throws IOException if the file cannot be read or a field is invalid
     */
    static @NotNull DialogDefinition read(@NotNull PackSource source, @NotNull String name, @NotNull String definitionPath) throws IOException {
        JsonObjectReader definition = JsonObjectReader.parse(source.read(definitionPath), definitionPath);
        Set<Integer> ids = new HashSet<>();

        checkFields(definition, DIALOG_FIELDS);

        try {
            return new DialogDefinition(name, definition.getInt(X_FIELD, 0), definition.getInt(Y_FIELD, 0), definition.getInt(WIDTH_FIELD),
                                        definition.getInt(HEIGHT_FIELD), readControls(definition, CONTROLS_FIELD, ids));
        } catch (IllegalArgumentException exception) {
            throw Exceptions.newIOException(exception, "{0}: {1}", definition.getLocation(), exception.getMessage());
        }
    }

    private static @NotNull List<@NotNull DialogControl> readControls(@NotNull JsonObjectReader parent, @NotNull String field,
                                                                      @NotNull Set<@NotNull Integer> ids) throws IOException {
        if (!parent.has(field)) {
            return List.of();
        }

        List<?> elements = parent.getArray(field);
        List<DialogControl> controls = new ArrayList<>(elements.size());

        for (int index = 0; index < elements.size(); index++) {
            controls.add(readControl(JsonObjectReader.of(elements.get(index), parent.getLocation(field) + "[" + index + "]"), ids));
        }

        return controls;
    }

    private static @NotNull DialogControl readControl(@NotNull JsonObjectReader control, @NotNull Set<@NotNull Integer> ids) throws IOException {
        checkFields(control, CONTROL_FIELDS);

        int id = control.getInt(ID_FIELD);

        if (!ids.add(id)) {
            throw Exceptions.newIOException("{0}: the control id {1} is used twice.", control.getLocation(ID_FIELD), id);
        }

        ControlKind kind = readConstant(control, KIND_FIELD, ControlKind.class);
        String alignment = control.findText(ALIGN_FIELD);
        String image = control.findText(IMAGE_FIELD);
        ResourceKey imageKey;

        try {
            imageKey = image != null ? ResourceKey.of(image) : null;
        } catch (IllegalArgumentException exception) {
            throw Exceptions.newIOException(exception, "{0}: invalid resource id {1}.", control.getLocation(IMAGE_FIELD), image);
        }

        Map<String, String> attributes = control.getTextMap(ATTRIBUTES_FIELD);

        return new DialogControl(id, kind,
                                 control.getInt(X_FIELD), control.getInt(Y_FIELD), control.getInt(WIDTH_FIELD), control.getInt(HEIGHT_FIELD),
                                 control.findText(TEXT_FIELD), alignment != null ? readConstant(control, ALIGN_FIELD, TextAlignment.class)
                                                                                 : TextAlignment.LEFT,
                                 imageKey, attributes, readControls(control, CHILDREN_FIELD, ids));
    }

    private static <E extends Enum<E>> @NotNull E readConstant(@NotNull JsonObjectReader object, @NotNull String field,
                                                               @NotNull Class<E> type) throws IOException {
        String text = object.getText(field);

        for (E constant : type.getEnumConstants()) {
            if (constant.name().toLowerCase(Locale.ROOT).replace(CONSTANT_SEPARATOR, NAME_SEPARATOR).equals(text)) {
                return constant;
            }
        }

        throw Exceptions.newIOException("{0}: unknown {1} {2}.", object.getLocation(field), field, text);
    }

    private static void checkFields(@NotNull JsonObjectReader object, @NotNull Set<@NotNull String> known) throws IOException {
        for (Object name : object.getNames()) {
            if (!known.contains(String.valueOf(name))) {
                throw Exceptions.newIOException("{0}: unknown field.", object.getLocation(String.valueOf(name)));
            }
        }
    }

    private PackDialogUtils() {
    }
}

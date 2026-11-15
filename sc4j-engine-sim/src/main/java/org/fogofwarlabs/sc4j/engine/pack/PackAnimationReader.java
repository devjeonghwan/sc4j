package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.animation.*;
import org.fogofwarlabs.sc4j.engine.content.type.ImageType;
import org.fogofwarlabs.sc4j.engine.content.type.SoundType;
import org.fogofwarlabs.sc4j.engine.json.JsonObjectReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the JSON animations of every sprite sheet of a pack into one {@link AnimationLibrary}.
 * <p>
 * Every sprite sheet is one animation set, of the index of the sheet in the pack. A sheet with animations is also one
 * program: its animations in file order, each a label at its first instruction. An animation whose name is a kind of
 * the pack is that kind's animation of the set; others are blocks to jump to or call. An object {@code {"label": name}}
 * in an animation labels the next instruction and is no instruction itself. Labels are resolved in a first pass, so
 * an instruction may name a label that comes later.
 */
final class PackAnimationReader {
    /**
     * The field of the animations of a sprite sheet definition.
     */
    static final @NotNull String ANIMATIONS_FIELD = "animations";
    /**
     * The field of an inline label.
     */
    static final @NotNull String LABEL_FIELD = "label";

    private final @NotNull PackSource source;
    private final @NotNull PackManifest manifest;
    private final @NotNull Map<@NotNull String, @NotNull PackInstructionType> instructionTypes;
    private final @NotNull List<@NotNull SoundType> soundTypes;
    private final @NotNull List<@NotNull ImageType> imageTypes;

    private final @NotNull List<@NotNull AnimationKind> kinds;

    /**
     * Creates a {@link PackAnimationReader} instance.
     *
     * @param source           the pack files
     * @param manifest         the pack manifest
     * @param instructionTypes the instruction types by keyword
     * @param soundTypes       the sound types of the pack in sound index order
     * @param imageTypes       the image types of the pack in sprite sheet index order
     * @throws IllegalArgumentException if a kind name is empty
     */
    PackAnimationReader(@NotNull PackSource source, @NotNull PackManifest manifest,
                        @NotNull Map<@NotNull String, @NotNull PackInstructionType> instructionTypes,
                        @NotNull List<@NotNull SoundType> soundTypes, @NotNull List<@NotNull ImageType> imageTypes) {
        this.source           = source;
        this.manifest         = manifest;
        this.instructionTypes = instructionTypes;
        this.soundTypes       = soundTypes;
        this.imageTypes       = imageTypes;

        List<String> kindNames = manifest.getAnimationKinds();
        List<AnimationKind> packKinds = new ArrayList<>(kindNames.size());

        for (int index = 0; index < kindNames.size(); index++) {
            packKinds.add(AnimationKind.of(index, kindNames.get(index)));
        }

        this.kinds = List.copyOf(packKinds);
    }

    /**
     * Reads the library.
     *
     * @return the library
     * @throws IOException if a definition cannot be read or an animation is invalid
     */
    @NotNull AnimationLibrary read() throws IOException {
        List<AnimationProgram> programs = new ArrayList<>();
        List<AnimationSet> sets = new ArrayList<>();
        List<String> sheetIds = this.manifest.getSpriteSheetIds();

        for (int setIndex = 0; setIndex < sheetIds.size(); setIndex++) {
            String sheetId = sheetIds.get(setIndex);
            String definitionPath = this.manifest.getSpriteSheetPath(sheetId);
            JsonObjectReader definition = JsonObjectReader.parse(this.source.read(definitionPath), definitionPath);
            AnimationSet.Builder set = new AnimationSet.Builder(setIndex, sheetId);

            if (definition.has(ANIMATIONS_FIELD)) {
                this.readProgram(definition, sheetId, programs, set);
            }

            sets.add(set.build());
        }

        return new AnimationLibrary(this.kinds, programs, sets);
    }

    private void readProgram(@NotNull JsonObjectReader definition, @NotNull String sheetId, @NotNull List<@NotNull AnimationProgram> programs,
                             @NotNull AnimationSet.Builder set) throws IOException {
        JsonObjectReader animations = definition.getObject(ANIMATIONS_FIELD);
        int frameLimit = definition.getInt(PackSpriteSheetUtils.COLUMNS_FIELD);
        Map<String, Integer> labels = new LinkedHashMap<>();
        int length = 0;

        for (Object name : animations.getNames()) {
            String animationName = String.valueOf(name);
            List<?> elements = animations.getArray(animationName);

            if (elements.isEmpty()) {
                throw Exceptions.newIOException("{0}: the animation {1} is empty.", animations.getLocation(animationName), animationName);
            }

            this.addLabel(labels, animationName, length, animations.getLocation(animationName));

            for (int index = 0; index < elements.size(); index++) {
                JsonObjectReader element = JsonObjectReader.of(elements.get(index), animations.getLocation(animationName) + "[" + index + "]");
                String label = this.findInlineLabel(element);

                if (label != null) {
                    this.addLabel(labels, label, length, element.getLocation());
                } else {
                    length++;
                }
            }
        }

        if (length == 0) {
            throw Exceptions.newIOException("{0}: the sprite sheet {1} has animations without instructions.", animations.getLocation(), sheetId);
        }

        List<AnimationInstruction> instructions = new ArrayList<>(length);

        for (Object name : animations.getNames()) {
            String animationName = String.valueOf(name);
            List<?> elements = animations.getArray(animationName);

            for (int index = 0; index < elements.size(); index++) {
                JsonObjectReader element = JsonObjectReader.of(elements.get(index), animations.getLocation(animationName) + "[" + index + "]");

                if (this.findInlineLabel(element) == null) {
                    instructions.add(this.readInstruction(element, labels, frameLimit));
                }
            }
        }

        List<AnimationLabel> programLabels = new ArrayList<>(labels.size());

        for (Map.Entry<String, Integer> label : labels.entrySet()) {
            if (label.getValue() >= length) {
                throw Exceptions.newIOException("{0}: the label {1} is after the last instruction.", animations.getLocation(), label.getKey());
            }

            programLabels.add(new AnimationLabel(label.getKey(), label.getValue()));
        }

        AnimationProgram program = new AnimationProgram(programs.size(), sheetId, instructions, programLabels);

        programs.add(program);

        for (AnimationKind kind : this.kinds) {
            Integer start = labels.get(kind.getName());

            if (start != null && animations.has(kind.getName())) {
                set.setAnimation(kind, program, start);
            }
        }
    }

    private void addLabel(@NotNull Map<@NotNull String, @NotNull Integer> labels, @NotNull String label, int position, @NotNull String location) throws IOException {
        if (label.isEmpty() || labels.putIfAbsent(label, position) != null) {
            throw Exceptions.newIOException("{0}: the label {1} is empty or defined twice.", location, label);
        }
    }

    private @Nullable String findInlineLabel(@NotNull JsonObjectReader element) throws IOException {
        if (!element.has(LABEL_FIELD)) {
            return null;
        }

        if (element.getNames().size() != 1) {
            throw Exceptions.newIOException("{0}: a label object holds only the label.", element.getLocation());
        }

        return element.getText(LABEL_FIELD);
    }

    private @NotNull AnimationInstruction readInstruction(@NotNull JsonObjectReader element, @NotNull Map<@NotNull String, @NotNull Integer> labels,
                                                          int frameLimit) throws IOException {
        PackInstructionType type = null;

        for (Object name : element.getNames()) {
            PackInstructionType named = this.instructionTypes.get(String.valueOf(name));

            if (named != null) {
                if (type != null) {
                    throw Exceptions.newIOException("{0}: an instruction has one keyword, found {1} and {2}.", element.getLocation(), type.getKeyword(),
                                                    named.getKeyword());
                }

                type = named;
            }
        }

        if (type == null) {
            throw Exceptions.newIOException("{0}: no instruction keyword in {1}.", element.getLocation(), element.getNames());
        }

        PackInstructionReader reader = new PackInstructionReader(element, type.getKeyword(), labels, this.soundTypes, this.imageTypes, frameLimit);

        return type.read(reader);
    }
}

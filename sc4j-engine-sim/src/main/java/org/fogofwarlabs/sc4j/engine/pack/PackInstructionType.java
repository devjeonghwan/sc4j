package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Reads one kind of instruction of the JSON animations of a standard pack, named by its keyword: the field of the
 * instruction object that says what it is, such as {@code "wait"} in {@code {"wait": 4}}. The pack has a type for every
 * engine instruction; a mod adds its own types for its own instructions when it opens a pack
 * ({@link StandardPack#open(java.nio.file.Path, java.util.List)}).
 */
public interface PackInstructionType {
    /**
     * @return the keyword, unique among the types of a pack
     */
    @NotNull String getKeyword();

    /**
     * Creates the instruction of one instruction object.
     *
     * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
     * @return the created instruction
     * @throws IOException if the object is not a valid instruction of this type
     */
    @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException;
}

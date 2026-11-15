package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.fogofwarlabs.sc4j.engine.animation.instruction.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Reads the engine instructions of the pack animations; the package description lists their JSON form.
 */
enum StandardInstructionType implements PackInstructionType {
    /**
     * {@code {"frame": n}}: {@link ShowFrame} of frame number {@code n}, below the sheet's frame limit.
     */
    FRAME("frame") {
        /**
         * Creates the instruction of one instruction object.
         *
         * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
         * @return the created instruction
         * @throws IOException if the object is not a valid instruction of this type
         */
        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            int frame = reader.getIntValue();

            if (frame < 0 || frame >= reader.getFrameLimit()) {
                throw reader.createException("frame {0} is outside the sheet's 0..{1}", frame, reader.getFrameLimit() - 1);
            }

            return new ShowFrame(frame);
        }
    },
    /**
     * {@code {"wait": t}}: {@link Wait} of {@code t} ticks.
     */
    WAIT("wait") {
        /**
         * Creates the instruction of one instruction object.
         *
         * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
         * @return the created instruction
         * @throws IOException if the object is not a valid instruction of this type
         */
        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            int ticks = reader.getIntValue();

            if (ticks < 1) {
                throw reader.createException("a wait lasts at least 1 tick: {0}", ticks);
            }

            return new Wait(ticks);
        }
    },
    /**
     * {@code {"waitRandom": [a, b]}}: {@link WaitRandom} of {@code a} up to {@code b} ticks.
     */
    WAIT_RANDOM("waitRandom") {
        /**
         * Creates the instruction of one instruction object.
         *
         * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
         * @return the created instruction
         * @throws IOException if the object is not a valid instruction of this type
         */
        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            int[] range = reader.getIntValues(RANGE_LENGTH);

            if (range[0] < 1 || range[1] < range[0]) {
                throw reader.createException("a random wait lasts 1 tick or more, minimum first: {0}..{1}", range[0], range[1]);
            }

            return new WaitRandom(range[0], range[1]);
        }
    },
    /**
     * {@code {"jump": label}}: {@link Jump} to a label of the program.
     */
    JUMP("jump") {
        /**
         * Creates the instruction of one instruction object.
         *
         * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
         * @return the created instruction
         * @throws IOException if the object is not a valid instruction of this type
         */
        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            return new Jump(reader.resolveLabel(reader.getTextValue()));
        }
    },
    /**
     * {@code {"randomJump": label, "chance": c}}: {@link RandomJump} to a label with a chance of {@code c} in {@value RandomJump#CHANCE_RANGE}.
     */
    RANDOM_JUMP("randomJump") {
        /**
         * Creates the instruction of one instruction object.
         *
         * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
         * @return the created instruction
         * @throws IOException if the object is not a valid instruction of this type
         */
        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            int target = reader.resolveLabel(reader.getTextValue());
            int chance = reader.getInt(CHANCE_FIELD);

            if (chance < 0 || chance > RandomJump.CHANCE_RANGE) {
                throw reader.createException("the chance must be 0..{0}: {1}", RandomJump.CHANCE_RANGE, chance);
            }

            return new RandomJump(chance, target);
        }
    },
    /**
     * {@code {"call": label}}: {@link Call} of the block at a label.
     */
    CALL("call") {
        /**
         * Creates the instruction of one instruction object.
         *
         * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
         * @return the created instruction
         * @throws IOException if the object is not a valid instruction of this type
         */
        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            return new Call(reader.resolveLabel(reader.getTextValue()));
        }
    },
    /**
     * {@code {"return": true}}: {@link Return}.
     */
    RETURN("return") {
        /**
         * Creates the instruction of one instruction object.
         *
         * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
         * @return the created instruction
         * @throws IOException if the object is not a valid instruction of this type
         */
        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            reader.requireTrueValue();

            return Return.INSTANCE;
        }
    },
    /**
     * {@code {"end": true}}: {@link End}.
     */
    END("end") {
        /**
         * Creates the instruction of one instruction object.
         *
         * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
         * @return the created instruction
         * @throws IOException if the object is not a valid instruction of this type
         */
        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            reader.requireTrueValue();

            return End.INSTANCE;
        }
    },
    /**
     * {@code {"sound": id}}: {@link PlaySound} of a sound of the pack.
     */
    SOUND("sound") {
        /**
         * Creates the instruction of one instruction object.
         *
         * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
         * @return the created instruction
         * @throws IOException if the object is not a valid instruction of this type
         */
        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            return new PlaySound(reader.resolveSound(reader.getTextValue()));
        }
    },
    /**
     * {@code {"spawnOverlay": id, "x": dx, "y": dy}}: {@link SpawnOverlay} of a sprite sheet of the pack; the offsets default to {@code 0}.
     */
    SPAWN_OVERLAY("spawnOverlay") {
        /**
         * Creates the instruction of one instruction object.
         *
         * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
         * @return the created instruction
         * @throws IOException if the object is not a valid instruction of this type
         */
        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            return new SpawnOverlay(reader.resolveSpriteSheet(reader.getTextValue()),
                                    reader.getInt(OFFSET_X_FIELD, 0), reader.getInt(OFFSET_Y_FIELD, 0));
        }
    },
    /**
     * {@code {"attack": slot}}: {@link FireWeapon} of a weapon slot number, or of {@code "primary"} (slot {@code 0}) or
     * {@code "secondary"} (slot {@code 1}).
     */
    ATTACK("attack") {
        /**
         * Creates the instruction of one instruction object.
         *
         * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
         * @return the created instruction
         * @throws IOException if the object is not a valid instruction of this type
         */
        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            if (!(reader.getValue() instanceof String slotName)) {
                int slot = reader.getIntValue();

                if (slot < 0) {
                    throw reader.createException("the weapon slot must not be negative: {0}", slot);
                }

                return new FireWeapon(slot);
            }

            return switch (slotName) {
                case PRIMARY_SLOT_NAME -> new FireWeapon(PRIMARY_SLOT);
                case SECONDARY_SLOT_NAME -> new FireWeapon(SECONDARY_SLOT);
                default -> throw reader.createException("unknown weapon slot {0}", slotName);
            };
        }
    },
    /**
     * {@code {"move": d}}: {@link Move} of {@code d} pixels.
     */
    MOVE("move") {
        /**
         * Creates the instruction of one instruction object.
         *
         * @param reader the instruction object, its keyword value, arguments and the names the pack resolves
         * @return the created instruction
         * @throws IOException if the object is not a valid instruction of this type
         */
        @Override
        public @NotNull AnimationInstruction read(@NotNull PackInstructionReader reader) throws IOException {
            return new Move(reader.getIntValue());
        }
    };

    /**
     * The number of values of a range, minimum and maximum.
     */
    private static final int RANGE_LENGTH = 2;
    /**
     * The field of the chance of a random jump.
     */
    private static final @NotNull String CHANCE_FIELD = "chance";
    /**
     * The field of the horizontal offset of an overlay.
     */
    private static final @NotNull String OFFSET_X_FIELD = "x";
    /**
     * The field of the vertical offset of an overlay.
     */
    private static final @NotNull String OFFSET_Y_FIELD = "y";
    /**
     * The name of the first weapon slot.
     */
    private static final @NotNull String PRIMARY_SLOT_NAME = "primary";
    /**
     * The name of the second weapon slot.
     */
    private static final @NotNull String SECONDARY_SLOT_NAME = "secondary";
    /**
     * The first weapon slot.
     */
    private static final int PRIMARY_SLOT = 0;
    /**
     * The second weapon slot.
     */
    private static final int SECONDARY_SLOT = 1;

    private final @NotNull String keyword;

    StandardInstructionType(@NotNull String keyword) {
        this.keyword = keyword;
    }

    /**
     * @return the keyword, unique among the types of a pack
     */
    @Override
    public @NotNull String getKeyword() {
        return this.keyword;
    }
}

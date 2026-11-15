package org.fogofwarlabs.sc4j.engine.client.scene;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the scene stack: first scene, deferred and immediate switches, open failures, the retained scene, transitions,
 * modal scenes and the close order.
 */
final class SceneStackTest {
    private static final long FIRST_TIME = 100;
    private static final long SECOND_TIME = 200;
    private static final long THIRD_TIME = 300;
    private static final long TRANSITION_LENGTH = 150;

    /**
     * The events of the scenes, the transitions and the listener of the running test.
     */
    private static final @NotNull List<String> EVENTS = new ArrayList<>();

    /**
     * Records what the stack does to a scene in a shared event list.
     */
    private static final class RecordingScene implements Scene<List<String>, List<String>> {
        private final @NotNull String name;
        private final boolean failing;

        private RecordingScene(@NotNull String name, boolean failing) {
            this.name    = name;
            this.failing = failing;
        }

        /**
         * Opens the scene: loads what it shows. Called once, before the first {@link #update}.
         *
         * @param context the client's shared services
         * @throws IOException if a resource of the scene cannot be read
         */
        @Override
        public void open(@NotNull List<String> context) throws IOException {
            context.add("open " + this.name);

            if (this.failing) {
                throw Exceptions.newIOException("missing {0}", this.name);
            }
        }

        /**
         * Updates the scene at the supplied time.
         *
         * @param time the client time in milliseconds
         */
        @Override
        public void update(long time) {
            EVENTS.add("update " + this.name + " " + time);
        }

        /**
         * Draws the scene.
         *
         * @param surface the surface
         */
        @Override
        public void draw(@NotNull List<String> surface) {
            surface.add("draw " + this.name);
        }

        /**
         * Handles an input event the scene's stack hands to it ({@link SceneStack#handleInput}): by default while it is the top scene.
         *
         * @param event the event
         * @return {@code true}: the scene uses every event
         */
        @Override
        public boolean handleInput(@NotNull InputEvent event) {
            EVENTS.add("input " + this.name + " " + event.getType());

            return true;
        }

        /**
         * Closes the scene: releases what it holds. A scene that was never opened is closed only if the stack opened it.
         */
        @Override
        public void close() {
            EVENTS.add("close " + this.name);
        }
    }

    /**
     * Completes a fixed time after its start and draws a marker while it runs.
     */
    private static final class TimedTransition implements SceneTransition<List<String>> {
        private long end;

        private TimedTransition() {
            this.end = 0;
        }

        /**
         * Starts the transition at the supplied time; called once, on the first frame the stack applies it.
         *
         * @param time the client time in milliseconds
         */
        @Override
        public void start(long time) {
            this.end = time + TRANSITION_LENGTH;
            EVENTS.add("transition start " + time);
        }

        /**
         * Advances the transition.
         *
         * @param time the client time in milliseconds
         * @return {@code true} if the transition is complete and the stack switches scenes, {@code false} otherwise
         */
        @Override
        public boolean update(long time) {
            return time >= this.end;
        }

        /**
         * Draws the transition over the scenes.
         *
         * @param surface the surface
         */
        @Override
        public void draw(@NotNull List<String> surface) {
            surface.add("draw transition");
        }
    }

    private static @NotNull SceneStack<List<String>, List<String>, RecordingScene> createStack() {
        EVENTS.clear();

        return new SceneStack<>(EVENTS, new SceneStack.Listener<>() {
            /**
             * Called after a scene became current: the stack opened it and closed the left scene.
             *
             * @param previous the left scene, or {@code null} for the first scene
             * @param next     the new current scene
             */
            @Override
            public void onSwitched(@Nullable RecordingScene previous, @NotNull RecordingScene next) {
                EVENTS.add("switched " + (previous == null ? "-" : previous.name) + " " + next.name);
            }

            /**
             * Called when a scene the stack switches to fails to open; the current scene stays.
             *
             * @param scene     the scene that failed
             * @param exception what its open threw
             */
            @Override
            public void onOpenFailed(@NotNull RecordingScene scene, @NotNull Exception exception) {
                EVENTS.add("failed " + scene.name + " " + exception.getMessage());
            }
        });
    }

    @Test
    void replace_should_switch_when_the_pending_scene_is_applied() throws IOException {
        SceneStack<List<String>, List<String>, RecordingScene> stack = createStack();
        RecordingScene title = new RecordingScene("title", false);
        RecordingScene menu = new RecordingScene("menu", false);

        stack.start(title);
        stack.replace(menu);
        stack.update(FIRST_TIME);
        stack.applyPendingScene(SECOND_TIME);
        stack.update(SECOND_TIME);

        Assertions.assertEquals(List.of("open title", "switched - title", "update title 100", "open menu", "close title", "switched title menu",
                                        "update menu 200"), EVENTS);
        Assertions.assertSame(menu, stack.requireScene());
        Assertions.assertNull(stack.getPendingScene());
    }

    @Test
    void switch_to_should_keep_the_current_scene_when_the_next_fails_to_open() throws IOException {
        SceneStack<List<String>, List<String>, RecordingScene> stack = createStack();
        RecordingScene title = new RecordingScene("title", false);

        stack.start(title);

        Assertions.assertFalse(stack.switchTo(new RecordingScene("broken", true)));
        Assertions.assertSame(title, stack.requireScene());
        Assertions.assertEquals(List.of("open title", "switched - title", "open broken", "failed broken missing broken"), EVENTS);
    }

    @Test
    void switch_to_should_keep_the_retained_scene_open_until_the_stack_closes() throws IOException {
        SceneStack<List<String>, List<String>, RecordingScene> stack = createStack();
        RecordingScene title = new RecordingScene("title", false);
        RecordingScene menu = new RecordingScene("menu", false);

        stack.setRetainedScene(title);
        stack.start(title);
        stack.switchTo(menu);
        stack.close();

        Assertions.assertEquals(List.of("open title", "switched - title", "open menu", "switched title menu", "close menu", "close title"), EVENTS);
        Assertions.assertNull(stack.getScene());
    }

    @Test
    void transition_defers_the_switch_until_it_completes_and_draws_over_the_scene() throws IOException {
        SceneStack<List<String>, List<String>, RecordingScene> stack = createStack();
        RecordingScene title = new RecordingScene("title", false);
        RecordingScene menu = new RecordingScene("menu", false);
        List<String> surface = new ArrayList<>();

        stack.start(title);
        stack.replace(menu, new TimedTransition());
        stack.applyPendingScene(FIRST_TIME);
        stack.draw(surface);
        stack.applyPendingScene(SECOND_TIME);

        Assertions.assertSame(title, stack.requireScene());

        stack.applyPendingScene(THIRD_TIME);
        stack.draw(surface);

        Assertions.assertSame(menu, stack.requireScene());
        Assertions.assertEquals(List.of("draw title", "draw transition", "draw menu"), surface);
        Assertions.assertEquals(List.of("open title", "switched - title", "transition start 100", "open menu", "close title", "switched title menu"),
                                EVENTS);
    }

    @Test
    void modal_scenes_draw_over_the_scene_and_take_the_input() throws IOException {
        SceneStack<List<String>, List<String>, RecordingScene> stack = createStack();
        RecordingScene menu = new RecordingScene("menu", false);
        RecordingScene options = new RecordingScene("options", false);
        RecordingScene confirm = new RecordingScene("confirm", false);
        List<String> surface = new ArrayList<>();

        stack.start(menu);
        stack.pushModal(options);
        stack.pushModal(confirm);

        Assertions.assertSame(confirm, stack.getTopScene());

        stack.draw(surface);
        stack.popModal();

        Assertions.assertSame(options, stack.getTopScene());

        stack.close();

        Assertions.assertEquals(List.of("draw menu", "draw options", "draw confirm"), surface);
        Assertions.assertEquals(List.of("open menu", "switched - menu", "open options", "open confirm",
                                        "close confirm", "close options", "close menu"), EVENTS);
        Assertions.assertFalse(stack.hasModal());
    }

    @Test
    void handle_input_should_go_to_the_top_modal_scene_else_the_current_scene() throws IOException {
        SceneStack<List<String>, List<String>, RecordingScene> stack = createStack();
        RecordingScene menu = new RecordingScene("menu", false);
        RecordingScene options = new RecordingScene("options", false);

        Assertions.assertFalse(stack.handleInput(InputEvent.createCharacterTyped('a')), "no scene yet");

        stack.start(menu);
        stack.handleInput(InputEvent.createCharacterTyped('a'));
        stack.pushModal(options);
        stack.handleInput(InputEvent.createPointerMoved(1, 2));
        stack.popModal();
        stack.handleInput(InputEvent.createKey(InputEvent.Type.KEY_PRESSED, Key.ESCAPE, 0));
        stack.close();

        Assertions.assertEquals(List.of("open menu", "switched - menu", "input menu CHARACTER_TYPED", "open options", "input options POINTER_MOVED",
                                        "close options", "input menu KEY_PRESSED", "close menu"), EVENTS);
    }

    @Test
    void handle_input_should_go_to_the_scene_the_input_router_chooses() throws IOException {
        SceneStack<List<String>, List<String>, RecordingScene> stack = createStack();
        RecordingScene menu = new RecordingScene("menu", false);
        RecordingScene options = new RecordingScene("options", false);

        stack.start(menu);
        stack.pushModal(options);
        stack.setInputRouter(event -> event.isPointerEvent() ? menu : null);
        stack.handleInput(InputEvent.createPointerMoved(1, 2));
        stack.handleInput(InputEvent.createCharacterTyped('a'));
        stack.setInputRouter(null);
        stack.handleInput(InputEvent.createPointerMoved(1, 2));
        stack.close();

        Assertions.assertEquals(List.of("open menu", "switched - menu", "open options", "input menu POINTER_MOVED", "input options CHARACTER_TYPED",
                                        "input options POINTER_MOVED", "close options", "close menu"), EVENTS);
    }
}

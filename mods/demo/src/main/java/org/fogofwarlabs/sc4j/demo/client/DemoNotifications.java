package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Tells the person what happens to their units, from the game view after each update: a unit that appears complete is ready, a building
 * whose construction completes is done, and a unit that loses hit points is under attack (at most once per {@value #ATTACK_INTERVAL} game
 * loops), which also pings the minimap where the unit stands. Each notification plays its pack sound and shows a message.
 */
public final class DemoNotifications {
    /**
     * The game loops between two under-attack notifications.
     */
    public static final int ATTACK_INTERVAL = 240;
    /**
     * The colour of the minimap ping of an attack, packed {@code 0xRRGGBBAA}.
     */
    public static final int ATTACK_PING_COLOR = 0xFF3020FF;

    private final @NotNull DemoClient demo;
    private final @NotNull DemoGameScreen screen;
    private final @NotNull Player player;

    private final @NotNull Map<@NotNull Unit, @NotNull UnitType> types;
    private final @NotNull Map<@NotNull Unit, @NotNull Integer> hitPoints;
    private final @NotNull Set<@NotNull Unit> constructing;
    private final @NotNull Set<@NotNull Unit> seen;
    private boolean started;
    private int lastAttackFrame;

    /**
     * Creates a {@link DemoNotifications} instance; the first update only takes in the units of the start.
     *
     * @param demo   the demo's shared services
     * @param screen the screen that shows the messages
     * @param player the person's player
     */
    public DemoNotifications(@NotNull DemoClient demo, @NotNull DemoGameScreen screen, @NotNull Player player) {
        this.demo   = demo;
        this.screen = screen;
        this.player = player;

        this.types           = new IdentityHashMap<>();
        this.hitPoints       = new IdentityHashMap<>();
        this.constructing    = new HashSet<>();
        this.seen            = new HashSet<>();
        this.started         = false;
        this.lastAttackFrame = -ATTACK_INTERVAL;
    }

    /**
     * Compares the person's units with the last update and notifies what changed.
     *
     * @param view the game view
     */
    public void update(@NotNull GameView view) {
        List<Unit> units = view.getUnits();
        UnitView attacked = null;

        this.seen.clear();

        for (Unit unit : units) {
            UnitView unitView = view.getUnitView(unit);
            UnitType type = unitView.getType();

            if (unitView.getOwner() != this.player || type == null) {
                continue;
            }

            this.seen.add(unit);

            boolean complete = unitView.getConstructionProgress() >= UnitView.COMPLETE;
            UnitType known = this.types.put(unit, type);
            Integer previousHitPoints = this.hitPoints.put(unit, unitView.getHitPoints());

            if (known != type) {
                if (!complete) {
                    this.constructing.add(unit);
                } else if (this.started) {
                    this.notify(DemoSounds.UNIT_READY, DemoClient.getName(type) + " ready.");
                }

                continue;
            }

            if (complete && this.constructing.remove(unit)) {
                this.notify(DemoSounds.CONSTRUCTION_DONE, DemoClient.getName(type) + " built.");
            }

            if (attacked == null && previousHitPoints != null && unitView.getHitPoints() < previousHitPoints && complete) {
                attacked = unitView;
            }
        }

        this.types.keySet().retainAll(this.seen);
        this.hitPoints.keySet().retainAll(this.seen);
        this.constructing.retainAll(this.seen);
        this.started = true;

        if (attacked != null && view.getFrame() - this.lastAttackFrame >= ATTACK_INTERVAL) {
            this.lastAttackFrame = view.getFrame();
            this.screen.getScreen().getMinimap().addPing(attacked.getX(), attacked.getY(), ATTACK_PING_COLOR);
            this.notify(DemoSounds.UNDER_ATTACK, "Under attack!");
        }
    }

    private void notify(@NotNull String sound, @NotNull String message) {
        this.demo.getSounds().play(sound);
        this.screen.addMessage(message);
    }
}

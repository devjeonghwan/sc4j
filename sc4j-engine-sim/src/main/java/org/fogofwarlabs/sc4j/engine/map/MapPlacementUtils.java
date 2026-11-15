package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.faction.FactionDefinition;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.economy.HarvestSystem;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.PlayerSlot;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Puts the units of a map into a new game with the engine production default ({@link StandardProductionSystem#createUnit}): the start units of
 * every player, the preplaced units and the resource containers of the engine map format.
 */
public final class MapPlacementUtils {
    /**
     * The instruction limit of the animation programs the placement starts.
     */
    private static final int INSTRUCTION_LIMIT = 0;

    /**
     * Places the units of a map, before the first frame, in this order: for a map of the engine format, the resource containers in map order for
     * the neutral player, filled through the harvest system if the game has one; the preplaced units in map order for the players of their slots;
     * then, for every player slot of the setup in ascending order that has a faction and a start location on the map, the faction's town hall at the
     * start location and each start unit of the faction at the free position nearest to it ({@link FactionDefinition#getTownHall},
     * {@link FactionDefinition#getStartUnits}). A unit whose slot the game does not have, or that finds no free position, is not placed.
     *
     * @param game the game
     * @param map  the map
     * @throws IllegalStateException if the game's production system is not the engine default
     */
    public static void placeUnits(@NotNull Game game, @NotNull MapModel map) {
        if (!(game.getProduction() instanceof StandardProductionSystem production)) {
            throw Exceptions.newIllegalStateException("Placing the units of a map needs the standard production system.");
        }

        StandardMapData data = map.getModData() instanceof StandardMapData standard ? standard : null;

        if (data != null && data.getNeutralSlotIndex() < game.getPlayerCount()) {
            HarvestSystem harvest = game.hasSystem(HarvestSystem.KEY) ? game.getModSystem(HarvestSystem.KEY) : null;

            for (MapResource resource : data.getResources()) {
                Unit container = production.createUnit(game.getPlayer(data.getNeutralSlotIndex()), resource.getType(), resource.getX(),
                                                       resource.getY(), INSTRUCTION_LIMIT);

                if (container != null && harvest != null) {
                    harvest.setResources(container, resource.getKind(), resource.getAmount());
                }
            }
        }

        for (PreplacedUnit unit : map.getPreplacedUnits()) {
            if (unit.getOwnerSlotIndex() < game.getPlayerCount()) {
                production.createUnit(game.getPlayer(unit.getOwnerSlotIndex()), unit.getType(), unit.getX(), unit.getY(), INSTRUCTION_LIMIT);
            }
        }

        for (PlayerSlot slot : game.getPlayers()) {
            Faction faction = slot.getFaction();
            StartLocation start = findStartLocation(map, slot.getSlotIndex());

            if (faction == null || start == null || slot.getSlotIndex() >= game.getPlayerCount()) {
                continue;
            }

            FactionDefinition definition = game.getContent().getDefinition().getFactions().getDefinition(faction);
            UnitType townHall = definition.getTownHall();

            if (townHall != null) {
                production.createUnit(game.getPlayer(slot.getSlotIndex()), townHall, start.getX(), start.getY(), INSTRUCTION_LIMIT);
            }

            for (UnitType startUnit : definition.getStartUnits()) {
                production.createUnit(game.getPlayer(slot.getSlotIndex()), startUnit, start.getX(), start.getY(), INSTRUCTION_LIMIT);
            }
        }
    }

    private static @Nullable StartLocation findStartLocation(@NotNull MapModel map, int slotIndex) {
        for (StartLocation location : map.getStartLocations()) {
            if (location.getSlotIndex() == slotIndex) {
                return location;
            }
        }

        return null;
    }

    private MapPlacementUtils() {
    }
}

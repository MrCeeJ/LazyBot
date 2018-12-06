package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.protocol.data.Units;

import java.util.List;
import java.util.Map;

import static com.github.ocraft.s2client.protocol.data.Units.*;
import static java.util.Map.entry;

class TechUtils {

    private static final Map<Units, List<Units>> techRequirements = Map.ofEntries(
            entry(TERRAN_COMMAND_CENTER, List.of()),
            entry(TERRAN_ORBITAL_COMMAND, List.of(TERRAN_BARRACKS)),
            entry(TERRAN_PLANETARY_FORTRESS, List.of(TERRAN_ENGINEERING_BAY)),
            entry(TERRAN_SUPPLY_DEPOT, List.of()),
            entry(TERRAN_REFINERY, List.of()),
            entry(TERRAN_BARRACKS, List.of(TERRAN_SUPPLY_DEPOT)),
            entry(TERRAN_ENGINEERING_BAY, List.of()),
            entry(TERRAN_BUNKER, List.of(TERRAN_BARRACKS)),
            entry(TERRAN_MISSILE_TURRET, List.of(TERRAN_ENGINEERING_BAY)),
            entry(TERRAN_SENSOR_TOWER, List.of(TERRAN_ENGINEERING_BAY)),
            entry(TERRAN_FACTORY, List.of(TERRAN_BARRACKS)),
            entry(TERRAN_GHOST_ACADEMY, List.of(TERRAN_BARRACKS)),
            entry(TERRAN_ARMORY, List.of(TERRAN_FACTORY)),
            entry(TERRAN_STARPORT, List.of(TERRAN_FACTORY)),
            entry(TERRAN_FUSION_CORE, List.of(TERRAN_STARPORT)),
            entry(TERRAN_BARRACKS_TECHLAB, List.of(TERRAN_BARRACKS)),
            entry(TERRAN_BARRACKS_REACTOR, List.of(TERRAN_BARRACKS)),
            entry(TERRAN_FACTORY_TECHLAB, List.of(TERRAN_FACTORY)),
            entry(TERRAN_FACTORY_REACTOR, List.of(TERRAN_FACTORY)),
            entry(TERRAN_STARPORT_TECHLAB, List.of(TERRAN_STARPORT)),
            entry(TERRAN_STARPORT_REACTOR, List.of(TERRAN_STARPORT))
    );


    static Map<Units, List<Units>> getTechRequirements() {
       return techRequirements;
    }
}

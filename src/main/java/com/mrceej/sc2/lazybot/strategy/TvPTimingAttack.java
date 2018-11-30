package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.protocol.data.Units;

import java.util.List;

public class TvPTimingAttack implements Doctrine {
    @Override
    public void calculateUrgency() {

    }

    private int buildOrderPosition = 0;

    private List<Units> buildOrder = List.of(
            Units.TERRAN_BARRACKS,
            Units.TERRAN_REFINERY,
            Units.TERRAN_REAPER,
            Units.TERRAN_ORBITAL_COMMAND,
            Units.TERRAN_COMMAND_CENTER,
            Units.TERRAN_BARRACKS,
            Units.TERRAN_BARRACKS,
            Units.TERRAN_BARRACKS_REACTOR,
            Units.TERRAN_REFINERY,
            Units.TERRAN_FACTORY,
            Units.TERRAN_BARRACKS,
            Units.TERRAN_BARRACKS_TECHLAB,
            Units.TERRAN_ORBITAL_COMMAND,
            //Stimpack
            Units.TERRAN_STARPORT,
            Units.TERRAN_FACTORY,
            Units.TERRAN_STARPORT_REACTOR,
            Units.TERRAN_MEDIVAC,
            Units.TERRAN_MEDIVAC,       //Load Marines and move out when Units.TERRAN_MEDIVACs pop out
            Units.TERRAN_ENGINEERING_BAY,
            Units.TERRAN_ENGINEERING_BAY,
            // Combat Shield
            Units.TERRAN_COMMAND_CENTER,
            // Terran Infantry Weapons Level 1
            //  Terran Infantry Armor Level 1
            Units.TERRAN_REFINERY,
            Units.TERRAN_REFINERY,
            Units.TERRAN_BARRACKS,
            Units.TERRAN_BARRACKS,
            Units.TERRAN_BARRACKS,
            Units.TERRAN_ARMORY,
            Units.TERRAN_ORBITAL_COMMAND,
            Units.TERRAN_BARRACKS, Units.TERRAN_BARRACKS_REACTOR,
            Units.TERRAN_BARRACKS, Units.TERRAN_BARRACKS_REACTOR,
            Units.TERRAN_BARRACKS, Units.TERRAN_BARRACKS_REACTOR,
            Units.TERRAN_FACTORY, Units.TERRAN_FACTORY_TECHLAB,
            // Terran Infantry Weapons Level 2
            //  Terran Infantry Armor Level 2
            //   Terran Vehicle and Ship Weapons Level 1
            Units.TERRAN_REFINERY,
            Units.TERRAN_REFINERY,
            Units.TERRAN_COMMAND_CENTER,
            Units.TERRAN_FACTORY,
            Units.TERRAN_BARRACKS,
            Units.TERRAN_BARRACKS,
            Units.TERRAN_BARRACKS,
            Units.TERRAN_FACTORY, Units.TERRAN_FACTORY_TECHLAB,
            //      Terran Infantry Weapons Level 3
            //      Terran Infantry Armor Level 3
            Units.TERRAN_BARRACKS, Units.TERRAN_BARRACKS_TECHLAB,
            Units.TERRAN_BARRACKS, Units.TERRAN_BARRACKS_TECHLAB,
            Units.TERRAN_BARRACKS, Units.TERRAN_BARRACKS_TECHLAB,
            Units.TERRAN_GHOST_ACADEMY,
            Units.TERRAN_PLANETARY_FORTRESS
    );

    @Override
    public Units getConstructionOrder() {
        return buildOrder.get(buildOrderPosition);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void debugStatus() {

    }
}

package com.mrceej.sc2.lazybot.lazyBot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.strategy.Doctrine;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.Utils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.github.ocraft.s2client.protocol.data.Units.*;

public class DynamicBuildOrder extends Doctrine {

    private static final int SUPPLY_BUFFER = 2;

    private static final List<Units> macroStructures = List.of(
            TERRAN_BARRACKS,
            TERRAN_BARRACKS,
            TERRAN_FACTORY,
            TERRAN_STARPORT
    );

    private static final List<Units> macroArmy = List.of(
            TERRAN_MEDIVAC,
            TERRAN_HELLION,
            TERRAN_MARAUDER,
            TERRAN_MARINE
    );


    DynamicBuildOrder(S2Agent agent, Utils utils, BuildUtils buildUtils) {
        super(agent, utils, buildUtils);
    }

    @Override
    public Units getConstructionOrder(int minerals, int gas) {
        Units constructionOrder;

        constructionOrder = checkSupply();

        if (constructionOrder == null) {
            constructionOrder = checkUnits(minerals, gas);
        }

        if (constructionOrder == null) {
            constructionOrder = checkBases(minerals);
        }

        if (constructionOrder == null) {
            constructionOrder = checkMacro(minerals, gas);
        }

        constructionOrder = checkWorkers(constructionOrder, minerals, gas);

        return constructionOrder;
    }

    private Units checkWorkers(Units constructionOrder, int minerals, int gas) {
        if (constructionOrder == TERRAN_ORBITAL_COMMAND ||
                constructionOrder == TERRAN_PLANETARY_FORTRESS ||
                constructionOrder == TERRAN_SUPPLY_DEPOT ||
                constructionOrder == INVALID) {
            return constructionOrder;
        }

        if (utils.canBuildAdditionalUnit(TERRAN_SCV, minerals, gas) &&
                utils.countFinishedUnitType(TERRAN_SCV) < 90) {
            setConstructionDesire(TERRAN_SCV);
            if (minerals >= 50) {
                return TERRAN_SCV;
            } else {
                return Units.INVALID;
            }
        }
        return constructionOrder;
    }

    private Units checkMacro(int minerals, int gas) {

        double income = utils.mineralRate;
        double expenditure = getCurrentExpenditure();
        double planned = getPlannedExpenditure();

        if (income > expenditure + planned) {
            Optional<Units> building = macroStructures.stream()
                    .filter(buildUtils::canBuildBuilding)
                    .min(getCountOfBuildingComparator());

            if (building.isPresent()) {
                return buildDependency(building.get(), minerals, gas);
            }
        }
        return null;
    }

    private Units buildDependency(Units unit, int minerals, int gas) {
        switch (unit) {
            case TERRAN_BARRACKS:
                return TERRAN_BARRACKS;
            case TERRAN_FACTORY:
                return TERRAN_FACTORY;
            case TERRAN_STARPORT:
                return TERRAN_STARPORT;
            case TERRAN_BARRACKS_REACTOR:
            case TERRAN_BARRACKS_TECHLAB:
                List<UnitInPool> barracks = utils.getFinishedUnits(TERRAN_BARRACKS);
                if (barracks.isEmpty() && utils.canBuildAdditionalUnit(TERRAN_BARRACKS, minerals, gas)) {
                    return TERRAN_BARRACKS;
                } else if (utils.canBuildAdditionalUnit(unit, minerals, gas)) {
                    return unit;
                } else return null;
            case TERRAN_FACTORY_REACTOR:
            case TERRAN_FACTORY_TECHLAB:
                List<UnitInPool> factories = utils.getFinishedUnits(TERRAN_FACTORY);
                if (factories.isEmpty() && utils.canBuildAdditionalUnit(TERRAN_FACTORY, minerals, gas)) {
                    return TERRAN_FACTORY;
                } else if (utils.canBuildAdditionalUnit(unit, minerals, gas)) {
                    return unit;
                } else return null;
            case TERRAN_STARPORT_REACTOR:
            case TERRAN_STARPORT_TECHLAB:
                List<UnitInPool> starports = utils.getFinishedUnits(TERRAN_STARPORT);
                if (starports.isEmpty() && utils.canBuildAdditionalUnit(TERRAN_STARPORT, minerals, gas)) {
                    return TERRAN_STARPORT;
                } else if (utils.canBuildAdditionalUnit(unit, minerals, gas)) {
                    return unit;
                } else return null;
            default:
                return null;
        }

    }


    private Units checkBases(int minerals) {
        int workers = utils.countFinishedUnitType(TERRAN_SCV);
        int bases = utils.getAllBasesIncludingUnderProduction().size();
        int gases = utils.countOfUnitsIncludingUnderConstruction(TERRAN_REFINERY);

        if (utils.getAllMyFinishedBases().size() != 0) {
            if ((workers / bases) > 16) {
                if ((gases / bases) < 1.5 && utils.countOfUnitsBuildingUnit(TERRAN_REFINERY) == 0) {
                    setConstructionDesire(TERRAN_REFINERY);
                    if (minerals >= 75) {
                        return TERRAN_REFINERY;
                    } else {
                        return INVALID;
                    }
                } else {
                    setConstructionDesire(TERRAN_COMMAND_CENTER);
                    if (minerals >= 400) {
                        return TERRAN_COMMAND_CENTER;
                    } else {
                        return Units.INVALID;
                    }
                }
            }
        }
        return null;
    }

    private Units checkUnits(int minerals, int gas) {
        for (Units u : macroArmy) {
            if (utils.canBuildAdditionalUnit(u, minerals, gas)) {
                //    log.info("BBO wants to build :" + u);
                setConstructionDesire(u);
                return u;
            }
        }
        return null;
    }

    private Units checkSupply() {
        if (buildUtils.needSupply(SUPPLY_BUFFER)) {
            setConstructionDesire(TERRAN_SUPPLY_DEPOT);
            return buildUtils.canBuildBuilding(TERRAN_SUPPLY_DEPOT) ? TERRAN_SUPPLY_DEPOT : INVALID;
        }
        return null;
    }

    public Comparator<Units> getCountOfBuildingComparator() {
        return (u1, u2) -> {
            Integer c1 = utils.countFinishedUnitType(u1);
            Integer c2 = utils.countFinishedUnitType(u2);
            return c1.compareTo(c2);
        };
    }

    @Override
    public String getName() {
        return "Dynamic Build Order";
    }

    @Override
    public void debugStatus() {

    }

    private double getCurrentExpenditure() {
        double total = 0d;
        total += utils.getAllMyFinishedBases().size() * Utils.WORKER_COST_PER_MIN;
        total += utils.countFinishedUnitType(Units.TERRAN_BARRACKS) * Utils.MARINE_COST_PER_MIN;
        total += utils.countFinishedUnitType(Units.TERRAN_FACTORY) * Utils.HELLION_COST_PER_MIN;
        total += utils.countFinishedUnitType(Units.TERRAN_STARPORT) * Utils.MEDIVAC_COST_PER_MIN;
        return total;
    }

    private double getPlannedExpenditure() {
        double total = 0;
        total += utils.countOfUnitsBuildingUnit(Units.TERRAN_COMMAND_CENTER) * Utils.WORKER_COST_PER_MIN;
        total += utils.countOfUnitsBuildingUnit(TERRAN_PLANETARY_FORTRESS) * Utils.WORKER_COST_PER_MIN;
        total += utils.countOfUnitsBuildingUnit(TERRAN_ORBITAL_COMMAND) * Utils.WORKER_COST_PER_MIN;
        total += utils.countOfUnitsBuildingUnit(Units.TERRAN_BARRACKS) * Utils.MARINE_COST_PER_MIN;
        total += utils.countOfUnitsBuildingUnit(TERRAN_FACTORY) * Utils.HELLION_COST_PER_MIN;
        total += utils.countOfUnitsBuildingUnit(TERRAN_STARPORT) * Utils.MEDIVAC_COST_PER_MIN;

        return total;
    }
}

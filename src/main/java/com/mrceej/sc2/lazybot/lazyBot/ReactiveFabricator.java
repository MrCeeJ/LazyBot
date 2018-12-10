package com.mrceej.sc2.lazybot.lazyBot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import lombok.extern.log4j.Log4j2;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
public class ReactiveFabricator {
    private static final int SUPPLY_BUFFER = 2;
    private final S2Agent agent;
    private Strategy strategy;
    private Utils utils;
    private BuildUtils buildUtils;

    ReactiveFabricator(S2Agent agent) {
        this.agent = agent;
    }

    void init(Strategy strategy, Utils utils, BuildUtils buildUtils) {
        this.strategy = strategy;
        this.utils = utils;
        this.buildUtils = buildUtils;
    }

    void onUnitCreated(Units unitType) {

    }

    public void run() {
        checkSupply();
        checkMakeUnits();
        checkMakeBuildings();
    }

    private void checkMakeBuildings() {
        int workers = utils.countFinishedUnitType(TERRAN_SCV);
        int bases = utils.getAllBasesIncludingUnderProduction().size();
        int gases = utils.countOfUnitsIncludingUnderConstruction(TERRAN_REFINERY);
        int minerals = agent.observation().getMinerals();

        if (utils.getAllMyFinishedBases().size() != 0) {
            if ((workers / bases) > 16) {
                if ((gases / bases) < 1.5 && utils.countOfUnitsBuildingUnit(TERRAN_REFINERY) == 0) {
                    if (minerals >= 75) {
                        buildUtils.buildUnit(TERRAN_REFINERY);
                    }
                } else {
                    if (minerals >= 400) {
                        buildUtils.buildUnit(TERRAN_COMMAND_CENTER);
                    }
                }
            } else {
                checkPlans();
            }
        }
    }

    private void checkPlans() {
        Units order = checkMacro();
        if (order != null) {
            buildUtils.buildUnit(order);
        }
    }

    private void checkMakeUnits() {
        utils.getBuilderBuildings().stream()
                .filter(utils.isNotBuildingAnything())
                .forEach(this::buildAThing);

    }

    private void buildAThing(UnitInPool unit) {
        Units type = (Units) unit.unit().getType();
        switch (type) {
            case TERRAN_STARPORT:
                buildUtils.buildUnit(TERRAN_MEDIVAC);
                break;
            case TERRAN_FACTORY:
                buildUtils.buildUnit(TERRAN_HELLION);
                break;
            case TERRAN_BARRACKS:
                buildUtils.buildUnit(TERRAN_MARINE);
                break;
            case TERRAN_COMMAND_CENTER:
            case TERRAN_ORBITAL_COMMAND:
            case TERRAN_PLANETARY_FORTRESS:
                buildUtils.buildUnit(TERRAN_SCV);
        }
    }

    private void checkSupply() {
        if (buildUtils.needSupply(SUPPLY_BUFFER)) {
            if (buildUtils.canBuildBuilding(TERRAN_SUPPLY_DEPOT)) {
                buildUtils.buildUnit((TERRAN_SUPPLY_DEPOT));
            }
        }

    }


    private static final List<Units> macroStructures = List.of(
            TERRAN_BARRACKS,
            TERRAN_BARRACKS,
            TERRAN_FACTORY,
            TERRAN_STARPORT
    );

    private Comparator<Units> getCountOfBuildingComparator() {
        return (u1, u2) -> {
            Integer c1 = utils.countFinishedUnitType(u1);
            Integer c2 = utils.countFinishedUnitType(u2);
            return c1.compareTo(c2);
        };
    }

    private Units checkMacro() {
        int minerals = agent.observation().getMinerals();
        int gas = agent.observation().getVespene();
        double income = utils.mineralRate;
        double expenditure = getCurrentExpenditure();
        double planned = getPlannedExpenditure();

        if (income > expenditure + planned) {
            log.info("Income greater than expenditure (" + income + " / " + expenditure+") investigating macro options");

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

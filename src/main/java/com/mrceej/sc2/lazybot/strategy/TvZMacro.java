package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import lombok.extern.log4j.Log4j2;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
public class TvZMacro extends Doctrine {

    private static final int SUPPLY_BUFFER = 2;

    private int macroPosition;

    private static final List<Units> macroStructures = List.of(
            TERRAN_BARRACKS,
            TERRAN_BARRACKS_TECHLAB,
            TERRAN_FACTORY,
            TERRAN_STARPORT
    );

    TvZMacro(S2Agent agent, Utils utils, BuildUtils buildUtils) {
        super(agent, utils, buildUtils);
    }

    @Override
    public void checkSupply() {
        if (buildUtils.needSupply(SUPPLY_BUFFER)) {
            if (buildUtils.canBuildBuilding(TERRAN_SUPPLY_DEPOT)) {
                buildUtils.buildUnit((TERRAN_SUPPLY_DEPOT));
            }
        }

    }

    @Override
    public void checkMakeUnits() {
        buildUtils.getBuilderBuildings().stream()
                .filter(buildUtils.isNotBuildingAnything())
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
            case TERRAN_BARRACKS_TECHLAB:
                buildUtils.buildUnit(TERRAN_MARAUDER);
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
    @Override
    public void checkMakeBuildings() {
        int workers = buildUtils.countFinishedUnitType(TERRAN_SCV);
        int bases = buildUtils.getAllBasesIncludingUnderProduction().size();
        int gases = buildUtils.countOfUnitsIncludingUnderConstruction(TERRAN_REFINERY);
        int minerals = agent.observation().getMinerals();

        if (buildUtils.getAllMyFinishedBases().size() != 0) {
            if ((workers / bases) > 16) {
                if ((gases / bases) < 1.5 && buildUtils.countOfUnitsBuildingUnit(TERRAN_REFINERY) == 0) {
                    if (minerals >= 75) {
                        buildUtils.buildUnit(TERRAN_REFINERY);
                    }
                } else {
                    if (minerals >= 400) {
                        buildUtils.buildUnit(TERRAN_COMMAND_CENTER);
                    }
                }
            } else {
                checkMacro();
            }
        }

    }

    private void checkMacro() {
        int minerals = agent.observation().getMinerals();
        int gas = agent.observation().getVespene();
        double income = utils.mineralRate;
        double expenditure = utils.getCurrentExpenditure();
        double planned = utils.getPlannedExpenditure();

        if (income > expenditure + planned) {
            log.info("Income greater than expenditure (" + income + " / " + expenditure+") investigating macro options");

            macroUp(minerals, gas);
            Optional<Units> building = macroStructures.stream()
                    .filter(buildUtils::canBuildBuilding)
                    .filter(Objects::nonNull)
                    .min(buildUtils.getCountOfBuildingComparator());

            log.info("Option decided :" + building.orElse(INVALID));
            building.ifPresent(units -> buildUtils.buildUnit(buildUtils.getUnitDependency(units, minerals, gas)));
        }
    }

    private void macroUp(int minerals, int gas) {

    }


}

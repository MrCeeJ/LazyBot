package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import com.mrceej.sc2.lazybot.utils.ZergUtils;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
public class ZvTMacro extends Doctrine {

    private static final int SUPPLY_BUFFER = 2;

    private final ZergUtils zergUtils;

    private static final List<Units> macroStructures = List.of(
            TERRAN_BARRACKS,
            TERRAN_BARRACKS_TECHLAB,
            TERRAN_FACTORY,
            TERRAN_STARPORT
    );

    ZvTMacro(S2Agent agent, Utils utils, BuildUtils buildUtils, ZergUtils zergUtils) {
        super(agent, utils, buildUtils);
        this.zergUtils = zergUtils;
    }

    @Override
    public void checkSupply() {
        if (zergUtils.needSupply(SUPPLY_BUFFER)) {
            if (zergUtils.canMorphUnit(ZERG_OVERLORD)) {
                zergUtils.morphUnit(ZERG_OVERLORD);
            }
        }
    }

    @Override
    public void checkMakeUnits() {
        int workers = zergUtils.getDroneCount();
        if (workers < 80) {
            if (zergUtils.canMorphUnit(ZERG_DRONE)) {
                zergUtils.morphUnit(ZERG_DRONE);
            }
        } else {
            if (zergUtils.canMorphUnit(ZERG_ROACH)) {
                zergUtils.morphUnit(ZERG_ROACH);
            } else {
                if (zergUtils.canBuildUnit(ZERG_QUEEN)) {
                    zergUtils.buildUnit(ZERG_QUEEN);
                }
            }
        }
    }

    @Override
    public void checkMakeBuildings() {
        int workers = zergUtils.getDroneCount();
        int bases = zergUtils.getMyHatcheries().size();
        if (bases > 0) {
            if (workers / bases > 14) {
                if (buildUtils.canAffordUnit(ZERG_HATCHERY)) {
                    zergUtils.buildHatchery();
                }
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
            log.info("Income greater than expenditure (" + income + " / " + expenditure + ") investigating macro options");

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

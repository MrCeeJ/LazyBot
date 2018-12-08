package com.mrceej.sc2.lazybot.lazyBot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import com.mrceej.sc2.lazybot.strategy.Doctrine;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
class Fabrication {

    private final S2Agent agent;
    private Strategy strategy;
    private Utils utils;
    private boolean isSavingUp = false;
    private BuildUtils buildUtils;
    private List<Units> buildingRequests;

    Fabrication(S2Agent agent) {
        this.agent = agent;
    }

    void init(Strategy strategy, Utils utils, BuildUtils buildUtils) {
        this.utils = utils;
        this.strategy = strategy;
        this.buildUtils = buildUtils;
        buildingRequests = new ArrayList<>();
    }

    void run() {
        runBuild();
    }

    private void runBuild() {
        Units next = getNextBuildItem();
        if (next != INVALID) {
            buildingRequests.add(next);
        }

        for (Units unit : buildingRequests) {
            if (!buildingInProgress(unit) && !workerAssignedToBuild(unit)) {
                buildUtils.buildUnit(unit);
            }
        }
    }

    private boolean workerAssignedToBuild(Units unit) {
        return utils.countOfUnitsBuildingUnit(unit) > 0;
    }

    private boolean buildingInProgress(Units unit) {
        return utils.countOfUnitsBeingBuilt(unit) > 0;
    }

    void updateBuild(Units unitType) {
        buildingRequests.remove(unitType);

    }

    private Units getNextBuildItem() {
        int minerals = agent.observation().getMinerals();
        int gas = agent.observation().getVespene();
        Units nextConstruction;
        List<Doctrine> strats = strategy.getPriority();
        debugStrats(strats);
        for (Doctrine d : strats) {
            nextConstruction = d.getConstructionOrder(minerals, gas);
            if (nextConstruction != null) {
                if (nextConstruction.equals(Units.INVALID)) {
                    if (!isSavingUp) {
                        log.info("Saving up for next fabrication from : " + d.getName() + " : " + d.getConstructionDesire());
                        isSavingUp = true;
                    }
                    return Units.INVALID;
                } else {
                    isSavingUp = false;
                    return nextConstruction;
                }
            }
        }
        log.info("Nothing to build from doctrines");
        return INVALID;
    }

    private void debugStrats(List<Doctrine> strats) {
        if (agent.observation().getGameLoop() % 100 == 0) {
            for (Doctrine d : strats) {
                d.debugStatus();
            }
        }
    }
}

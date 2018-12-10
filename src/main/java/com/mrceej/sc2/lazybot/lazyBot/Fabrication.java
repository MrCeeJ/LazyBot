package com.mrceej.sc2.lazybot.lazyBot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.UnitType;
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

    private String debugNext;
    private String debugQueue;
    private String debugBuilding;
    private boolean debugNothingToBuild;
    private String debugSaving;

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
        debug(next);
        if (next != INVALID) {
            buildingRequests.add(next);
        }

        for (Units unit : buildingRequests) {
            if (!buildingInProgress(unit) && !workerAssignedToBuild(unit)) {
                buildUtils.buildUnit(unit);
            }
        }
    }

    private void debug(Units nextUnit) {
        String next = nextUnit.toString();
        String building = utils.printUnits(utils.getAllUnitsBeingBuilt());
        String queue = utils.printUnitTypes(buildingRequests);

        if (!next.equals(debugNext) ||
                !building.equals(debugBuilding) ||
                !queue.equals(debugQueue)) {
            debugNext = next;
            debugBuilding = building;
            debugQueue = queue;
            if (next.equals("INVALID")) {
                log.info("Next item - saving up for: " + debugSaving);
            } else {
                log.info("Next item : " + next);
            }
            log.info("current queue : " + queue);
            log.info("currently building : " + building);
        }
    }

    private boolean workerAssignedToBuild(Units unit) {
        return utils.countOfUnitsBuildingUnit(unit) > 0;
    }

    private boolean buildingInProgress(Units unit) {
        return utils.countOfUnitsBeingBuilt(unit) > 0;
    }

    void onUnitCreated(Units unitType) {
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
                        debugSaving = d.getConstructionDesire().toString();
                        log.info("Saving up for next fabrication from : " + d.getName() + " : " + debugSaving);
                        isSavingUp = true;
                    }
                    debugNothingToBuild = false;
                    return Units.INVALID;
                } else {
                    isSavingUp = false;
                    debugNothingToBuild = false;
                    return nextConstruction;
                }
            }
        }
        if (!debugNothingToBuild)
        {
            log.info("Nothing to build from doctrines");
            debugNothingToBuild = true;
        }
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

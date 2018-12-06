package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.strategy.Doctrine;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
class Fabrication {

    private S2Agent agent;
    private Strategy strategy;
    private Utils utils;
    private boolean isSavingUp = false;
    private Units previousConstruction;
    private int previousUnitsBuildingCount;
    private int previousBuildingBuildingCount;
    private int logLock = 0;
    private BuildUtils buildUtils;
    private List<Units> buildingRequests;

    Fabrication(S2Agent agent) {
        this.agent = agent;
    }

    void init(Strategy strategy, Utils utils, BuildUtils buildUtils) {
        this.utils = utils;
        this.strategy = strategy;
        this.buildUtils = buildUtils;
        previousConstruction = null;
        previousUnitsBuildingCount = 0;
        previousBuildingBuildingCount = 0;
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


    private void arunBuild() {
        if (previousConstruction == null) {
            previousConstruction = getNextBuildItem();
        }

        Units nextConstruction;
        if (previousConstruction != INVALID) {
            int currentUnitsCount = utils.countOfUnitsBuildingUnit(previousConstruction);
            int currentBuildingCount = utils.countOfUnitsBeingBuilt(previousConstruction);
            if (currentUnitsCount > previousUnitsBuildingCount) {
                if (currentBuildingCount > previousBuildingBuildingCount) {
                    if (logLock != 4) {
                        log.info("building " + previousConstruction + " is under construction already");
                        logLock = 4;
                    }
                } else {
                    if (logLock != 1) {
                        log.info("ordered the building of " + previousConstruction + " already, waiting for unit to get there");
                        logLock = 1;
                    }
                }
                nextConstruction = getNextBuildItem();
            } else {
                if (logLock != 2) {
                    log.info("Not started building " + previousConstruction + " yet, trying again!");
                    logLock = 2;
                }
                nextConstruction = previousConstruction;
            }
        } else {
            if (logLock != 3) {
                log.info("Was previously saving money, taking a look for something to build.");
                logLock = 3;
            }
            nextConstruction = getNextBuildItem();
        }
        if (nextConstruction != INVALID) {
            buildUtils.buildUnit(nextConstruction);
            previousConstruction = nextConstruction;
            previousUnitsBuildingCount = utils.getFinishedUnits(nextConstruction).size();
            previousBuildingBuildingCount = utils.countOfUnitsBuildingUnit(nextConstruction);
            logLock = 0;
        }
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

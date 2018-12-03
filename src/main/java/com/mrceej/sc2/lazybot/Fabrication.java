package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;
import com.mrceej.sc2.lazybot.strategy.Doctrine;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Optional;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
class Fabrication {

    private S2Agent agent;
    private General general;
    private Strategy strategy;
    private MapUtils mapUtils;
    private Utils utils;
    private boolean isSavingUp = false;

    Fabrication(S2Agent agent) {
        this.agent = agent;
    }

    void init(General general, Strategy strategy, MapUtils mapUtils, Utils utils) {
        this.general = general;
        this.utils = utils;
        this.strategy = strategy;
        this.mapUtils = mapUtils;
    }

    void run() {
        buildNextItem();
    }

    private void buildNextItem() {
        int minerals = agent.observation().getMinerals();
        int gas = agent.observation().getVespene();
        Units nextConstruction;
        List<Doctrine> strats = strategy.getPriority();
        debugStrats(strats);
        for (Doctrine d : strats) {
            nextConstruction = d.getConstructionOrder(minerals, gas);
            if (nextConstruction != null) {
                if (nextConstruction.equals(Units.INVALID)) {
                    if(!isSavingUp){
                        log.info("Saving up for next fabrication from : " + d.getName());
                        isSavingUp = true;
                    }

                    return;
                } else {
                    isSavingUp = false;
                    if (buildUnit(nextConstruction)) {
                        minerals -= utils.getMineralCost(nextConstruction);
                        gas -= utils.getGasCost(nextConstruction);
                    }
                }
            }
        }
    }

    private void debugStrats(List<Doctrine> strats) {
        if (agent.observation().getGameLoop() % 50 == 0) {
            for (Doctrine d : strats) {
                d.debugStatus();
            }
        }
    }

    private boolean buildUnit(Units unit) {
        if (unit.equals(TERRAN_COMMAND_CENTER)) {
            return tryToExpand();
        } else if (unit.equals(TERRAN_REFINERY)) {
            return tryToBuildRefinary();
        } else if (unit.equals(TERRAN_SCV)) {
            return tryToBuildSCV();
        } else if (unit.equals(TERRAN_MARINE)) {
            return tryToBuildMarine();
        } else {
            return tryToBuildBuilding(unit);
        }
    }

    private boolean tryToExpand() {
        if (agent.observation().getMinerals() > 400) {
            log.info("Attempting to build a Command Center");
            if (utils.countOfUnitUnderConstruction(TERRAN_COMMAND_CENTER) == 0) {
                Unit unit = mapUtils.getRandomUnit(TERRAN_SCV);
                agent.actions().unitCommand(unit, Abilities.BUILD_COMMAND_CENTER,
                        mapUtils.getNearestExpansionLocationTo(mapUtils.getStartingBaseLocation().toPoint2d()), false);
            }
            return true;
        }
        log.info("Not enough minerals to build a Command Center");
        return false;
    }

    private boolean tryToBuildRefinary() {
        if (agent.observation().getMinerals() > 75) {
            Optional<Unit> geyser = mapUtils.findNearestVespene(agent.observation().getStartLocation().toPoint2d());
            if (geyser.isPresent()) {
                return tryToBuildBuildingOnUnit(TERRAN_REFINERY, geyser.get());
            }
        }
        log.info("Not enough minerals to build a Refinary");
        return false;
    }


    private boolean tryToBuildMarine() {
        List<Unit> barracks = utils.getFinishedUnits(TERRAN_BARRACKS);
        log.debug("Found " + barracks + " barracks.");
        int count = 0;
        if (agent.observation().getMinerals() >= 50) {
            for (Unit b : barracks) {
                if (b.getOrders().size() == 0) {
                    agent.actions().unitCommand(b, Abilities.TRAIN_MARINE, false);
//                    log.info("Building marine on the " + count + "th attempt.");
                    return true;
                } else {
                    count++;
                }
            }
        }
        log.info("Unable to build a marine after " + count + " attempts.");
        return false;
    }

    private boolean tryToBuildSCV() {
        if (agent.observation().getMinerals() < 50) {
            log.info("Insufficient minerals for a worker!");
            return false;
        }
        List<Unit> commandCenters = utils.getFinishedUnits(TERRAN_COMMAND_CENTER);
        int count = 0;
        for (Unit com : commandCenters) {
            if (com.getOrders().size() == 0) {
                log.info("Telling the cc to build an scv on attempt :" + count);
                agent.actions().unitCommand(com, Abilities.TRAIN_SCV, false);
                return true;
            } else {
                count++;
            }
        }
        log.info("Unable to build a marine after " + count + " attempts.");
        return false;
    }

    private boolean tryToBuildBuilding(Units unitType) {
        Unit unit = mapUtils.getRandomUnit(TERRAN_SCV);
        agent.actions().unitCommand(unit, utils.getAbilityToBuildUnit(unitType),
                unit.getPosition().toPoint2d().add(Point2d.of(mapUtils.getRandomScalar(), mapUtils.getRandomScalar()).mul(15.0f)), false);
        return true;
    }

    private boolean tryToBuildBuildingOnUnit(Units unitType, Unit location) {
        Unit unit = mapUtils.getRandomUnit(TERRAN_SCV);
        agent.actions().unitCommand(unit, utils.getAbilityToBuildUnit(unitType), location, false);
        return true;
    }

}

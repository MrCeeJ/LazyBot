package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Ability;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
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
    private Units previousConstuction;
    private int previousConstuctionCount;

    Fabrication(S2Agent agent) {
        this.agent = agent;
    }

    void init(General general, Strategy strategy, MapUtils mapUtils, Utils utils) {
        this.general = general;
        this.utils = utils;
        this.strategy = strategy;
        this.mapUtils = mapUtils;
        previousConstuction = INVALID;
        previousConstuctionCount = 0;
    }

    void run() {
        runBuild();
    }

    private void runBuild() {
        if (previousConstuction != INVALID){

        }


        Units nextConstruction = getNextBuildItem();
        if (nextConstruction != INVALID){
            buildUnit(nextConstruction);
            previousConstuction = nextConstruction;
            previousConstuctionCount = utils.getFinishedUnits(nextConstruction).size();
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

    private boolean buildUnit(@org.jetbrains.annotations.NotNull Units unit) {
        if (unit.equals(TERRAN_COMMAND_CENTER)) {
            return tryToExpand();
        } else if (unit.equals(TERRAN_REFINERY)) {
            return tryToBuildRefinary();
        } else if (unit.equals(TERRAN_SCV)) {
            return tryToBuildSCV();
        } else if (unit.equals(TERRAN_MARINE)) {
            return tryToBuildMarine();
        } else if (unit.equals(TERRAN_ORBITAL_COMMAND)) {
            return tryToBuildOrbital();
        } else if (unit.equals(TERRAN_FACTORY_REACTOR)) {
            return tryToBuildAddon(unit, Abilities.BUILD_REACTOR);
        } else if (unit.equals(TERRAN_STARPORT_TECHLAB)) {
            return tryToBuildAddon(unit, Abilities.BUILD_TECHLAB);
        } else {
            return tryToBuildBuilding(unit);
        }
    }

    private boolean tryToBuildOrbital() {
        if (agent.observation().getMinerals() > 150) {
            log.info("Attempting to build an Orbital Command Center");
            List<UnitInPool> commandCenters = utils.getFinishedUnits(TERRAN_COMMAND_CENTER);
            if (commandCenters.size() > 0) {
                agent.actions().unitCommand(commandCenters.get(0).unit(), Abilities.MORPH_ORBITAL_COMMAND, false);
                return true;
            }
        }
        log.info("Not enough minerals to build a Orbital Command Center");
        return false;
    }

    private boolean tryToBuildAddon(Units unit, Ability ability) {
        if (agent.observation().getMinerals() < 50) {
            log.info("Not enough minerals to build " + ability);
            return false;
        }
        if (agent.observation().getVespene() < 25 && ability.equals(Abilities.BUILD_TECHLAB)) {
            log.info("Not enough gas to build a Tech lab");
            return false;
        }
        if (agent.observation().getVespene() < 50) {
            log.info("Not enough gas to build a Reactor");
            return false;
        }
        log.info("Attempting to build a " + ability);
        List<UnitInPool> units = utils.getFinishedUnits(unit);
        for (UnitInPool u : units) {
            if (u.unit().getAddOnTag().isEmpty()) {
                agent.actions().unitCommand(units.get(0).unit(), ability, false);
                return true;
            }
        }
        return false;
    }

    private boolean tryToExpand() {
        if (agent.observation().getMinerals() > 400) {
            log.info("Attempting to build a Command Center");
            if (utils.countOfUnitUnderConstruction(TERRAN_COMMAND_CENTER) == 0) {
                UnitInPool unit = mapUtils.getRandomUnit(TERRAN_SCV);
                general.deallocateWorker(unit);
                agent.actions().unitCommand(unit.unit(), Abilities.BUILD_COMMAND_CENTER,
                        mapUtils.getNearestExpansionLocationTo(mapUtils.getStartingBaseLocation().toPoint2d()), false);
            }
            return true;
        }
        log.info("Not enough minerals to build a Command Center");
        return false;
    }

    private boolean tryToBuildRefinary() {
        if (agent.observation().getMinerals() > 75) {
            Optional<UnitInPool> geyser = mapUtils.findNearestVespene(mapUtils.getStartingBase());
            if (geyser.isPresent()) {
                return tryToBuildRefineryOnUnit(geyser.get());
            }
        }
        log.info("Not enough minerals to build a Refinary");
        return false;
    }


    private boolean tryToBuildMarine() {
        List<UnitInPool> barracks = utils.getFinishedUnits(TERRAN_BARRACKS);
        log.debug("Found " + barracks + " barracks.");
        int count = 0;
        if (agent.observation().getMinerals() >= 50) {
            for (UnitInPool b : barracks) {
                if (b.unit().getOrders().size() == 0) {
                    agent.actions().unitCommand(b.unit(), Abilities.TRAIN_MARINE, false);
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
        List<UnitInPool> commandCenters = utils.getFinishedUnits(TERRAN_COMMAND_CENTER);
        int count = 0;
        for (UnitInPool com : commandCenters) {
            if (com.unit().getOrders().size() == 0) {
                log.info("Telling the cc to build an scv on attempt :" + count);
                agent.actions().unitCommand(com.unit(), Abilities.TRAIN_SCV, false);
                return true;
            } else {
                count++;
            }
        }
        log.info("Unable to build a marine after " + count + " attempts.");
        return false;
    }

    private boolean tryToBuildBuilding(Units unitType) {
        UnitInPool unit = mapUtils.getRandomUnit(TERRAN_SCV);
        Ability ability = utils.getAbilityToBuildUnit(unitType);
        if (agent.observation().getMinerals() >= utils.getMineralCost(unitType) && agent.observation().getVespene() >= utils.getGasCost(unitType)) {
            while (true) {
                Point2d location = unit.unit().getPosition().toPoint2d().add(Point2d.of(mapUtils.getRandomScalar(), mapUtils.getRandomScalar()).mul(15.0f));
                if (agent.query().placement(ability, location)) {
                    log.info("Identified location to build :" + ability);
                    agent.actions().unitCommand(unit.unit(), ability,
                            location, false);
                    return true;
                }
            }
        }
        log.info("Not enough resources to build a " + ability);
        return false;
    }

    private boolean tryToBuildRefineryOnUnit(UnitInPool location) {
        UnitInPool unit = mapUtils.getRandomUnit(TERRAN_SCV);
        general.deallocateWorker(unit);
        agent.actions().unitCommand(unit.unit(), utils.getAbilityToBuildUnit(TERRAN_REFINERY), location.unit(), false);
        if (unit.unit().getOrders().size() == 0) {
            log.info("WARNING unit has no orders");
        }
        return true;
    }

}

package com.mrceej.sc2.lazybot;

import SC2APIProtocol.Data;
import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Ability;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;

import java.util.List;
import java.util.Optional;

import com.mrceej.sc2.lazybot.strategy.Doctrine;
import lombok.extern.log4j.Log4j2;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
class Fabrication {

    private S2Agent agent;
    private General general;
    private Strategy strategy;
    private MapUtils mapUtils;
    private Utils utils;

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

//        if (agent.observation().getFoodCap() < 200) {
//            if (agent.observation().getFoodUsed() + utils.getMaxSupplyProduction() >= agent.observation().getFoodCap() + utils.getSupplyInProgress()) {
//                if (minerals < 100) {
//               //     log.warn("Not enough minerals to build a depot");
//                    return;
//                } else {
//                    log.info("Time to build a depot!");
//                    buildUnit(TERRAN_SUPPLY_DEPOT);
//                }
//            }
//        }
        Units nextConstruction;
        List<Doctrine> strats = strategy.getPriority();

        debugStrats(strats);

        for (Doctrine d : strats) {
            nextConstruction = d.getConstructionOrder(minerals, gas);
            //log.info("Building attempt :" + d.getName() + " : " + nextConstruction);
            if (nextConstruction != null) {
                if (nextConstruction.equals(Units.INVALID)) {
                    //log.warn("Need more money to build, lets save up!");
                    return;
                } else {
                    if (buildUnit(nextConstruction)) {
                        log.info("Time to build a :" + nextConstruction);
                        //break;
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
            if (agent.observation().getUnits(Alliance.SELF, utils.doesBuildWith(Abilities.BUILD_COMMAND_CENTER)).isEmpty()) {
                Optional<UnitInPool> unitInPool = mapUtils.getRandomUnit(TERRAN_SCV);
                if (unitInPool.isPresent()) {
                    Unit unit = unitInPool.get().unit();
                    agent.actions().unitCommand(
                            unit,
                            Abilities.BUILD_COMMAND_CENTER,
                            mapUtils.getNearestExpansionLocationTo(mapUtils.getStartingBaseLocation()), false);
                    if (unit.getOrders().get(0).getAbility().equals(Abilities.BUILD_COMMAND_CENTER)) {
                        general.setRole(unit, General.Role.BUILD_SUPPLY_DEPOT);

                    }
                }
            }
            return true;
        }
        log.info("Not enough minerals to build a Command Center");
        return false;
    }

    private boolean tryToBuildMarine() {
        List<UnitInPool> barracks = agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_BARRACKS));
        if (agent.observation().getMinerals() >= 50) {
            for (UnitInPool b : barracks) {
                if (b.getUnit().isPresent()) {
                    if (b.getUnit().get().getOrders().size() == 0) {
                        agent.actions().unitCommand(b.getUnit().get(), Abilities.TRAIN_MARINE, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean tryToBuildSCV() {
        if (agent.observation().getMinerals() < 50) {
            log.info("Telling the cc to build an scv!");
            return false;
        }
        List<UnitInPool> commandCenters = agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(TERRAN_COMMAND_CENTER));
        for (UnitInPool cc : commandCenters) {
            if (cc.getUnit().isPresent()) {
                Unit com = cc.getUnit().get();
                if (com.getOrders().size() == 0) {
                    log.info("Telling the cc to build an scv!");
                    agent.actions().unitCommand(com, Abilities.TRAIN_SCV, false);
                    return true;
                }
            }
        }
        return true;
    }

    private boolean tryToBuildBuilding(UnitType unitType) {
//        if (!agent.observation().getUnits(Alliance.SELF, buildingUtils.doesBuildWith(buildingUtils.getAbilityTypeForStructure(unitType))).isEmpty()) {
//            return false;
//        }
        Optional<UnitInPool> unitInPool = mapUtils.getRandomUnit(TERRAN_SCV);
        if (unitInPool.isPresent()) {
            Unit unit = unitInPool.get().unit();
            agent.actions().unitCommand(
                    unit,
                    utils.getAbilityTypeForStructure(unitType),
                    unit.getPosition().toPoint2d().add(Point2d.of(mapUtils.getRandomScalar(), mapUtils.getRandomScalar()).mul(15.0f)),
                    false);
            return true;
        }
        return false;
    }
}

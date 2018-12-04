package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.observation.ui.ControlGroup;
import com.github.ocraft.s2client.protocol.observation.ui.ObservationUi;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Unit;
import com.mrceej.sc2.lazybot.Combat.Squad;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Slf4j
class General {
    private S2Agent agent;
    private Utils utils;
    private MapUtils mapUtils;

    private ArrayList<Squad> squads;
    private Map<UnitInPool, UnitInPool> workerToBaseMap;

    private Squad currentSquad;

    private boolean firstCommandCenter = true;

    General(S2Agent agent) {
        this.agent = agent;
        this.squads = new ArrayList<>();
        this.workerToBaseMap = new HashMap<>();
    }

    void init(Utils utils, MapUtils mapUtils) {
        this.utils = utils;
        this.mapUtils = mapUtils;
        this.currentSquad = new Squad(agent, utils, mapUtils);
        squads.add(currentSquad);
    }

    void onUnitCreated(UnitInPool unitInPool) {
        log.info("Created unit :" + unitInPool.getTag() + " :" + unitInPool.unit().getType());
        Units unitType = (Units) unitInPool.unit().getType();
        switch (unitType) {
            case TERRAN_COMMAND_CENTER:
                if (firstCommandCenter) {
                    mapUtils.setStartingBase(unitInPool);
                    this.firstCommandCenter = false;
                }
                addToControlGroup(5, unitType);
                break;
            case TERRAN_SCV:
                addToControlGroup(0, unitType);
                onSCVCreated(unitInPool);
                break;
            case TERRAN_BARRACKS:
                addToControlGroup(6, unitType);
                break;
            case TERRAN_MARINE:
                addToControlGroup(1, unitType);
                onSoldierCreated(unitInPool, TERRAN_MARINE);
                break;
            case TERRAN_MARAUDER:
                addToControlGroup(1, unitType);
                onSoldierCreated(unitInPool, TERRAN_MARAUDER);
                break;
            default:
                break;
        }
    }

    void onBuildingConstructionComplete(UnitInPool unit) {
        Units unitType = (Units) unit.unit().getType();
        switch (unitType) {
            case TERRAN_COMMAND_CENTER:
                addToControlGroup(5, unitType);
                rebalanceWorkers();
                break;
            case TERRAN_REFINERY:
                allocateWorkersToGas(unit, 2);
                rebalanceWorkers();
                break;
            default:
                break;
        }
    }

    private void addToControlGroup(int group, Units unit) {
        // TODO
//        Ui.ActionControlGroup gca = SC2APIProtocol.Ui.ActionControlGroup.newBuilder().setControlGroupIndex(group).setAction(Ui.ActionControlGroup.ControlGroupAction.Append && gca.).build();
//        Spatial.FeatureLayers.getDefaultInstance().
//            Set<Ui.ControlGroup> groups = agent.observation().getRawObservation().getUi().get().getControlGroups();
//        Ui.ActionControlGroup.ControlGroupAction cgpa = Ui.ActionControlGroup.ControlGroupAction.forNumber(group);
        // cgpa.
        log.info("Control groups :");
        Optional<ObservationUi> obs = agent.observation().getRawObservation().getUi();
        if (obs.isPresent()) {
            Set<ControlGroup> controlGroups = obs.get().getControlGroups();
            for (ControlGroup g : controlGroups) {
                log.info("Group :" + g.toString());
                log.info("Index:" + g.getIndex());
                log.info("Leader :" + g.getLeaderUnitType());
                log.info("Count :" + g.getCount());
            }
        }
//        agent.observation().getRawObservation().getUi().get().getControlGroups().stream()
//                .filter(group -> isPresent())
//                .map()


//        getGroupsList().stream().map(ControlGroup::from)
//                .collect(collectingAndThen(toSet(), Collections::unmodifiableSet));
//        Set<ControlGroup> groups_ =         .collect(collectingAndThen(toSet(), Collections::unmodifiableSet));
//                agent.observation().getRawObservation().getUi().get().getControlGroups();
//        for (ControlGroup gp : groups_) {
//
//        }
//            agent.actionsFeatureLayer().unitCommand(Abilities.Other.)
    }

    void onUnitIdle(UnitInPool unitInPool) {
        log.info("Idle unit :" + unitInPool.getTag() + " :" + unitInPool.unit().getType());
        Unit unit = unitInPool.unit();
        Units unitType = (Units) unit.getType();
        switch (unitType) {
            case TERRAN_SCV:
                handleIdleSCV(unitInPool);
                break;
            case TERRAN_COMMAND_CENTER:
                rebalanceWorkers();
                break;
            case TERRAN_SUPPLY_DEPOT:
                if (unit.getBurrowed().isPresent()) {
                    if (!unit.getBurrowed().get()) {
                        agent.actions().unitCommand(unit, Abilities.MORPH_SUPPLY_DEPOT_LOWER, false);
                    }
                }
            default:
                break;
        }
    }

    private void allocateWorkersToGas(UnitInPool refinery, int number) {
        List<UnitInPool> bases = utils.getFinishedUnits(TERRAN_COMMAND_CENTER);
        bases.sort(mapUtils.getLinearDistanceComparatorForUnit(refinery.unit().getPosition().toPoint2d()));
        int found = 0;
        for (UnitInPool base : bases) {
            int workersAvailble = countAssignedWorkers(base);
            if (workersAvailble >= number - found) {
                reassignWorkers(number, base, refinery);
                break;
            } else {
                reassignWorkers(workersAvailble, base, refinery);
                found += workersAvailble;
                if (found >= number)
                    break;
            }
        }
    }
     public void deallocateWorker(UnitInPool scv) {
        workerToBaseMap.remove(scv);
     }
    private void rebalanceWorkers() {
        List<UnitInPool> bases = utils.getFinishedUnits(TERRAN_COMMAND_CENTER);
        int workers = agent.observation().getFoodWorkers();
        int average = workers / bases.size();
        List<UnitInPool> basesOver = new ArrayList<>();
        List<UnitInPool> basesUnder = new ArrayList<>();
        for (UnitInPool base : bases) {
            int assignedWorkers = countAssignedWorkers(base);
            if (assignedWorkers > average) {
                log.info("Base : " + base.getTag().toString() + " Assigned too many workers :" + assignedWorkers + " - average :" + average);
                basesOver.add(base);
            } else if (assignedWorkers < average) {
                basesUnder.add(base);
                log.info("Base : " + base.getTag().toString() + "Assigned too few workers :" + assignedWorkers + " - average :" + average);
            } else {
                log.info("Base : " + base.getTag().toString() + "Assigned just enough workers :" + assignedWorkers + " - average :" + average);
            }

            if (basesOver.size() > 0 && basesUnder.size() > 0) {
                int difference = countAssignedWorkers(basesOver.get(0)) - countAssignedWorkers(basesUnder.get(0));
                reassignWorkers(difference / 2, basesOver.get(0), basesUnder.get(0));
                rebalanceWorkers();
            }
        }
    }

    private int countAssignedWorkers(UnitInPool unit) {
        return getWorkersAssignedToUnit(unit).size();
    }

    private ArrayList<UnitInPool> getWorkersAssignedToUnit(UnitInPool base) {
        ArrayList<UnitInPool> workers = new ArrayList<>();
        for (UnitInPool w : workerToBaseMap.keySet()) {
            if (workerToBaseMap.get(w).getTag().equals(base.getTag())) {
                workers.add(w);
            }
        }
        return workers;
    }

    private void reassignWorkers(int number, UnitInPool from, UnitInPool to) {
        ArrayList<UnitInPool> workers = getWorkersAssignedToUnit(from);
        Point2d location = to.unit().getPosition().toPoint2d();
        for (int i = 0; i < number; i++) {
            UnitInPool scv = workers.get(i);
            log.info("Reassigning worker :" + scv.getTag().toString() + " from " + from.getTag().toString() + " to " + to.getTag().toString());
            workerToBaseMap.put(scv, to);
            if (to.unit().getType().equals(TERRAN_COMMAND_CENTER)) {
                rebaseSCVToCommandCenter(location, scv);
            } else {
                rebaseSCVToRefinery(to, scv);
            }
        }
    }

    private void handleIdleSCV(UnitInPool unitInPool) {
        if (workerToBaseMap.containsKey(unitInPool)) {
            log.info("Found idle scv, sending him back to work at ");
            UnitInPool destination = workerToBaseMap.get(unitInPool);
            if (destination.unit().getType().equals(TERRAN_COMMAND_CENTER)) {
                log.info("Found idle scv: " + unitInPool.unit().getTag() + ", sending him back to work at cc");
                rebaseSCVToCommandCenter(destination.unit().getPosition().toPoint2d(), unitInPool);
            } else {
                log.info("Found idle scv: " + unitInPool.getTag() + ", sending him back to work at refinery");
                rebaseSCVToRefinery(destination, unitInPool);
            }
        } else {
            log.info("Found unassigned scv: " + unitInPool.unit().getTag() + ", sending him back to work!");
            onSCVCreated(unitInPool);
        }
    }

    private void rebaseSCVToRefinery(UnitInPool location, UnitInPool scv) {
        agent.actions().unitCommand(scv.unit(), Abilities.SMART, location.unit(), false);
    }

    private void rebaseSCVToCommandCenter(Point2d location, UnitInPool scv) {
        mapUtils.findNearestMineralPatch(location).ifPresent(mineralPatch ->
                agent.actions().unitCommand(scv.unit(), Abilities.SMART, mineralPatch, false));
    }

    private void onSoldierCreated(UnitInPool unit, Units type) {
        if (currentSquad.getOrders().equals(Squad.Orders.DEFEND)) {
            currentSquad.addUnitAndGiveOrders(unit);
        } else {
            Squad squad = new Squad(agent, utils, mapUtils);
            squad.addUnitAndGiveOrders(unit);
            squads.add(squad);
            currentSquad = squad;
        }

    }

    private void onSCVCreated(UnitInPool unitInPool) {
        if (workerToBaseMap.containsKey(unitInPool)) {
            UnitInPool target = workerToBaseMap.get(unitInPool);
            log.info("Detected old worker re-creation");
            log.info("Worker :" + unitInPool.unit().getTag() + " was assigned to " + target.unit().getType() + " number " + target.getTag());
        } else {
            List<UnitInPool> commandCenters = utils.getFinishedUnits(TERRAN_COMMAND_CENTER);
            UnitInPool destination = commandCenters.stream()
                    .filter(c -> c.unit().getAssignedHarvesters().isPresent())
                    .min(Comparator.comparing(c -> c.unit().getAssignedHarvesters().get()))
                    .orElse(null);

            if (destination == null) {
                return;
            }
            workerToBaseMap.put(unitInPool, destination);
            Point2d location = destination.unit().getPosition().toPoint2d();
            mapUtils.findNearestMineralPatch(location).ifPresent(mineralPath ->
                    agent.actions().unitCommand(unitInPool.unit(), Abilities.SMART, mineralPath, false));
        }
    }
}


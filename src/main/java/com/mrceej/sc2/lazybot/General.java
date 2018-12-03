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
    private Map<Unit, Unit> workerToBaseMap;

    private Squad currentSquad;

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
        Unit unit = unitInPool.unit();
        Units unitType = (Units) unit.getType();
        switch (unitType) {
            case TERRAN_COMMAND_CENTER:
                addToControlGroup(5, unitType);
                break;
            case TERRAN_SCV:
                addToControlGroup(0, unitType);
                onSCVCreated(unit);
                break;
            case TERRAN_BARRACKS:
                addToControlGroup(6, unitType);
                break;
            case TERRAN_MARINE:
                addToControlGroup(1, unitType);
                onSoldierCreated(unit, TERRAN_MARINE);
                break;
            case TERRAN_MARAUDER:
                addToControlGroup(1, unitType);
                onSoldierCreated(unit, TERRAN_MARAUDER);
                break;
            default:
                break;
        }
    }

    void onBuildingConstructionComplete(UnitInPool unitInPool) {
        Unit unit = unitInPool.unit();
        Units unitType = (Units) unit.getType();
        switch (unitType) {
            case TERRAN_COMMAND_CENTER:
                addToControlGroup(5, unitType);
                rebalanceWorkers();
                break;
            case TERRAN_REFINERY:
                allocateWorkersToGas(unit, 3);
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
                handleIdleSCV(unit);
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

    private void allocateWorkersToGas(Unit refinery, int number) {
        List<Unit> bases = utils.getFinishedUnits(TERRAN_COMMAND_CENTER);
        bases.sort(mapUtils.getLinearDistanceComparatorForUnit(refinery.getPosition().toPoint2d()));
        int found = 0;
        for (Unit base : bases) {
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


    private void rebalanceWorkers() {
        List<Unit> bases = utils.getFinishedUnits(TERRAN_COMMAND_CENTER);
        int workers = agent.observation().getFoodWorkers();
        int average = workers / bases.size();
        List<Unit> basesOver = new ArrayList<>();
        List<Unit> basesUnder = new ArrayList<>();
        for (Unit base : bases) {
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

    private int countAssignedWorkers(Unit unit) {
        return getWorkersAssignedToUnit(unit).size();
    }

    private ArrayList<Unit> getWorkersAssignedToUnit(Unit base) {
        ArrayList<Unit> workers = new ArrayList<>();
        for (Unit w : workerToBaseMap.keySet()) {
            if (workerToBaseMap.get(w).getTag().equals(base.getTag())) {
                workers.add(w);
            }
        }
        return workers;
    }

    private void reassignWorkers(int number, Unit from, Unit to) {
        ArrayList<Unit> workers = getWorkersAssignedToUnit(from);
        Point2d location = to.getPosition().toPoint2d();
        for (int i = 0; i < number; i++) {
            Unit scv = workers.get(i);
            log.info("Reassigning worker :" + scv.getTag().toString() + " from " + from.getTag().toString() + " to " + to.getTag().toString());
            workerToBaseMap.put(scv, to);
            if (to.getType().equals(TERRAN_COMMAND_CENTER)) {
                rebaseSCVToCommandCenter(location, scv);
            } else {
                rebaseSCVToRefinary(to, scv);
            }
        }
    }

    private void handleIdleSCV(Unit scv) {
        if (workerToBaseMap.containsKey(scv)) {
            log.info("Found idle scv, sending him back to work at ");
            Unit destination = workerToBaseMap.get(scv);
            if (destination.getType().equals(TERRAN_COMMAND_CENTER)) {
                log.info("Found idle scv: " + scv.getTag() + ", sending him back to work at cc");
                rebaseSCVToCommandCenter(destination.getPosition().toPoint2d(), scv);
            } else {
                log.info("Found idle scv: " + scv.getTag() + ", sending him back to work at refinery");
                rebaseSCVToRefinary(destination, scv);
            }
        } else {
            log.info("Found unassigned scv: "+scv.getTag()+", sending him back to work!");
            onSCVCreated(scv);
        }
    }

    private void rebaseSCVToRefinary(Unit location, Unit scv) {
        agent.actions().unitCommand(scv, Abilities.SMART, location, false);
    }

    private void rebaseSCVToCommandCenter(Point2d location, Unit scv) {
        mapUtils.findNearestMineralPatch(location).ifPresent(mineralPath ->
                agent.actions().unitCommand(scv, Abilities.SMART, mineralPath, false));
    }

    private void onSoldierCreated(Unit unit, Units type) {
        if (currentSquad.getOrders().equals(Squad.Orders.DEFEND)) {
            currentSquad.addUnitAndGiveOrders(unit);
        } else {
            Squad squad = new Squad(agent, utils, mapUtils);
            squad.addUnitAndGiveOrders(unit);
            squads.add(squad);
            currentSquad = squad;
        }

    }

    private void onSCVCreated(Unit scv) {
        List<Unit> commandCenters = utils.getFinishedUnits(TERRAN_COMMAND_CENTER);
        Unit destination = commandCenters.stream()
                .filter(c -> c.getAssignedHarvesters().isPresent())
                .min(Comparator.comparing(c -> c.getAssignedHarvesters().get()))
                .orElse(null);

        if (destination == null) {
            return;
        }
        workerToBaseMap.put(scv, destination);
        Point2d location = destination.getPosition().toPoint2d();
        mapUtils.findNearestMineralPatch(location).ifPresent(mineralPath ->
                agent.actions().unitCommand(scv, Abilities.SMART, mineralPath, false));
    }
}

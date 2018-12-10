package com.mrceej.sc2.lazybot.Combat;

import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.retBot.RetBot;
import com.mrceej.sc2.lazybot.state.State;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.MapUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import com.mrceej.sc2.lazybot.utils.ZergUtils;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class Overmind {


    private List<State> unitStates;
    private Utils utils;
    private MapUtils mapUtils;
    private BuildUtils buildUtils;
    private ZergUtils zergUtils;
    private RetBot agent;

    public Overmind(RetBot agent) {

        this.agent = agent;
    }

    public void init(List<State> unitStates, Utils utils, MapUtils mapUtils, BuildUtils buildUtils, ZergUtils zergUtils) {
        this.unitStates = unitStates;
        this.utils = utils;
        this.mapUtils = mapUtils;
        this.buildUtils = buildUtils;
        this.zergUtils = zergUtils;
    }

    public void onUnitCreated(UnitInPool unit) {

    }

    public void onBuildingConstructionComplete(UnitInPool unit) {
        Units unitType = (Units) unit.unit().getType();
        switch (unitType) {
            case ZERG_HATCHERY:
            case ZERG_LAIR:
            case ZERG_HIVE:
                rebalanceWorkers();
            break;
        }
    }

    private void rebalanceWorkers() {
        List<UnitInPool> bases = zergUtils.getMyFinshedHatcheries();
        if (bases.size() < 2){
            return;
        }
        int workers = agent.observation().getFoodWorkers();
        int average = workers / bases.size();
        List<UnitInPool> basesOver = new ArrayList<>();
        List<UnitInPool> basesUnder = new ArrayList<>();
        for (UnitInPool base : bases) {
            int assignedWorkers = base.unit().getAssignedHarvesters().orElse(0);
            if (assignedWorkers > average) {
                log.info("Base : " + base.getTag().toString() + " Assigned too many workers :" + assignedWorkers + " - average :" + average);
                basesOver.add(base);
            } else if (assignedWorkers < average) {
                basesUnder.add(base);
                log.info("Base : " + base.getTag().toString() + " Assigned too few workers :" + assignedWorkers + " - average :" + average);
            } else {
                log.info("Base : " + base.getTag().toString() + " Assigned just enough workers :" + assignedWorkers + " - average :" + average);
            }

            if (basesOver.size() > 0 && basesUnder.size() > 0) {
                int difference = basesOver.get(0).unit().getAssignedHarvesters().orElse(0) - basesUnder.get(0).unit().getAssignedHarvesters().orElse(0);
                reassignWorkers(difference / 2, basesOver.get(0), basesUnder.get(0));
            }
        }
    }

    private void reassignWorkers(int i, UnitInPool from, UnitInPool to) {
        List<UnitInPool> workers = getNWorkersFromBase(i,from);
        for(UnitInPool u : workers) {
            mapUtils.findNearestMineralPatch(from.unit().getPosition().toPoint2d()).ifPresent(mineralPatch ->
                    agent.actions().unitCommand(u.unit(), Abilities.SMART, mineralPatch, false));
        }
    }

    private List<UnitInPool> getNWorkersFromBase(int i, UnitInPool from) {
        List<UnitInPool> drones = zergUtils.getAllUnitsOfType(Units.ZERG_DRONE);
        drones.sort(mapUtils.getLinearDistanceComparatorForUnit(from.unit().getPosition().toPoint2d()));
        return drones.subList(0,i);

    }

    public void onUnitIdle(UnitInPool unitInPool) {

    }
}

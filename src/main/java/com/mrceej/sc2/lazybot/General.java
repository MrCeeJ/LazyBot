package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Unit;
import com.github.ocraft.s2client.protocol.unit.UnitOrder;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_MARAUDER;
import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_MARINE;

@Slf4j
class General {
    private S2Agent agent;
    private Utils utils;
    private MapUtils mapUtils;

    General(S2Agent agent) {
        this.agent = agent;

    }

    void init(Utils utils, MapUtils mapUtils) {
        this.utils = utils;
        this.mapUtils = mapUtils;
    }

    enum Role {
        NONE, GET_MINERALS, GET_GAS, BUILD_SUPPLY_DEPOT
    }

    private Map<Long, Role> assignments = new HashMap<>();

    void setRole(Unit unit, Role role) {
        assignments.put(unit.getTag().getValue(), role);
    }

    private Role getRole(Unit unit) {
        return assignments.getOrDefault(unit.getTag().getValue(), Role.NONE);
    }

    void onUnitCreated(UnitInPool unitInPool) {
        log.info("Created unit :" + unitInPool.getTag() + " :" + unitInPool.unit().getType());
        Unit unit = unitInPool.unit();
        switch ((Units) unit.getType()) {
            case TERRAN_COMMAND_CENTER:
                rebalenceWorkers();
                break;
            case TERRAN_REFINERY:
                //assignXWorkersToGas(2, unit);
                break;
            case TERRAN_SCV:
                onSCVCreated(unit);
                break;
            case TERRAN_BARRACKS:
                break;
            case TERRAN_MARINE:
                onSoldierCreated(unit, TERRAN_MARINE);
                break;
            case TERRAN_MARAUDER:
                onSoldierCreated(unit, TERRAN_MARAUDER);
                break;
            default:
                break;
        }
    }

    void onUnitIdle(UnitInPool unitInPool) {
        onUnitCreated(unitInPool);
    }

    private void rebalenceWorkers() {
        //TODO
        log.debug("HAELP! need to fix workers :)");
    }

    private void onSoldierCreated(Unit unit, Units type) {
        if (utils.countUnitType(type) < 15) {
            agent.actions().unitCommand(unit, Abilities.MOVE, utils.getCCLocation(), false);
        } else {
            mapUtils.findEnemyPosition().ifPresent(point2d ->
                    agent.actions().unitCommand(unit, Abilities.ATTACK_ATTACK, point2d, false));
        }
    }

    private void onSCVCreated(Unit scv) {
        List<UnitInPool> cc = utils.getCommandCenters();
        int workers = 100;
        Unit destination = null;
        for (UnitInPool com : cc) {
            if (com.getUnit().isPresent()) {
                Unit commandCenter = com.getUnit().get();
                if (commandCenter.getBuildProgress() == 1f) {
                    if (commandCenter.getAssignedHarvesters().isPresent()) {
                        if (commandCenter.getAssignedHarvesters().get() < workers) {
                            destination = commandCenter;
                            workers = commandCenter.getAssignedHarvesters().get();
                        }
                    }
                }
            }
        }
        if (destination == null) {
            return;
        }
        // cc.sort(Comparator.comparing(o -> o.getUnit().get().getAssignedHarvesters().get()));
        Point2d location = destination.getPosition().toPoint2d();
        mapUtils.findNearestMineralPatch(location).ifPresent(mineralPath ->
                agent.actions().unitCommand(scv, Abilities.SMART, mineralPath, false));
        setRole(scv, Role.GET_MINERALS);
    }
}

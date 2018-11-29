import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.unit.Unit;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_MARAUDER;
import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_MARINE;

@Slf4j
class Military {
    private S2Agent agent;
    private Utils utils;
    private BuildingUtils buildingUtils;

    Military(S2Agent agent) {
        this.agent = agent;

    }

    void init(Utils utils, BuildingUtils buildingUtils) {
        this.utils = utils;
        this.buildingUtils = buildingUtils;
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

    void onUnitCreated(UnitInPool unit) {
       log.debug("Created unit :" + unit.getTag() + " :" + unit.unit().getType());
        unit.getUnit().ifPresent(unit1 -> setRole(unit1, Role.GET_MINERALS));
    }

    void onUnitIdle(UnitInPool unitInPool) {
        Unit unit = unitInPool.unit();
        switch ((Units) unit.getType()) {
            case TERRAN_COMMAND_CENTER:
                agent.actions().unitCommand(unit, Abilities.TRAIN_SCV, false);
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

    private void onSoldierCreated(Unit unit, Units type) {
        if (utils.countUnitType(type) < 15) {
            agent.actions().unitCommand(unit, Abilities.MOVE, buildingUtils.getCCLocation(), false);
        } else {
            utils.findEnemyPosition().ifPresent(point2d ->
                    agent.actions().unitCommand(unit, Abilities.ATTACK_ATTACK, point2d, false));
        }
    }

    private void onSCVCreated(Unit scv) {
        utils.findNearestMineralPatch(scv.getPosition().toPoint2d()).ifPresent(mineralPath ->
                agent.actions().unitCommand(scv, Abilities.SMART, mineralPath, false));
        setRole(scv, Role.GET_MINERALS);
    }
}

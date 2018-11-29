import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Ability;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;
import java.util.Map;
import java.util.function.Predicate;
import static java.util.Map.entry;

class BuildingUtils {

    private final S2Agent agent;
    private static final Map<UnitType, Ability> UnitTypeToAbilityMap = Map.ofEntries(
            entry(Units.TERRAN_COMMAND_CENTER, Abilities.BUILD_COMMAND_CENTER),
            entry(Units.TERRAN_SUPPLY_DEPOT, Abilities.BUILD_SUPPLY_DEPOT),
            entry(Units.TERRAN_REFINERY, Abilities.BUILD_REFINERY),
            entry(Units.TERRAN_BARRACKS, Abilities.BUILD_BARRACKS),
            entry(Units.TERRAN_ENGINEERING_BAY, Abilities.BUILD_ENGINEERING_BAY),
            entry(Units.TERRAN_MISSILE_TURRET, Abilities.BUILD_MISSILE_TURRET),
            entry(Units.TERRAN_BUNKER, Abilities.BUILD_BUNKER),
            entry(Units.TERRAN_SENSOR_TOWER, Abilities.BUILD_SENSOR_TOWER),
            entry(Units.TERRAN_GHOST_ACADEMY, Abilities.BUILD_GHOST_ACADEMY),
            entry(Units.TERRAN_FACTORY, Abilities.BUILD_FACTORY),
            entry(Units.TERRAN_STARPORT, Abilities.BUILD_STARPORT),
            entry(Units.TERRAN_ARMORY, Abilities.BUILD_ARMORY),
            entry(Units.TERRAN_FUSION_CORE, Abilities.BUILD_FUSION_CORE)
    );

    BuildingUtils(S2Agent agent)
    {
        this.agent = agent;

    }

    Ability getAbilityTypeForStructure(UnitType unitType) {
        return UnitTypeToAbilityMap.get(unitType);
    }

    Point2d getCCLocation() {
        Unit cc = agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_COMMAND_CENTER)).get(0).unit();
        return cc.getPosition().toPoint2d();
    }

    int getNumberOfBasesIncludingConstruction() {
        return getNumberOfBases() + getBuildingUnderConstruction(Abilities.BUILD_COMMAND_CENTER);
    }

    int getNumberOfBases() {
        return agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_COMMAND_CENTER)).size();
    }
    int getBuildingUnderConstruction(Abilities buildCommandCenter) {
        return agent.observation().getUnits(Alliance.SELF, doesBuildWith(buildCommandCenter)).size();
    }

    Predicate<UnitInPool> doesBuildWith(Ability abilityTypeForStructure) {
        return unitInPool -> unitInPool.unit()
                .getOrders()
                .stream()
                .anyMatch(unitOrder -> abilityTypeForStructure.equals(unitOrder.getAbility()));
    }
}

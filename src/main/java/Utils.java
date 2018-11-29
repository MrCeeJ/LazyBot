import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Ability;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.game.raw.StartRaw;
import com.github.ocraft.s2client.protocol.response.ResponseGameInfo;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

class Utils {

    private S2Agent agent;
    float mineralRate;
    float vespeneRate;

    static final double MARINE_COST_PER_MIN = 166.6666666666667;
    static final double WORKER_COST_PER_MIN = 250;

    Utils(S2Agent agent) {
        this.agent = agent;
    }

    Optional<Unit> findNearestMineralPatch(Point2d start) {
        List<UnitInPool> units = agent.observation().getUnits(Alliance.NEUTRAL);
        double distance = Double.MAX_VALUE;
        Unit target = null;
        for (UnitInPool unitInPool : units) {
            Unit unit = unitInPool.unit();
            if (unit.getType().equals(Units.NEUTRAL_MINERAL_FIELD)) {
                double d = unit.getPosition().toPoint2d().distance(start);
                if (d < distance) {
                    distance = d;
                    target = unit;
                }
            }
        }
        return Optional.ofNullable(target);
    }

    int countUnitType(Units unitType) {
        return agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(unitType)).size();
    }


    // Tries to find a random location that can be pathed to on the map.
    // Returns Point2d if a new, random location has been found that is pathable by the unit.
    Optional<Point2d> findEnemyPosition() {
        ResponseGameInfo gameInfo = agent.observation().getGameInfo();

        Optional<StartRaw> startRaw = gameInfo.getStartRaw();
        if (startRaw.isPresent()) {
            Set<Point2d> startLocations = new HashSet<>(startRaw.get().getStartLocations());
            startLocations.remove(agent.observation().getStartLocation().toPoint2d());
            if (startLocations.isEmpty()) return Optional.empty();
            return Optional.of(new ArrayList<>(startLocations)
                    .get(ThreadLocalRandom.current().nextInt(startLocations.size())));
        } else {
            return Optional.empty();
        }
    }

    Predicate<UnitInPool> doesBuildWith(Ability abilityTypeForStructure) {
        return unitInPool -> unitInPool.unit()
                .getOrders()
                .stream()
                .anyMatch(unitOrder -> abilityTypeForStructure.equals(unitOrder.getAbility()));
    }

    Optional<UnitInPool> getRandomUnit(UnitType unitType) {
        List<UnitInPool> units = agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(unitType));
        return units.isEmpty()
                ? Optional.empty()
                : Optional.of(units.get(ThreadLocalRandom.current().nextInt(units.size())));
    }

    float getRandomScalar() {
        return ThreadLocalRandom.current().nextFloat() * 2 - 1;
    }

    void updateIncomes() {
        mineralRate = agent.observation().getScore().getDetails().getCollectionRateMinerals();
        vespeneRate = agent.observation().getScore().getDetails().getCollectionRateVespene();
    }

    int getMaxSupplyProduction() {
        int total = 0;
        total += agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_COMMAND_CENTER)).size();
        total += agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_BARRACKS)).size() * 2;
        return total;
    }

}

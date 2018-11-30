package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.game.raw.StartRaw;
import com.github.ocraft.s2client.protocol.response.ResponseGameInfo;
import com.github.ocraft.s2client.protocol.spatial.Point;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class MapUtils {

    private final S2Agent agent;
    private Point2d STARTING_BASE_LOCATION;
   // private List<Point> base_locations;

    MapUtils(S2Agent agent)
    {
        this.agent = agent;
    }

    void init() {
      //  base_locations = agent.query().calculateExpansionLocations(agent.observation());
        STARTING_BASE_LOCATION = agent.observation().getStartLocation().toPoint2d();

    }

    Point2d getStartingBaseLocation() {
        return STARTING_BASE_LOCATION;
    }

    Point2d getNearestExpansionLocationTo(Point2d source){
        List<Point> base_locations = agent.query().calculateExpansionLocations(agent.observation());
        base_locations.sort(getDistanceComparator(source));
        return base_locations.get(0).toPoint2d();
    }

    private Comparator<Point> getDistanceComparator(Point2d source) {
        return (p1, p2) -> {
            Double d1 = p1.toPoint2d().distance(source);
            Double d2 = p2.toPoint2d().distance(source);
            return d1.compareTo(d2);
        };
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

    Optional<UnitInPool> getRandomUnit(UnitType unitType) {
        List<UnitInPool> units = agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(unitType));
        return units.isEmpty()
                ? Optional.empty()
                : Optional.of(units.get(ThreadLocalRandom.current().nextInt(units.size())));
    }

    float getRandomScalar() {
        return ThreadLocalRandom.current().nextFloat() * 2 - 1;
    }

}

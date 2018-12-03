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
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_COMMAND_CENTER;

@Log4j2
public class MapUtils {

    private final S2Agent agent;
    private Point STARTING_BASE_LOCATION;
    private Utils utils;
    // private List<Point> base_locations;

    MapUtils(S2Agent agent) {
        this.agent = agent;
    }

    void init(Utils utils) {
        this.utils = utils;
        //  base_locations = agent.query().calculateExpansionLocations(agent.observation());
        STARTING_BASE_LOCATION = agent.observation().getStartLocation();

    }

    Point getStartingBaseLocation() {
        return STARTING_BASE_LOCATION;
    }

    public Point2d getCCLocation() {
        List<UnitInPool> cc = agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(TERRAN_COMMAND_CENTER));
        if (cc.size() > 0)
            return cc.get(0).unit().getPosition().toPoint2d();

        return STARTING_BASE_LOCATION.toPoint2d();
    }
    Point2d getNearestExpansionLocationTo(Point2d source) {
        return agent.query().calculateExpansionLocations(agent.observation()).stream()
                .map(Point::toPoint2d)
                .min(getLinearDistanceComparatorForPoint2d(source))
                .orElseGet(null);
    }

    Optional<Unit> findNearestBuilding(UnitType unitType, Point2d location) {
        return agent.observation().getUnits(Alliance.SELF).stream()
                .map(UnitInPool::getUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(unit -> unit.getType().equals(unitType))
                .min(getLinearDistanceComparatorForUnit(location));
    }


    Optional<Unit> findNearestVespene(Point2d location) {

        List<Unit> refineries = utils.getFinishedUnits(Units.TERRAN_REFINERY);

        return agent.observation().getUnits(Alliance.NEUTRAL).stream()
                .map(UnitInPool::getUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(unit -> unit.getType().equals(Units.NEUTRAL_VESPENE_GEYSER))
                .filter(unit -> positionNotIn(unit, refineries))
                .peek(u -> log.info("Distance to site :" + u.getTag() + " is " + agent.query().pathingDistance(u, location)))
                .min(getLinearDistanceComparatorForUnit(location));
    }

    boolean positionNotIn(Unit unit, List<Unit> units) {
        return units.stream()
                .map(Unit::getPosition)
                .noneMatch(x -> x.equals(unit.getPosition()));
    }

    Comparator<Unit> getLinearDistanceComparatorForUnit(Point2d location) {
        return (u1, u2) -> {
            Double d1 = u1.getPosition().toPoint2d().distance(location);
            Double d2 = u2.getPosition().toPoint2d().distance(location);
            return d1.compareTo(d2);
        };
    }

    Comparator<Unit> getPathingDistanceComparatorForUnit(Point2d location) {
        return (u1, u2) -> {
            Float f1 = agent.query().pathingDistance(u1.getPosition().toPoint2d(), location);
            Float f2 = agent.query().pathingDistance(u2.getPosition().toPoint2d(), location);
            return f1.compareTo(f2);
        };
    }

    private Comparator<Point2d> getLinearDistanceComparatorForPoint2d(Point2d source) {
        return (p1, p2) -> {
            Double d1 = p1.distance(source);
            Double d2 = p2.distance(source);
            return d1.compareTo(d2);
        };
    }

    private Comparator<Point2d> getPathingDistanceComparatorForPoint2d(Point2d source) {
        return (p1, p2) -> {
            Float f1 = agent.query().pathingDistance(p1, source);
            Float f2 = agent.query().pathingDistance(p2, source);
            return f1.compareTo(f2);
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
    public Optional<Point2d> findEnemyPosition() {
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

    Unit getRandomUnit(Units unitType) {
        List<Unit> units = utils.getFinishedUnits(unitType);
        return units.get(ThreadLocalRandom.current().nextInt(units.size()));
    }

    float getRandomScalar() {
        return ThreadLocalRandom.current().nextFloat() * 2 - 1;
    }

}

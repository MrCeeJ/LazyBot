package com.mrceej.sc2.lazybot.utils;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.game.raw.StartRaw;
import com.github.ocraft.s2client.protocol.response.ResponseGameInfo;
import com.github.ocraft.s2client.protocol.spatial.Point;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
public class MapUtils {

    private final S2Agent agent;
    private Point STARTING_BASE_LOCATION;
    @Getter @Setter
    private UnitInPool startingBase;
    private Utils utils;
    // private List<Point> base_locations;

    public MapUtils(S2Agent agent) {
        this.agent = agent;
    }

    public void init(Utils utils) {
        this.utils = utils;
        //  base_locations = agent.query().calculateExpansionLocations(agent.observation());
        STARTING_BASE_LOCATION = agent.observation().getStartLocation();
    }

    Point getStartingBaseLocation() {
        return STARTING_BASE_LOCATION;
    }

    public Point2d getCCLocation() {
        List<UnitInPool> cc = utils.getAllMyFinishedBases();
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

    Optional<UnitInPool> findNearestBuilding(UnitType unitType, Point2d location) {
        return agent.observation().getUnits(Alliance.SELF).stream()
                .filter(unit -> unit.unit().getType().equals(unitType))
                .min(getLinearDistanceComparatorForUnit(location));
    }

    Optional<UnitInPool> findNearestVespene(UnitInPool source) {

        List<UnitInPool> refineries = utils.getFinishedUnits(Units.TERRAN_REFINERY);

        return agent.observation().getUnits(Alliance.NEUTRAL, UnitInPool.isUnit(NEUTRAL_VESPENE_GEYSER)).stream()
                .filter(unit -> positionNotIn(unit, refineries))
//                .peek(u -> log.info("Pathing distance to site :" + u.getTag() + " is " + agent.query().pathingDistance(u.unit(), source.unit().getPosition().toPoint2d())))
//                .peek(u -> log.info("Line distance to site :" + u.getTag() + " is " +u.unit().getPosition().toPoint2d().distance(source.unit().getPosition().toPoint2d())))
                .min(getLinearDistanceComparatorForUnit(source.unit().getPosition().toPoint2d()));
    }

    private boolean positionNotIn(UnitInPool unit, List<UnitInPool> units) {
        return units.stream()
                .noneMatch(u -> u.unit().getPosition().equals(unit.unit().getPosition()));
    }

    public Comparator<UnitInPool> getLinearDistanceComparatorForUnit(Point2d location) {
        return (u1, u2) -> {
            Double d1 = u1.unit().getPosition().toPoint2d().distance(location);
            Double d2 = u2.unit().getPosition().toPoint2d().distance(location);
            return d1.compareTo(d2);
        };
    }

    Comparator<Unit> getPathingDistanceComparatorForUnit(Unit unit) {
        Point2d destination = unit.getPosition().toPoint2d();
        return (u1, u2) -> {
            Float f1 = agent.query().pathingDistance(u1.getPosition().toPoint2d(), destination);
            Float f2 = agent.query().pathingDistance(u2.getPosition().toPoint2d(), destination);
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

    public Optional<Unit> findNearestMineralPatch(Point2d start) {
        List<UnitInPool> units = agent.observation().getUnits(Alliance.NEUTRAL, UnitInPool.isUnit(NEUTRAL_MINERAL_FIELD));
        double distance = Double.MAX_VALUE;
        Unit target = null;
        for (UnitInPool unitInPool : units) {
            Unit unit = unitInPool.unit();
                double d = unit.getPosition().toPoint2d().distance(start);
                if (d < distance) {
                    distance = d;
                    target = unit;
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

    Optional<UnitInPool> getNearestFreeWorker(Point2d location) {
        return agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(TERRAN_SCV)).stream()
                .filter(unit -> unit.unit().getOrders().size() == 1)
                .filter(unit -> unit.unit().getOrders().get(0).getAbility().equals(Abilities.HARVEST_GATHER))
                .min(getLinearDistanceComparatorForUnit(location));
    }


    UnitInPool getRandomUnit(Units unitType) {
        List<UnitInPool> units = utils.getFinishedUnits(unitType);
        return units.get(ThreadLocalRandom.current().nextInt(units.size()));
    }

    float getRandomScalar() {
        return ThreadLocalRandom.current().nextFloat() * 2 - 1;
    }

}

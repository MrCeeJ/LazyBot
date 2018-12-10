package com.mrceej.sc2.lazybot.utils;

import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Ability;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;
import com.mrceej.sc2.lazybot.strategy.CjBot;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.ocraft.s2client.protocol.data.Units.*;

public class ZergUtils {

    private CjBot agent;
    private Utils utils;
    private BuildUtils buildUtils;
    private MapUtils mapUtils;

    public ZergUtils(CjBot agent) {

        this.agent = agent;
    }

    public void init(Utils utils, BuildUtils buildUtils, MapUtils mapUtils) {
        this.utils = utils;
        this.buildUtils = buildUtils;
        this.mapUtils = mapUtils;
    }

    public boolean canMorphUnit(Units unit) {
        if (!buildUtils.canAffordUnit(unit)) {
            return false;
        }

        if (!hasTechRequirementsForUnit(unit)) {
            return false;
        }

        List<UnitInPool> eggs = getLarva();
        return eggs.size() != 0;
    }


    public void morphUnit(Units unitType) {
        List<UnitInPool> eggs = getLarva();
        if (eggs.size() > 0) {
            agent.actions().unitCommand(eggs.get(0).unit(), getAbilityToMakeUnit(unitType), false);
        }
    }

    private Ability getAbilityToMakeUnit(Units unitType) {
        return agent.observation().getUnitTypeData(false).get(unitType).getAbility().orElse(Abilities.INVALID);
    }

    private Optional<UnitType> getTechRequirements(Units unitType) {
        return agent.observation().getUnitTypeData(false).get(unitType).getTechRequirement();
    }

    private boolean hasTechRequirementsForUnit(Units unit) {
        return getTechRequirements(unit)
                .map(this::hasUnit)
                .orElse(true);
    }

    private boolean hasUnit(UnitType unitType) {
        return getAllUnitsOfType((Units) unitType).size() > 0;
    }

    public List<UnitInPool> getLarva() {
        return getAllUnitsOfType(ZERG_LARVA);
    }

    public List<UnitInPool> getAllUnitsOfType(Units unit) {
        return agent.observation().getUnits(Alliance.SELF, (unitInPool -> unitInPool.unit().getType().equals(unit)));
    }

    public int getDroneCount() {
        return getAllUnitsOfType(ZERG_DRONE).size();
    }

    public boolean canBuildUnit(Units unit) {
        if (!hasTechRequirementsForUnit(unit)) {
            return false;
        }
        for (UnitInPool base : getMyHatcheries()) {
            if (base.unit().getBuildProgress() == 1f) {
                if (base.unit().getOrders().size() == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildUnit(Units unit) {
        Optional<Unit> builderOptional = getMyHatcheries().stream()
                .map(UnitInPool::unit)
                .filter(base -> base.getBuildProgress() == 1f)
                .filter(base -> base.getOrders().size() == 0)
                .findFirst();

        builderOptional.ifPresent(builder -> agent.actions().unitCommand(builder, getAbilityToMakeUnit(unit), false));
    }

    public List<UnitInPool> getMyFinshedHatcheries() {
        return getMyHatcheries().stream()
                .filter(u -> u.unit().getBuildProgress() == 1f)
                .collect(Collectors.toList());
    }

    public List<UnitInPool> getMyHatcheries() {
        List<UnitInPool> hatcheries = getAllUnitsOfType(ZERG_HATCHERY);
        hatcheries.addAll(getAllUnitsOfType(ZERG_LAIR));
        hatcheries.addAll(getAllUnitsOfType(ZERG_HIVE));
        return hatcheries;
    }

    public boolean needSupply(int supplyBuffer) {
        int available = agent.observation().getFoodCap() + getSupplyInProgress();
        int need = supplyBuffer + agent.observation().getFoodUsed();
        return agent.observation().getFoodCap() < 200 && need > available;
    }

    private int getSupplyInProgress() {
        return (int) getAllUnitsOfType(ZERG_OVERLORD).stream()
                .filter(u -> u.unit().getBuildProgress() < 1f)
                .count() * 8;
    }

    public void buildHatchery() {
        Point2d location = mapUtils.getNearestExpansionLocationTo(mapUtils.getStartingBaseLocation().toPoint2d());
        Optional<UnitInPool> unitOptional = getNearestFreeDrone(location);
        unitOptional.ifPresent(unit -> agent.actions().unitCommand(unit.unit(), Abilities.BUILD_HATCHERY, location, false));
    }

    Optional<UnitInPool> getNearestFreeDrone(Point2d location) {
        return agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(ZERG_DRONE)).stream()
                .filter(unit -> unit.unit().getOrders().size() == 1)
                .filter(unit -> unit.unit().getOrders().get(0).getAbility().equals(Abilities.HARVEST_GATHER))
                .min(mapUtils.getLinearDistanceComparatorForUnit(location));
    }
}

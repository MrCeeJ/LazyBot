package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Ability;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;
import com.github.ocraft.s2client.protocol.unit.UnitOrder;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Map.entry;

@Log4j2
public class Utils {

    private final S2Agent agent;
    public float mineralRate;
    float vespeneRate;

    public static final double MARINE_COST_PER_MIN = 166.6666666666667;
    public static final double WORKER_COST_PER_MIN = 250;

    private static final Map<UnitType, Abilities> UnitTypeToAbilityMap = Map.ofEntries(
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
            entry(Units.TERRAN_FUSION_CORE, Abilities.BUILD_FUSION_CORE),
            entry(Units.TERRAN_MARINE, Abilities.TRAIN_MARINE),
            entry(Units.TERRAN_SCV, Abilities.TRAIN_SCV),
            entry(Units.TERRAN_REAPER, Abilities.TRAIN_REAPER),
            entry(Units.TERRAN_ORBITAL_COMMAND, Abilities.MORPH_ORBITAL_COMMAND),
            entry(Units.TERRAN_TECHLAB, Abilities.BUILD_TECHLAB_BARRACKS),
            entry(Units.TERRAN_REACTOR, Abilities.BUILD_REACTOR_STARPORT),
            entry(Units.TERRAN_PLANETARY_FORTRESS, Abilities.MORPH_PLANETARY_FORTRESS),
            entry(Units.TERRAN_MEDIVAC, Abilities.TRAIN_MEDIVAC)
    );

    Utils(S2Agent agent) {
        this.agent = agent;

    }

    public int countUnitType(Units unitType) {
        return agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(unitType)).size();
    }

    void updateIncomes() {
        mineralRate = agent.observation().getScore().getDetails().getCollectionRateMinerals();
        vespeneRate = agent.observation().getScore().getDetails().getCollectionRateVespene();
    }

    Ability getAbilityTypeForStructure(UnitType unitType) {
        return UnitTypeToAbilityMap.get(unitType);
    }

    Ability getAbilityTypeForUnit(UnitType unitType) {
        return UnitTypeToAbilityMap.get(unitType);
    }

    Point2d getCCLocation() {
        Unit cc = agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_COMMAND_CENTER)).get(0).unit();
        return cc.getPosition().toPoint2d();
    }

    public List<UnitInPool> getCommandCenters() {
        return agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_COMMAND_CENTER));
    }

    public int getNumberOfBasesIncludingConstruction() {
        return getNumberOfBases() + getBuildingUnderConstruction(Abilities.BUILD_COMMAND_CENTER);
    }

    int getNumberOfBases() {
        return getCommandCenters().size();
    }

    public int getBuildingUnderConstruction(Abilities ability) {
        return agent.observation().getUnits(Alliance.SELF, doesBuildWith(ability)).size();
    }

    public int getUnitsUnderConstruction(UnitType unit) {

        return getBuildingUnderConstruction(UnitTypeToAbilityMap.get(unit));
    }

    Predicate<UnitInPool> doesBuildWith(Ability abilityTypeForStructure) {
        return unitInPool -> unitInPool.getUnit()
                .get()
                .getOrders()
                .stream()
                .anyMatch(unitOrder -> abilityTypeForStructure.equals(unitOrder.getAbility()));
    }


    int getMaxSupplyProduction() {
        int total = 0;
        total += agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_COMMAND_CENTER)).size();
        total += (agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_BARRACKS)).size() * 2);
        return total;
    }

    int getSupplyInProgress() {
        int total = 0;
        total += (countOfBuildingsInConstruction(Units.TERRAN_COMMAND_CENTER) * 10);
        total += (countOfBuildingsInConstruction(Units.TERRAN_SUPPLY_DEPOT) * 8);
        return total;
    }

    public int countOfBuildingsInConstruction(Units unit) {
        List<UnitInPool> units = agent.observation().getUnits();
        int count = 0;
        for (UnitInPool u : units) {
            if (u.getUnit().isPresent()) {
                List<UnitOrder> orders = u.getUnit().get().getOrders();
                for (UnitOrder order : orders) {
                    if (order.getAbility().equals(UnitTypeToAbilityMap.get(unit))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}

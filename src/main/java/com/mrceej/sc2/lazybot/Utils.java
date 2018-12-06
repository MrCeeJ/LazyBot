package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Ability;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.UnitOrder;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ocraft.s2client.protocol.data.Units.*;
import static java.util.Map.entry;

@Log4j2
public class Utils {

    private final S2Agent agent;
    float mineralRate;
    float vespeneRate;

    public static final double MARINE_COST_PER_MIN = 166.6666666666667;
    public static final double WORKER_COST_PER_MIN = 250;

    private static final Map<Units, Abilities> UnitTypeToAbilityMap = Map.ofEntries(
            entry(TERRAN_COMMAND_CENTER, Abilities.BUILD_COMMAND_CENTER),
            entry(TERRAN_SUPPLY_DEPOT, Abilities.BUILD_SUPPLY_DEPOT),
            entry(TERRAN_REFINERY, Abilities.BUILD_REFINERY),
            entry(TERRAN_BARRACKS, Abilities.BUILD_BARRACKS),
            entry(TERRAN_ENGINEERING_BAY, Abilities.BUILD_ENGINEERING_BAY),
            entry(TERRAN_MISSILE_TURRET, Abilities.BUILD_MISSILE_TURRET),
            entry(TERRAN_BUNKER, Abilities.BUILD_BUNKER),
            entry(TERRAN_SENSOR_TOWER, Abilities.BUILD_SENSOR_TOWER),
            entry(TERRAN_GHOST_ACADEMY, Abilities.BUILD_GHOST_ACADEMY),
            entry(TERRAN_FACTORY, Abilities.BUILD_FACTORY),
            entry(TERRAN_STARPORT, Abilities.BUILD_STARPORT),
            entry(TERRAN_ARMORY, Abilities.BUILD_ARMORY),
            entry(TERRAN_FUSION_CORE, Abilities.BUILD_FUSION_CORE),
            entry(TERRAN_MARINE, Abilities.TRAIN_MARINE),
            entry(TERRAN_SCV, Abilities.TRAIN_SCV),
            entry(TERRAN_REAPER, Abilities.TRAIN_REAPER),
            entry(TERRAN_ORBITAL_COMMAND, Abilities.MORPH_ORBITAL_COMMAND),
            entry(TERRAN_TECHLAB, Abilities.BUILD_TECHLAB_BARRACKS),
            entry(TERRAN_REACTOR, Abilities.BUILD_REACTOR_STARPORT),
            entry(TERRAN_PLANETARY_FORTRESS, Abilities.MORPH_PLANETARY_FORTRESS),
            entry(TERRAN_MEDIVAC, Abilities.TRAIN_MEDIVAC)
    );

    Utils(S2Agent agent) {
        this.agent = agent;
    }

    Ability getAbilityToBuildUnit(Units unitType) {
        return UnitTypeToAbilityMap.get(unitType);
    }

    void updateIncomes() {
        mineralRate = agent.observation().getScore().getDetails().getCollectionRateMinerals();
        vespeneRate = agent.observation().getScore().getDetails().getCollectionRateVespene();
    }

    public List<UnitInPool> getUnitsThatCanBuild(Units unitType) {
        Ability ability = UnitTypeToAbilityMap.get(unitType);
        return agent.observation().getUnits(Alliance.SELF, unitInPool -> unitInPool.unit().getType().getAbilities().contains(ability));
    }

    public int countFinishedUnitType(Units unitType) {
        return getFinishedUnits(unitType).size();
    }

    public List<UnitInPool> getFinishedUnits(Units unitType) {

        return agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(unitType)).stream()
                .filter(u -> u.unit().getBuildProgress() == 1f)
                .collect(Collectors.toList());
    }


    public int countOfUnitsIncludingUnderConstruction(Units unit) {
        return countFinishedUnitType(unit) + countOfUnitsBuildingUnit(unit);
    }

    public int countOfUnitsBuildingUnit(Units unitType) {
        return getUnitsBuildingUnit(unitType).size();
    }

    public List<UnitInPool> getUnitsBuildingUnit(Units unitType) {
        return agent.observation().getUnits(Alliance.SELF, isBuildingUnit(unitType));
    }

    public Predicate<UnitInPool> isBuildingUnit(Units unitType) {
        Ability ability = getAbilityToBuildUnit(unitType);
        return unitInPool -> unitInPool.unit().getOrders().stream()
                .map(UnitOrder::getAbility)
                .anyMatch(a -> a.equals(ability));
    }

    public int countOfUnitsBeingBuilt(Units unitType) {
        return getUnitsBeingBuilt(unitType).size();
    }

    public List<UnitInPool> getUnitsBeingBuilt(Units unitType) {
        return agent.observation().getUnits(Alliance.SELF, isBeingBuilt(unitType));
    }

    public Predicate<UnitInPool> isBeingBuilt(Units unitType) {
        return unitInPool -> unitInPool.unit().getType().equals(unitType)
                && unitInPool.unit().getBuildProgress() < 1f;
    }

    public int getMaxSupplyProduction() {
        int total = 0;
        total += getAllMyFinishedBases().size();
        total += (agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_BARRACKS)).size() * 2);
        return total;
    }

    public int getSupplyInProgress() {
        int total = 0;
        total += (countOfUnitsBuildingUnit(TERRAN_COMMAND_CENTER) * 10);
        total += (countOfUnitsBuildingUnit(Units.TERRAN_SUPPLY_DEPOT) * 8);
        return total;
    }

    public int getMineralCost(Units unit) {
        return agent.observation().getUnitTypeData(false).get(unit).getMineralCost().get();
    }

    public int getGasCost(Units unit) {
        return agent.observation().getUnitTypeData(false).get(unit).getVespeneCost().get();
    }

    public int getMineralCost(UnitInPool unit) {
        return getMineralCost((Units) Units.from(unit.unit().getType().getUnitTypeId()));
    }

    public int getGasCost(UnitInPool unit) {
        return getGasCost((Units) Units.from(unit.unit().getType().getUnitTypeId()));

    }

    public List<UnitInPool> getAllMyFinishedBases() {
        List<UnitInPool> bases = getFinishedUnits(TERRAN_COMMAND_CENTER);
        bases.addAll(getFinishedUnits(TERRAN_ORBITAL_COMMAND));
        bases.addAll(getFinishedUnits(TERRAN_PLANETARY_FORTRESS));
        return bases;
    }

    public boolean unitIsABase(UnitType unit) {
        return unitIsABase((Units) unit);
    }

    public boolean unitIsABase(Units unit) {
        return unit.equals(TERRAN_COMMAND_CENTER) ||
                unit.equals(TERRAN_PLANETARY_FORTRESS) ||
                unit.equals(TERRAN_ORBITAL_COMMAND);
    }
}

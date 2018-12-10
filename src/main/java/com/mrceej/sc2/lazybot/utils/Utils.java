package com.mrceej.sc2.lazybot.utils;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Ability;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;
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
    public float mineralRate;
    public float vespeneRate;

    public static final double MARINE_COST_PER_MIN = 166.6666666666667;
    public static final double WORKER_COST_PER_MIN = 250;
    public static final double HELLION_COST_PER_MIN = 285.7142857142857;
    public static final double MEDIVAC_COST_PER_MIN = 200;


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
            entry(TERRAN_HELLION, Abilities.TRAIN_HELLION),
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

    private static final List<Abilities> buildAbilities = new ArrayList<>(UnitTypeToAbilityMap.values());

    public Utils(S2Agent agent) {
        this.agent = agent;
    }

    Ability getAbilityToBuildUnit(Units unitType) {
        return UnitTypeToAbilityMap.get(unitType);
    }

    public void updateIncomes() {
        mineralRate = agent.observation().getScore().getDetails().getCollectionRateMinerals();
        vespeneRate = agent.observation().getScore().getDetails().getCollectionRateVespene();
    }

    public List<UnitInPool> getBuilderBuildings() {
        return agent.observation().getUnits(Alliance.SELF,canBuildSomething() );
    }

    private Predicate<UnitInPool> canBuildSomething() {
        return unitInPool -> unitInPool.unit().getType().getAbilities().stream().anyMatch(buildAbilities::contains);
    }


    public List<UnitInPool> getUnitsThatCanBuild(Units unitType) {
        return getUnitsThatCouldBuild(unitType).stream()
                .filter(isComplete())
                .filter(isNotBuildingAnything())
                .collect(Collectors.toList());
    }

    private Predicate<UnitInPool> isComplete() {
        return unitInPool -> unitInPool.unit().getBuildProgress() == 1f;
    }

    public Predicate<UnitInPool> isNotBuildingAnything() {
        return unitInPool -> unitInPool.unit().getOrders().stream()
                .map(UnitOrder::getAbility)
                .map(ability -> (Abilities) ability)
                .noneMatch(buildAbilities::contains);
    }


    private List<UnitInPool> getUnitsThatCouldBuild(Units unitType) {
        Ability ability = UnitTypeToAbilityMap.get(unitType);
        return agent.observation().getUnits(Alliance.SELF, unitInPool -> unitInPool.unit().getType().getAbilities().contains(ability));
    }

    public int countFinishedUnitType(Units unitType) {
        return getFinishedUnits(unitType).size();
    }

    public List<UnitInPool> getFinishedUnits(Units unitType) {
        return agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(unitType)).stream()
                .filter(isComplete())
                .collect(Collectors.toList());
    }


    public int countOfUnitsIncludingUnderConstruction(Units unit) {
        return countFinishedUnitType(unit) + countOfUnitsBuildingUnit(unit);
    }

    public int countOfUnitsBuildingUnit(Units unitType) {
        return getUnitsBuildingUnit(unitType).size();
    }

    private List<UnitInPool> getUnitsBuildingUnit(Units unitType) {
        return agent.observation().getUnits(Alliance.SELF, isBuildingUnit(unitType));
    }

    private Predicate<UnitInPool> isBuildingUnit(Units unitType) {
        Ability ability = getAbilityToBuildUnit(unitType);
        return unitInPool -> unitInPool.unit().getOrders().stream()
                .map(UnitOrder::getAbility)
                .anyMatch(a -> a.equals(ability));
    }

    public List<UnitInPool> getAllUnitsBeingBuilt() {
        return agent.observation().getUnits(Alliance.SELF, Predicate.not(isComplete()));
    }

    public int countOfUnitsBeingBuilt(Units unitType) {
        return getUnitsBeingBuiltOfType(unitType).size();
    }

    List<UnitInPool> getUnitsBeingBuiltOfType(Units unitType) {
        return agent.observation().getUnits(Alliance.SELF, isBeingBuilt(unitType));
    }

    private Predicate<UnitInPool> isBeingBuilt(Units unitType) {
        return unitInPool -> unitInPool.unit().getType().equals(unitType)
                && unitInPool.unit().getBuildProgress() < 1f;
    }

    public int getMaxSupplyProduction() {
        int total = 0;
        total += getAllMyFinishedBases().size();
        total += (agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(TERRAN_BARRACKS)).size() * 2);
        total += (agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(TERRAN_FACTORY)).size() * 2);
        total += (agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(TERRAN_STARPORT)).size() * 2);
        return total;
    }

    public int getSupplyInProgress() {
        int total = 0;
        total += (countOfUnitsBuildingUnit(TERRAN_COMMAND_CENTER) * 10);
        total += (countOfUnitsBuildingUnit(Units.TERRAN_SUPPLY_DEPOT) * 8);
        return total;
    }

    public int getMineralCost(Units unit) {
        return agent.observation().getUnitTypeData(false).get(unit).getMineralCost().orElse(0);
    }

    public int getGasCost(Units unit) {
        return agent.observation().getUnitTypeData(false).get(unit).getVespeneCost().orElse(0);
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
    public List<UnitInPool> getAllBasesIncludingUnderProduction() {
        List<UnitInPool> bases = getFinishedUnits(TERRAN_COMMAND_CENTER);
        bases.addAll(getUnitsBeingBuiltOfType(TERRAN_COMMAND_CENTER));
        bases.addAll(getFinishedUnits(TERRAN_ORBITAL_COMMAND));
        bases.addAll(getUnitsBeingBuiltOfType(TERRAN_ORBITAL_COMMAND));
        bases.addAll(getFinishedUnits(TERRAN_PLANETARY_FORTRESS));
        bases.addAll(getUnitsBeingBuiltOfType(TERRAN_PLANETARY_FORTRESS));
        return bases;
    }


    public boolean unitIsABase(UnitType unit) {
        return unitIsABase((Units) unit);
    }

    private boolean unitIsABase(Units unit) {
        return unit.equals(TERRAN_COMMAND_CENTER) ||
                unit.equals(TERRAN_PLANETARY_FORTRESS) ||
                unit.equals(TERRAN_ORBITAL_COMMAND);
    }

    public String printUnitTypes(List<Units> unitList) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Units unit : unitList) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(unit);
        }
        sb.append("]");
        return sb.toString();
    }

    public String printUnits(List<UnitInPool> unitList) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (UnitInPool unit : unitList) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(unit.unit().getType());
        }
        sb.append("]");
        return sb.toString();
    }
    public boolean canBuildAdditionalUnit(Units unit) {
        return canBuildAdditionalUnit(unit, agent.observation().getMinerals(), agent.observation().getVespene());
    }

    public boolean canBuildAdditionalUnit(Units unit, int minerals, int gas) {
        if (getMineralCost(unit) > minerals || getGasCost(unit) > gas) {
            return false;
        }
        return getUnitsThatCanBuild(unit).stream()
                .anyMatch(u -> u.unit().getOrders().size() == 0);
    }
}

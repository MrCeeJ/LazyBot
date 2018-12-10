package com.mrceej.sc2.lazybot.utils;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Ability;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;
import com.github.ocraft.s2client.protocol.unit.UnitOrder;
import com.mrceej.sc2.lazybot.Combat.General;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ocraft.s2client.protocol.data.Units.*;
import static java.util.Map.entry;

@Log4j2
public
class BuildUtils {

    private final S2Agent agent;
    private Utils utils;
    private MapUtils mapUtils;
    private final Map<Units, List<Units>> techRequirements = TechUtils.getTechRequirements();
    private General general;


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


    public BuildUtils(S2Agent agent) {
        this.agent = agent;
    }

    public void init(Utils utils, MapUtils mapUtils) {
        this.utils = utils;
        this.mapUtils = mapUtils;
    }


    public void buildUnit(Units unit) {

        switch (unit) {
            case TERRAN_SCV:
            case TERRAN_MARINE:
            case TERRAN_HELLION:
            case TERRAN_HELLION_TANK:
            case TERRAN_MARAUDER:
            case TERRAN_MEDIVAC:
                buildUnitFromBuilding(unit);
                 break;
            case TERRAN_BARRACKS_TECHLAB:
            case TERRAN_BARRACKS_REACTOR:
            case TERRAN_FACTORY_TECHLAB:
            case TERRAN_FACTORY_REACTOR:
            case TERRAN_STARPORT_TECHLAB:
            case TERRAN_STARPORT_REACTOR:
                 tryToBuildAddon(unit);
                 break;
            case TERRAN_COMMAND_CENTER:
                tryToExpand();
                break;
            case TERRAN_ORBITAL_COMMAND:
                tryToBuildOrbital();
                break;
            case TERRAN_REFINERY:
                tryToBuildRefinery();
                break;
            default:
                tryToBuildBuilding(unit);
                break;
        }
    }


    Ability getAbilityToBuildUnit(Units unitType) {
        return UnitTypeToAbilityMap.get(unitType);
    }

    private void buildUnitFromBuilding(Units unitType) {
        Ability ability = getAbilityToBuildUnit(unitType);
        if (canBuildAdditionalUnit(unitType)){
            List<UnitInPool> builders = getUnitsThatCanBuild(unitType);
            if (builders.size() > 0) {
                Unit builder = builders.get(0).unit();
                agent.actions().unitCommand(builder, ability, false);
                log.info("Started construction of " + unitType + " by " + builder.getType() + ", tag : " + builder.getTag());
            }
        }
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


    public List<UnitInPool> getUnitsThatCanBuild(Units unitType) {
        return getUnitsThatCouldBuild(unitType).stream()
                .filter(isComplete())
                .filter(isNotBuildingAnything())
                .collect(Collectors.toList());
    }

    public Predicate<UnitInPool> isNotBuildingAnything() {
        return unitInPool -> unitInPool.unit().getOrders().stream()
                .map(UnitOrder::getAbility)
                .map(ability -> (Abilities) ability)
                .noneMatch(buildAbilities::contains);
    }


    public List<UnitInPool> getBuilderBuildings() {
        return agent.observation().getUnits(Alliance.SELF,canBuildSomething() );
    }

    private Predicate<UnitInPool> canBuildSomething() {
        return unitInPool -> unitInPool.unit().getType().getAbilities().stream().anyMatch(buildAbilities::contains);
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

    public Comparator<Units> getCountOfBuildingComparator() {
        return (u1, u2) -> {
            Integer c1 = countFinishedUnitType(u1);
            Integer c2 = countFinishedUnitType(u2);
            return c1.compareTo(c2);
        };
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

    public List<UnitInPool> getAllUnitsBeingBuilt() {
        return agent.observation().getUnits(Alliance.SELF, Predicate.not(isComplete()));
    }

    private Predicate<UnitInPool> isComplete() {
        return unitInPool -> unitInPool.unit().getBuildProgress() == 1f;
    }

    private boolean tryToBuildUnit(Units unitType) {
        Ability ability = getAbilityToBuildUnit(unitType);
        if (canAffordUnit(unitType)) {
            List<UnitInPool> builders = getUnitsThatCanBuild(unitType).stream()
                    .filter(u -> u.unit().getOrders().size() == 0)
                    .collect(Collectors.toList());
            for (UnitInPool builder : builders) {
                agent.actions().unitCommand(builder.unit(), ability, false);
                log.info("Started construction of " + unitType + " by " + builder.unit().getType() + ", tag : " + builder.getTag());
                return true;
            }
            log.info("No units able to build a " + unitType + " from " + builders.size() + " builders.");
            return false;
        } else {
            log.info("Not enough resources to build a " + unitType);
            return false;
        }

    }

    private boolean tryToBuildOrbital() {
        if (canBuildBuilding(TERRAN_ORBITAL_COMMAND)) {
            log.info("Attempting to morph to an Orbital");
            List<UnitInPool> commandCenters = getUnitsThatCanBuild(TERRAN_ORBITAL_COMMAND);

            for (UnitInPool cc : commandCenters) {
                if (cc.getUnit().isPresent()) {
                    agent.actions().unitCommand(cc.unit(), Abilities.MORPH_ORBITAL_COMMAND, false);
                    return true;
                }
            }
            log.info("Unable to find a CC to build a Orbital from");
            return false;
        }
        log.info("Not enough minerals or tech to build a Orbital Command Center");
        return false;
    }

    private boolean tryToBuildAddon(Units unit) {
        Ability ability = getAbilityToBuildUnit(unit);

        if (agent.observation().getMinerals() < 50) {
            log.info("Not enough minerals to build " + ability);
            return false;
        }
        if (agent.observation().getVespene() < 25 && ability.equals(Abilities.BUILD_TECHLAB)) {
            log.info("Not enough gas to build a Tech lab");
            return false;
        }
        if (agent.observation().getVespene() < 50) {
            log.info("Not enough gas to build a Reactor");
            return false;
        }
        log.info("Attempting to build a " + ability);
        List<UnitInPool> units = getFinishedUnits(unit);
        for (UnitInPool u : units) {
            if (u.unit().getAddOnTag().isEmpty()) {
                agent.actions().unitCommand(u.unit(), ability, false);
                return true;
            }
        }
        return false;
    }

    private boolean tryToExpand() {
        if (canBuildBuilding(TERRAN_COMMAND_CENTER)) {
            log.info("Attempting to build a Command Center");
            if (countOfUnitsBuildingUnit(TERRAN_COMMAND_CENTER) == 0) {
                Point2d location = mapUtils.getNearestExpansionLocationTo(mapUtils.getStartingBaseLocation().toPoint2d());
                Optional<UnitInPool> unit = mapUtils.getNearestFreeWorker(location);
                if (unit.isPresent()) {
                    agent.actions().unitCommand(unit.get().unit(), Abilities.BUILD_COMMAND_CENTER,
                            location, false);
                    general.deallocateWorker(unit.get());
                    return true;
                }

            }
        }
        log.info("Not enough minerals or tech to build a Command Center");
        return false;
    }

    private boolean tryToBuildBuilding(Units unitType) {
        Ability ability = getAbilityToBuildUnit(unitType);
        if (canBuildBuilding(unitType)) {
            UnitInPool base = mapUtils.getStartingBase();
            while (true) {
                Point2d location = base.unit().getPosition().toPoint2d().add(Point2d.of(mapUtils.getRandomScalar(), mapUtils.getRandomScalar()).mul(15.0f));
                Optional<UnitInPool> unit = mapUtils.getNearestFreeWorker(location);
                if (unit.isPresent() && agent.query().placement(ability, location)) {
                    log.info("Identified location to :" + ability + " with worker " + unit.get().getTag() + " at " + location);
                    agent.actions().unitCommand(unit.get().unit(), ability, location, false);
                    general.deallocateWorker(unit.get());
                    return true;
                }
            }
        }
        log.info("Not enough resources or tech to build a " + ability);
        return false;
    }

    private boolean tryToBuildRefineryOnUnit(UnitInPool location) {
        Optional<UnitInPool> unit = mapUtils.getNearestFreeWorker(location.unit().getPosition().toPoint2d());
        if (unit.isPresent()) {
            agent.actions().unitCommand(unit.get().unit(), getAbilityToBuildUnit(TERRAN_REFINERY), location.unit(), false);
            general.deallocateWorker(unit.get());
            return true;
        }
        return false;
    }

    private boolean tryToBuildRefinery() {
        if (agent.observation().getMinerals() > 75) {
            Optional<UnitInPool> geyser = mapUtils.findNearestVespene(mapUtils.getStartingBase());
            if (geyser.isPresent()) {
                return tryToBuildRefineryOnUnit(geyser.get());
            } else {
                log.info("Not enough minerals to build a Refinery");
                return false;
            }
        }
        log.info("Not enough minerals to build a Refinery");
        return false;
    }

    public List<UnitInPool> getBuildingsThatCanCurrentlyBuild(Units building) {
        return canAffordUnit(building) &&
                validateTechRequirements(building) ?
                getUnitsThatCanBuild(building) : new ArrayList<>();
    }

    public boolean canBuildBuilding(Units building) {
        return getBuildingsThatCanCurrentlyBuild(building).size() > 0;
    }

    private boolean validateTechRequirements(Units building) {
        List<Units> reqs = techRequirements.get(building);
        if (reqs == null) {
            log.info("ALERT! no able to determine tech reqs for : "+building);
            return false;
        }

        for (Units unitType : reqs) {
            if (getFinishedUnits(unitType).size() == 0) {
                return false;
            }
        }
        return true;
    }

    public boolean needSupply(int supplyBuffer) {
        int available = agent.observation().getFoodCap() + getSupplyInProgress();
        int need = supplyBuffer + agent.observation().getFoodUsed() + getMaxSupplyProduction();
        return agent.observation().getFoodCap() < 200 && need > available;
    }
    public Units getUnitDependency(Units unit, int minerals, int gas) {
        switch (unit) {
            case TERRAN_BARRACKS:
                return TERRAN_BARRACKS;
            case TERRAN_FACTORY:
                return TERRAN_FACTORY;
            case TERRAN_STARPORT:
                return TERRAN_STARPORT;
            case TERRAN_BARRACKS_REACTOR:
            case TERRAN_BARRACKS_TECHLAB:
                List<UnitInPool> barracks = getFinishedUnits(TERRAN_BARRACKS);
                if (barracks.isEmpty() && canBuildAdditionalUnit(TERRAN_BARRACKS, minerals, gas)) {
                    return TERRAN_BARRACKS;
                } else if (canBuildAdditionalUnit(unit, minerals, gas)) {
                    return unit;
                } else return null;
            case TERRAN_FACTORY_REACTOR:
            case TERRAN_FACTORY_TECHLAB:
                List<UnitInPool> factories = getFinishedUnits(TERRAN_FACTORY);
                if (factories.isEmpty() && canBuildAdditionalUnit(TERRAN_FACTORY, minerals, gas)) {
                    return TERRAN_FACTORY;
                } else if (canBuildAdditionalUnit(unit, minerals, gas)) {
                    return unit;
                } else return null;
            case TERRAN_STARPORT_REACTOR:
            case TERRAN_STARPORT_TECHLAB:
                List<UnitInPool> starports = getFinishedUnits(TERRAN_STARPORT);
                if (starports.isEmpty() && canBuildAdditionalUnit(TERRAN_STARPORT, minerals, gas)) {
                    return TERRAN_STARPORT;
                } else if (canBuildAdditionalUnit(unit, minerals, gas)) {
                    return unit;
                } else return null;
            default:
                return null;
        }
    }

    public boolean canAffordUnit(Units unit) {
        return getMineralCost(unit) <= utils.getMinerals() &&
                getGasCost(unit) <= utils.getGas();
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
}
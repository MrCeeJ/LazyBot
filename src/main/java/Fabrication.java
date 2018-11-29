import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Ability;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.github.ocraft.s2client.protocol.unit.Alliance;
import com.github.ocraft.s2client.protocol.unit.Unit;

import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Slf4j
class Fabrication {

    private S2Agent agent;
    private Military military;
    private Utils utils;
    private Strategy strategy;
    private MapUtils mapUtils;
    private BuildingUtils buildingUtils;

    Fabrication(S2Agent agent) {
        this.agent = agent;
    }

    void init(Utils utils, Military military, Strategy strategy, MapUtils mapUtils, BuildingUtils buildingUtils) {
        this.military = military;
        this.utils = utils;
        this.strategy = strategy;
        this.mapUtils = mapUtils;
        this.buildingUtils = buildingUtils;
    }

    void run() {
        buildNextItem();
    }

    private boolean buildNextItem() {
        int minerals = agent.observation().getMinerals();
        if (agent.observation().getFoodCap() < 200) {
            if (agent.observation().getFoodUsed() >= agent.observation().getFoodCap() - utils.getMaxSupplyProduction()) {
                if (minerals < 100) {
                    System.out.println("Not enough minerals to build a depot");
                    return false;
                } else {
                    System.out.println("Time to build a depot!");
                    tryBuildSupplyDepot();
                }
            }
        }
        Units nextConstruction = Units.INVALID;
        switch (strategy.getMode()) {
            case TECH:
                tryBuildBarracks();
                break;
            case ECONOMY:
                nextConstruction = buildEconomy();
            case MACRO:
                tryBuildBarracks();
                break;
        }
        if (nextConstruction.equals(Units.INVALID)) {
            System.out.println("Nothing to build at the moment, lets save up!");
            return false;
        } else {
            System.out.println("Time to build a :"+ nextConstruction);

            return buildUnit(nextConstruction);
        }
    }

    private Units buildEconomy() {
        System.out.println("Building up the economy. Make it rain!");
        int workers = agent.observation().getFoodWorkers();
        int bases = buildingUtils.getNumberOfBasesIncludingConstruction();
        if ((workers / bases) > 16) {
            return TERRAN_COMMAND_CENTER;
        } else if (workers < 90) {
            return TERRAN_SCV;
        } else return Units.INVALID;
    }

    private boolean buildUnit(Units unit) {
        if (unit.equals(TERRAN_COMMAND_CENTER)) {
            return tryToExpand();
        } else if (unit.equals(TERRAN_SCV)) {
            return tryToBuildSCV();
        } else if (unit.equals(TERRAN_MARINE)) {
            return tryToBuildMarine();
        } else {
            return tryToBuildBuilding(unit);
        }

    }

    private boolean tryToExpand() {
        if (agent.observation().getMinerals() > 400) {
            System.out.println("Attempting to build a cc");
            tryBuildingBuildingAt(Abilities.BUILD_COMMAND_CENTER, TERRAN_SCV, mapUtils.getNearestExpansionLocationTo(mapUtils.getStartingBaseLocation()));
            return true;
        }
        System.out.println("Not enough minerals to build a cc");

        return false;
    }

    private void tryBuildingBuildingAt(Abilities abilityTypeForStructure, Units unitType, Point2d location) {
        if (agent.observation().getUnits(Alliance.SELF, utils.doesBuildWith(abilityTypeForStructure)).isEmpty()) {
            Optional<UnitInPool> unitInPool = utils.getRandomUnit(unitType);
            if (unitInPool.isPresent()) {
                Unit unit = unitInPool.get().unit();
                agent.actions().unitCommand(
                        unit,
                        abilityTypeForStructure,
                        location, false);
                if (unit.getOrders().get(0).getAbility().equals(abilityTypeForStructure)) {
                    military.setRole(unit, Military.Role.BUILD_SUPPLY_DEPOT);

                }
            }

        }
    }

    private boolean tryToBuildMarine() {
        List<UnitInPool> barracks = agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(Units.TERRAN_BARRACKS));
        if (agent.observation().getMinerals() >= 50) {
            for (UnitInPool b : barracks) {
                if (b.getUnit().isPresent()) {
                    agent.actions().unitCommand(b.getUnit().get(), Abilities.TRAIN_MARINE, false);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryToBuildSCV() {
        if (agent.observation().getMinerals() < 50) {
            return false;
        }
        List<UnitInPool> commandCenters = agent.observation().getUnits(Alliance.SELF, UnitInPool.isUnit(TERRAN_COMMAND_CENTER));
        for (UnitInPool cc : commandCenters) {
            if (cc.getUnit().isPresent()) {
                Unit com = cc.getUnit().get();
                System.out.println("Telling the cc to build an scv!");
//                if (com.getOrders().size() == 0) {
                    agent.actions().unitCommand(com, Abilities.TRAIN_SCV, false);
                    return true;
//                }
            }
        }
        return true;
    }

    private boolean tryBuildSupplyDepot() {
        System.out.println("Attempting to build a depot");
        //return tryBuildStructure(Abilities.BUILD_SUPPLY_DEPOT, TERRAN_SCV);
       return tryToBuildBuilding(TERRAN_SUPPLY_DEPOT);
    }

    private void tryBuildBarracks() {
        if (utils.countUnitType(Units.TERRAN_SUPPLY_DEPOT) > 0 && utils.countUnitType(Units.TERRAN_BARRACKS) * Utils.MARINE_COST_PER_MIN + Utils.WORKER_COST_PER_MIN < utils.mineralRate) {
            tryBuildStructure(Abilities.BUILD_BARRACKS, TERRAN_SCV);
        }
    }

    private boolean tryToBuildBuilding(UnitType unitType) {
        if (!agent.observation().getUnits(Alliance.SELF, buildingUtils.doesBuildWith(buildingUtils.getAbilityTypeForStructure(unitType))).isEmpty()) {
            return false;
        }

        Optional<UnitInPool> unitInPool = utils.getRandomUnit(TERRAN_SCV);
        if (unitInPool.isPresent()) {
            Unit unit = unitInPool.get().unit();
            agent.actions().unitCommand(
                    unit,
                    buildingUtils.getAbilityTypeForStructure(unitType),
                    unit.getPosition().toPoint2d().add(Point2d.of(utils.getRandomScalar(), utils.getRandomScalar()).mul(15.0f)),
                    false);
            return true;
        }
        return false;
    }
    private boolean tryBuildStructure(Ability abilityTypeForStructure, UnitType unitType) {
        // Just try a random location near the unit.
        Optional<UnitInPool> unitInPool = utils.getRandomUnit(unitType);
        if (unitInPool.isPresent()) {
            Unit unit = unitInPool.get().unit();
            agent.actions().unitCommand(
                    unit,
                    abilityTypeForStructure,
                    unit.getPosition().toPoint2d().add(Point2d.of(utils.getRandomScalar(), utils.getRandomScalar()).mul(15.0f)),
                    false);
            if (unit.getOrders().get(0).getAbility().equals(abilityTypeForStructure)) {
                military.setRole(unit, Military.Role.BUILD_SUPPLY_DEPOT);
            }
        }

        return true;
    }
}

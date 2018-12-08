package com.mrceej.sc2.lazybot.utils;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.data.Ability;
import com.github.ocraft.s2client.protocol.data.Units;
import com.github.ocraft.s2client.protocol.spatial.Point2d;
import com.mrceej.sc2.lazybot.lazyBot.General;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
public
class BuildUtils {

    private final S2Agent agent;
    private Utils utils;
    private MapUtils mapUtils;
    private final Map<Units, List<Units>> techRequirements = TechUtils.getTechRequirements();
    private General general;


    public BuildUtils(S2Agent agent) {
        this.agent = agent;
    }

    public void init(Utils utils, MapUtils mapUtils, General general) {
        this.utils = utils;
        this.mapUtils = mapUtils;
        this.general = general;
    }


    public boolean buildUnit(Units unit) {

        switch (unit) {
            case TERRAN_SCV:
            case TERRAN_MARINE:
                return tryToBuildUnit(unit);
            case TERRAN_FACTORY_REACTOR:
            case TERRAN_STARPORT_TECHLAB:
                return tryToBuildAddon(unit);
            case TERRAN_COMMAND_CENTER:
                return tryToExpand();
            case TERRAN_ORBITAL_COMMAND:
                return tryToBuildOrbital();
            case TERRAN_REFINERY:
                return tryToBuildRefinery();
            default:
                return tryToBuildBuilding(unit);
        }
    }

    private boolean tryToBuildUnit(Units unitType) {
        Ability ability = utils.getAbilityToBuildUnit(unitType);
        if (agent.observation().getMinerals() >= utils.getMineralCost(unitType) && agent.observation().getVespene() >= utils.getGasCost(unitType)) {
            List<UnitInPool> builders = utils.getUnitsThatCanBuild(unitType);
            Optional<UnitInPool> builder = builders.stream()
                    .filter(u -> u.unit().getOrders().size() == 0)
                    .findFirst();
            if (builder.isPresent()) {
                agent.actions().unitCommand(builder.get().unit(), ability, false);
                log.info("Started construction of " + unitType + " by " + builder.get().unit().getType() + ", tag : " + builder.get().getTag());
                return true;
            } else {
                log.info("No units able to build a " + unitType + " from " + builders.size() + " builders.");
                return false;
            }
        } else {
            log.info("Not enough resources to build a " + unitType);
            return false;
        }
    }

    private boolean tryToBuildOrbital() {
        if (canBuildBuilding(TERRAN_ORBITAL_COMMAND)) {
            log.info("Attempting to morph to an Orbital");
            List<UnitInPool> commandCenters = utils.getUnitsThatCanBuild(TERRAN_ORBITAL_COMMAND);

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
        Ability ability = utils.getAbilityToBuildUnit(unit);

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
        List<UnitInPool> units = utils.getFinishedUnits(unit);
        for (UnitInPool u : units) {
            if (u.unit().getAddOnTag().isEmpty()) {
                agent.actions().unitCommand(u.unit(), ability, false);
                general.deallocateWorker(u);
                return true;
            }
        }
        return false;
    }

    private boolean tryToExpand() {
        if (canBuildBuilding(TERRAN_COMMAND_CENTER)) {
            log.info("Attempting to build a Command Center");
            if (utils.countOfUnitsBuildingUnit(TERRAN_COMMAND_CENTER) == 0) {
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
        Ability ability = utils.getAbilityToBuildUnit(unitType);
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
            agent.actions().unitCommand(unit.get().unit(), utils.getAbilityToBuildUnit(TERRAN_REFINERY), location.unit(), false);
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

    public boolean canBuildBuilding(Units building) {
        return utils.getMineralCost(building) <= agent.observation().getMinerals() &&
                utils.getGasCost(building) <= agent.observation().getVespene() &&
                validateTechRequirements(building);
    }

    private boolean validateTechRequirements(Units building) {
        List<Units> reqs = techRequirements.get(building);
        for (Units unitType : reqs) {
            if (utils.getFinishedUnits(unitType).size() == 0) {
                return false;
            }
        }
        return true;
    }

    public boolean needSupply(int supplyBuffer) {
        int available = agent.observation().getFoodCap() + utils.getSupplyInProgress();
        int need = supplyBuffer + agent.observation().getFoodUsed() + utils.getMaxSupplyProduction();
        return agent.observation().getFoodCap() < 200 && need > available;
    }
}
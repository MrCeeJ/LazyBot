package com.mrceej.sc2.lazybot.utils;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import lombok.extern.log4j.Log4j2;

import java.util.List;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
public class Utils {

    private final S2Agent agent;
    public float mineralRate;
    public float vespeneRate;

    public static final double MARINE_COST_PER_MIN = 166.6666666666667;
    public static final double WORKER_COST_PER_MIN = 250;
    public static final double HELLION_COST_PER_MIN = 285.7142857142857;
    public static final double MEDIVAC_COST_PER_MIN = 200;
    private BuildUtils buildUtils;


    public Utils(S2Agent agent) {
        this.agent = agent;
    }

    public void init(BuildUtils buildUtils){

        this.buildUtils = buildUtils;
    }

    public void updateIncomes() {
        mineralRate = agent.observation().getScore().getDetails().getCollectionRateMinerals();
        vespeneRate = agent.observation().getScore().getDetails().getCollectionRateVespene();
    }

    public int getMinerals() {
        return agent.observation().getMinerals();
    }
    public int getGas() {
        return agent.observation().getVespene();
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


    public double getCurrentExpenditure() {
        double total = 0d;
        total += buildUtils.getAllMyFinishedBases().size() * Utils.WORKER_COST_PER_MIN;
        total += buildUtils.countFinishedUnitType(Units.TERRAN_BARRACKS) * Utils.MARINE_COST_PER_MIN;
        total += buildUtils.countFinishedUnitType(Units.TERRAN_FACTORY) * Utils.HELLION_COST_PER_MIN;
        total += buildUtils.countFinishedUnitType(Units.TERRAN_STARPORT) * Utils.MEDIVAC_COST_PER_MIN;
        return total;
    }

    public double getPlannedExpenditure() {
        double total = 0;
        total += buildUtils.countOfUnitsBuildingUnit(Units.TERRAN_COMMAND_CENTER) * Utils.WORKER_COST_PER_MIN;
        total += buildUtils.countOfUnitsBuildingUnit(TERRAN_PLANETARY_FORTRESS) * Utils.WORKER_COST_PER_MIN;
        total += buildUtils.countOfUnitsBuildingUnit(TERRAN_ORBITAL_COMMAND) * Utils.WORKER_COST_PER_MIN;
        total += buildUtils.countOfUnitsBuildingUnit(Units.TERRAN_BARRACKS) * Utils.MARINE_COST_PER_MIN;
        total += buildUtils.countOfUnitsBuildingUnit(TERRAN_FACTORY) * Utils.HELLION_COST_PER_MIN;
        total += buildUtils.countOfUnitsBuildingUnit(TERRAN_STARPORT) * Utils.MEDIVAC_COST_PER_MIN;

        return total;
    }
}

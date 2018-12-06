package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.BuildUtils;
import com.mrceej.sc2.lazybot.Utils;
import lombok.extern.log4j.Log4j2;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
public class Macro extends Doctrine {

    private float income;
    private double expenditure;

    public Macro(S2Agent agent, Utils utils, BuildUtils buildUtils) {
        super(agent, utils, buildUtils);

    }

    @Override
    public double calculateUrgency() {
        this.income = agent.observation().getScore().getDetails().getCollectionRateMinerals();
        this.expenditure = getExpenditure();
        return 21 * Utils.MARINE_COST_PER_MIN / (income - expenditure);
    }

    @Override
    public Units getConstructionOrder(int minerals, int gas) {

        if ((utils.countOfUnitsBuildingUnit(TERRAN_MARINE) < utils.countFinishedUnitType(TERRAN_BARRACKS))) {
            if (minerals > 50) {
                return Units.TERRAN_MARINE;
            } else {
                return Units.INVALID;
            }
        } else if (income - getPlannedExpenditure() > Utils.MARINE_COST_PER_MIN) {
            if (minerals > 150) {
                return Units.TERRAN_BARRACKS;
            } else return Units.INVALID;
        } else {
            return null;
        }
    }

    double getExpenditure() {
        return utils.countFinishedUnitType(Units.TERRAN_BARRACKS) * Utils.MARINE_COST_PER_MIN + Utils.WORKER_COST_PER_MIN;
    }

    double getPlannedExpenditure() {
        double total = getExpenditure();
        total += utils.countOfUnitsBuildingUnit(Units.TERRAN_BARRACKS) * Utils.MARINE_COST_PER_MIN;
        total += utils.countOfUnitsBuildingUnit(Units.TERRAN_COMMAND_CENTER) * Utils.WORKER_COST_PER_MIN;

        return total;
    }

    @Override
    public String getName() {
        return "Macro";
    }

    @Override
    public void debugStatus() {
        log.info("Macro: +" + income + " -" + expenditure + " -> urgency : " + urgency);
        log.info("Barracks :" + utils.countFinishedUnitType(TERRAN_BARRACKS));
        log.info("Marines in construction : " + utils.countOfUnitsBuildingUnit(TERRAN_MARINE));

    }
}

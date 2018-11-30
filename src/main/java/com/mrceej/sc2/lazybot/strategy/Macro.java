package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.Utils;
import lombok.extern.log4j.Log4j2;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
public class Macro implements Doctrine {

    private final S2Agent agent;
    private Utils utils;
    double urgency;
    private float income;
    private double expenditure;

    public Macro(S2Agent agent) {
        this.agent = agent;
    }


    public void init(Utils utils) {
        this.utils = utils;
    }

    @Override
    public void calculateUrgency() {
        this.income = agent.observation().getScore().getDetails().getCollectionRateMinerals();
        this.expenditure = getExpenditure();
        this.urgency = 21 * Utils.MARINE_COST_PER_MIN / (income - expenditure);
    }

    @Override
    public Units getConstructionOrder() {
        int minerals = agent.observation().getMinerals();

        if ((utils.countOfBuildingsInConstruction(TERRAN_MARINE) < utils.countUnitType(TERRAN_BARRACKS))) {
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
        return utils.countUnitType(Units.TERRAN_BARRACKS) * Utils.MARINE_COST_PER_MIN + Utils.WORKER_COST_PER_MIN;
    }

    double getPlannedExpenditure() {
        double total = getExpenditure();
        total += utils.countOfBuildingsInConstruction(Units.TERRAN_BARRACKS) * Utils.MARINE_COST_PER_MIN;
        total += utils.countOfBuildingsInConstruction(Units.TERRAN_COMMAND_CENTER) * Utils.WORKER_COST_PER_MIN;

        return total;
    }

    @Override
    public String getName() {
        return "Macro";
    }

    @Override
    public void debugStatus() {
        log.info("Macro: +" + income + " -" + expenditure + " -> urgency : " + urgency);
        log.info("Barracks :" + utils.countUnitType(TERRAN_BARRACKS));
        log.info("Marines in construction : " + utils.countOfBuildingsInConstruction(TERRAN_MARINE));

    }
}
package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.Utils;
import lombok.extern.log4j.Log4j2;

import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_COMMAND_CENTER;
import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_SCV;

@Log4j2
public class Economic implements Doctrine {

    private final S2Agent agent;
    private double urgency;
    private Utils utils;

    public Economic(S2Agent agent, Utils utils) {
        this.agent = agent;
        this.utils = utils;
    }

    @Override
    public void calculateUrgency() {
        int totalSCVs = 116;
        int currentSCVs = utils.countFinishedUnitType(Units.TERRAN_SCV);
        this.urgency = 100d * currentSCVs / totalSCVs;
    }

    @Override
    public Units getConstructionOrder(int minerals, int gas) {
        int workers = agent.observation().getFoodWorkers();
        int bases = utils.countOfUnitsIncludingUnderConstruction(TERRAN_COMMAND_CENTER);
        if (bases > 0) {
            if ((utils.countOfUnitUnderConstruction(TERRAN_COMMAND_CENTER) == 0) && (workers / bases) > 14) {
                if (minerals >= 400) {
                    return TERRAN_COMMAND_CENTER;
                } else {
                    //log.info(" .. Saving up for a command center");
                    return Units.INVALID;
                }
            } else if ((utils.countOfUnitUnderConstruction(TERRAN_SCV) < utils.countFinishedUnitType(TERRAN_COMMAND_CENTER)) && workers < 90) {
                if (minerals >= 50) {
                    return TERRAN_SCV;
                } else {
                    //log.info(" .. Saving up for an scv");
                    return Units.INVALID;
                }
            }
            return null;

        }
        log.warn(" .. no bases! panic!)");
        return null;
    }

    @Override
    public String getName() {
        return "Economic";
    }

    @Override
    public void debugStatus() {
        log.info("Economy : " + urgency);
        log.info("Workers in production :" + utils.countOfUnitUnderConstruction(TERRAN_SCV));
        log.info("Max Workers in production :" + utils.countFinishedUnitType(TERRAN_COMMAND_CENTER));
    }

}

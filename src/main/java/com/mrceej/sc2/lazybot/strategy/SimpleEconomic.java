package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.Utils;
import lombok.extern.log4j.Log4j2;

import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_COMMAND_CENTER;
import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_SCV;

@Log4j2
public class SimpleEconomic implements Doctrine {

    private final S2Agent agent;
    private double urgency;
    private Utils utils;

    public SimpleEconomic(S2Agent agent, Utils utils) {
        this.agent = agent;
        this.utils = utils;
    }

    @Override
    public void calculateUrgency() {
        this.urgency = 10;
    }

    @Override
    public Units getConstructionOrder(int minerals, int gas) {
        int workers = agent.observation().getFoodWorkers();
        int bases = utils.getNumberOfBasesIncludingConstruction();

        // If we need a new cc, save up
        if (utils.countOfBuildingsInConstruction(TERRAN_COMMAND_CENTER) == 0){
            if ((workers / bases) > 18) {
                if (minerals >= 400) {
                    return TERRAN_COMMAND_CENTER;
                } else {
                    //log.info(" .. Saving up for a command center");
                    return Units.INVALID;
                }
            }
        }
        // If we can build a worker, do it
        if ((utils.countOfBuildingsInConstruction(TERRAN_SCV) < utils.countUnitType(TERRAN_COMMAND_CENTER)) && workers < 90) {
            if (minerals >= 50) {
                return TERRAN_SCV;
            } else {
                //log.info(" .. Saving up for an scv");
                return Units.INVALID;
            }
        }
        // nothing to build
        return null;
    }

    @Override
    public String getName() {
        return "BasicEconomic";
    }

    @Override
    public void debugStatus() {
        log.info("Economy : " + urgency);
        log.info("Workers in production :" + utils.countOfBuildingsInConstruction(TERRAN_SCV));
        log.info("Max Workers in production :" + utils.countUnitType(TERRAN_COMMAND_CENTER));
    }

}

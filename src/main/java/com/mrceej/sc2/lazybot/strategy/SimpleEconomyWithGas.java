package com.mrceej.sc2.lazybot.strategy;

import SC2APIProtocol.Raw;
import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.Utils;
import lombok.extern.log4j.Log4j2;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
public class SimpleEconomyWithGas implements Doctrine {

    private final S2Agent agent;
    private double urgency;
    private Utils utils;
    private Units lastDesiredUnitType;

    public SimpleEconomyWithGas(S2Agent agent, Utils utils) {
        this.agent = agent;
        this.utils = utils;
    }

    @Override
    public void calculateUrgency() {
        this.urgency = 10;
    }

    @Override
    public Units getConstructionOrder(int minerals, int gas) {
        int workers = utils.countFinishedUnitType(TERRAN_SCV);
        int bases = utils.countOfUnitsIncludingUnderConstruction(TERRAN_COMMAND_CENTER);
        int gases = utils.countOfUnitsIncludingUnderConstruction(TERRAN_REFINERY);

        // Don;t crash if we are out of bases :)
        if (bases == 0) {
            lastDesiredUnitType = TERRAN_COMMAND_CENTER;
            if (minerals >= 400) {
                return lastDesiredUnitType;
            } else {
                return Units.INVALID;
            }
        }
        // If we need a new cc, save up
        if (utils.countOfUnitUnderConstruction(TERRAN_COMMAND_CENTER) == 0) {
            if ((workers / bases) > 16) {
                if ((gases / bases) < 2) {
                    lastDesiredUnitType = TERRAN_REFINERY;
                    if (minerals >= 75) {
                        return lastDesiredUnitType;
                    } else {
                        return INVALID;
                    }
                } else {
                    lastDesiredUnitType = TERRAN_COMMAND_CENTER;
                    if (minerals >= 400) {
                        return lastDesiredUnitType;
                    } else {
                        return Units.INVALID;
                    }
                }
            }
        }

        // If we can build a worker, do it
        if ((utils.countOfUnitUnderConstruction(TERRAN_SCV) < utils.countFinishedUnitType(TERRAN_COMMAND_CENTER)) && workers < 90) {
            lastDesiredUnitType = TERRAN_SCV;
            if (minerals >= 50) {
                return lastDesiredUnitType;
            } else {
                return Units.INVALID;
            }
        }
        // nothing to build
        lastDesiredUnitType = null;
        return null;
    }

    @Override
    public String getName() {
        return "Simple Economy With Gas";
    }

    @Override
    public void debugStatus() {
        log.info("Economy : " + urgency);
        log.info("Workers in production :" + utils.countOfUnitUnderConstruction(TERRAN_SCV));
        log.info("Max Workers in production :" + utils.countFinishedUnitType(TERRAN_COMMAND_CENTER));
        log.info("Currenty want a :" + lastDesiredUnitType);
    }
}

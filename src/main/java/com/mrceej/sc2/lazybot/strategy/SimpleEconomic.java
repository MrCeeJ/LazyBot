package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.Utils;
import lombok.extern.log4j.Log4j2;

import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_COMMAND_CENTER;
import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_SCV;

@Log4j2
public class SimpleEconomic extends Doctrine {

    public SimpleEconomic(S2Agent agent, Utils utils) {
        super(agent, utils);

    }

    @Override
    public double calculateUrgency() {
        return 10;
    }

    @Override
    public Units getConstructionOrder(int minerals, int gas) {
        int workers = utils.countFinishedUnitType(TERRAN_SCV);
        int bases = utils.countOfUnitsIncludingUnderConstruction(TERRAN_COMMAND_CENTER);

        // If we need a new cc, save up
        if (utils.countOfUnitUnderConstruction(TERRAN_COMMAND_CENTER) == 0){
            this.setConstructionDesire(TERRAN_COMMAND_CENTER);
            if ((workers / bases) > 16) {
                if (minerals >= 400) {
                    return TERRAN_COMMAND_CENTER;
                } else {
                    return Units.INVALID;
                }
            }
        }
        // If we can build a worker, do it
        if ((utils.countOfUnitUnderConstruction(TERRAN_SCV) < utils.countFinishedUnitType(TERRAN_COMMAND_CENTER)) && workers < 90) {
            this.setConstructionDesire(TERRAN_SCV);
            if (minerals >= 50) {
                return TERRAN_SCV;
            } else {
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
        log.info("Workers in production :" + utils.countOfUnitUnderConstruction(TERRAN_SCV));
        log.info("Max Workers in production :" + utils.countFinishedUnitType(TERRAN_COMMAND_CENTER));
    }

}

package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import lombok.extern.log4j.Log4j2;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
public class SimpleEconomyWithGas extends Doctrine {

    public SimpleEconomyWithGas(S2Agent agent, Utils utils, BuildUtils buildUtils) {
        super(agent, utils, buildUtils);
    }

    @Override
    public double calculateUrgency() {
        return 10;
    }

    @Override
    public Units getConstructionOrder(int minerals, int gas) {
        int workers = utils.countFinishedUnitType(TERRAN_SCV);
        int bases = utils.countOfUnitsIncludingUnderConstruction(TERRAN_COMMAND_CENTER);
        int gases = utils.countOfUnitsIncludingUnderConstruction(TERRAN_REFINERY);

        // Don;t crash if we are out of bases :)
        if (bases == 0) {
            setConstructionDesire(TERRAN_COMMAND_CENTER);
            if (minerals >= 400) {
                return TERRAN_COMMAND_CENTER;
            } else {
                return Units.INVALID;
            }
        }
        // If we need a new cc, save up
        if (utils.countOfUnitsBuildingUnit(TERRAN_COMMAND_CENTER) == 0) {
            if ((workers / bases) > 16) {
                if ((gases / bases) < 2) {
                    setConstructionDesire(TERRAN_REFINERY);
                    if (minerals >= 75) {
                        return TERRAN_REFINERY;
                    } else {
                        return INVALID;
                    }
                } else {
                    setConstructionDesire(TERRAN_COMMAND_CENTER);
                    if (minerals >= 400) {
                        return TERRAN_COMMAND_CENTER;
                    } else {
                        return Units.INVALID;
                    }
                }
            }
        }

        // If we can build a worker, do it
        if ((utils.countOfUnitsBuildingUnit(TERRAN_SCV) < utils.countFinishedUnitType(TERRAN_COMMAND_CENTER)) && workers < 90) {
            setConstructionDesire(TERRAN_SCV);
            if (minerals >= 50) {
                return TERRAN_SCV;
            } else {
                return Units.INVALID;
            }
        }
        // nothing to build
        setConstructionDesire(null);
        return null;
    }

    @Override
    public String getName() {
        return "Simple Economy With Gas";
    }

    @Override
    public void debugStatus() {
        log.info("Economy : " + urgency);
        log.info("Workers in production :" + utils.countOfUnitsBuildingUnit(TERRAN_SCV));
        log.info("Max Workers in production :" + utils.countFinishedUnitType(TERRAN_COMMAND_CENTER));
        log.info("Currently want a :" + getConstructionDesire());
    }
}

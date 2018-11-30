package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.Utils;
import lombok.extern.log4j.Log4j2;

import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_BARRACKS;
import static com.github.ocraft.s2client.protocol.data.Units.TERRAN_MARINE;

@Log4j2
public class SimpleMacro implements Doctrine {

    private final S2Agent agent;
    private Utils utils;
    double urgency;

    public SimpleMacro(S2Agent agent, Utils utils) {
        this.agent = agent;
        this.utils = utils;
    }

    @Override
    public void calculateUrgency() {
        this.urgency = 20;
    }

    @Override
    public Units getConstructionOrder(int minerals, int gas) {

        if (utils.countOfBuildingsInConstruction(TERRAN_MARINE) < utils.countUnitType(TERRAN_BARRACKS)) {
            if (minerals > 50) {
                return Units.TERRAN_MARINE;
            } else {
                return Units.INVALID;
            }
        } else if (minerals > 150) {
            return Units.TERRAN_BARRACKS;
        } else return Units.INVALID;
    }

    @Override
    public String getName() {
        return "Simple Macro";
    }

    @Override
    public void debugStatus() {
        log.info("Simple Macro:");
        log.info("Barracks :" + utils.countUnitType(TERRAN_BARRACKS));
        log.info("Marines in construction : " + utils.countOfBuildingsInConstruction(TERRAN_MARINE));
    }
}

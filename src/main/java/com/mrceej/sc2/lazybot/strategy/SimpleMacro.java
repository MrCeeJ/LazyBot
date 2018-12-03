package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.UnitType;
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
    private Units lastDesiredUnit;

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

        if (utils.countOfUnitUnderConstruction(TERRAN_MARINE) < utils.countFinishedUnitType(TERRAN_BARRACKS)) {
            lastDesiredUnit = Units.TERRAN_MARINE;
            if (minerals > 50) {
                return lastDesiredUnit;
            } else {
                return Units.INVALID;
            }
        } else if (minerals > 150) {
            lastDesiredUnit = Units.TERRAN_BARRACKS;
            return lastDesiredUnit;
        } else return Units.INVALID;
    }

    @Override
    public String getName() {
        return "Simple Macro";
    }

    @Override
    public void debugStatus() {
        log.info(this::getName);
        log.info("Barracks :" + utils.countFinishedUnitType(TERRAN_BARRACKS));
        log.info("Marines in construction : " + utils.countOfUnitUnderConstruction(TERRAN_MARINE));
        log.info("Currently wanting a : "+lastDesiredUnit);
    }
}
package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.Utils;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Tech implements Doctrine {

    private final S2Agent agent;
    private Utils utils;
    double urgency;

    public Tech(S2Agent agent, Utils utils) {
        this.agent = agent;
        this.utils = utils;
    }

    @Override
    public void calculateUrgency() {
        urgency = 100;
    }

    @Override
    public Units getConstructionOrder(int mineras, int gas) {
        return null;
    }

    @Override
    public String getName() {
        return "Tech";
    }

    @Override
    public void debugStatus() {
        log.info("Tech : " + urgency + " / 100");

    }
}

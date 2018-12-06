package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.BuildUtils;
import com.mrceej.sc2.lazybot.Utils;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Tech extends Doctrine {

    public Tech(S2Agent agent, Utils utils, BuildUtils buildUtils) {
        super(agent, utils, buildUtils);
    }

    @Override
    public double calculateUrgency() {
        return 100;
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

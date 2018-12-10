package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.Utils;

public abstract class Doctrine {


    final S2Agent agent;
    protected final Utils utils;
    protected final BuildUtils buildUtils;

    public Doctrine(S2Agent agent, Utils utils, BuildUtils buildUtils) {
        this.agent = agent;
        this.utils = utils;
        this.buildUtils = buildUtils;
    }

    public abstract void checkSupply();
    public abstract void checkMakeUnits();
    public abstract void checkMakeBuildings();

}

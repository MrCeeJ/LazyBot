package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ReactiveFabricator {
    private final S2Agent agent;
    private Strategy strategy;
    private Utils utils;
    private BuildUtils buildUtils;

    public ReactiveFabricator(S2Agent agent) {
        this.agent = agent;
    }

    public void init(Strategy strategy, Utils utils, BuildUtils buildUtils) {
        this.strategy = strategy;
        this.utils = utils;
        this.buildUtils = buildUtils;
    }

    public void onUnitCreated(Units unitType) {
    }

    public void run() {
        Doctrine d = strategy.getDoctrine();
        d.checkSupply();
        d.checkMakeUnits();
        d.checkMakeBuildings();
    }

}

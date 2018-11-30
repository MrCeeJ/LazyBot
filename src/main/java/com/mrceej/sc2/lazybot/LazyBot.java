package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import lombok.extern.log4j.Log4j2;

@Log4j2
class LazyBot extends S2Agent {
    private Strategy strategy = new Strategy(this);
    private Fabrication fabrication = new Fabrication(this);
    private General general = new General(this);
    private MapUtils mapUtils = new MapUtils(this);
    private Utils utils = new Utils(this);

    public void onGameStart() {
       log.info("Hello world of Starcraft II bots! com.mrceej.sc2.lazybot.LazyBot here!");
        init();
    }

    public void onStep() {
        if (observation().getGameLoop() % 50 == 0) {
            log.info("Game loop count :" + observation().getGameLoop());
            log.info("Minerals :" + observation().getMinerals() + " ("+utils.mineralRate+"/min)");
            log.info("Vespene :" + observation().getVespene() + " ("+utils.vespeneRate+"/min)");
        } else if (observation().getGameLoop() % 200 == 0) {
            log.info("Game loop count :" + observation().getGameLoop());
        }
        runAI();
    }

    @Override
    public void onUnitCreated(UnitInPool unit) {
        general.onUnitCreated(unit);
    }

    @Override
    public void onUnitIdle(UnitInPool unitInPool) {
        general.onUnitIdle(unitInPool);
    }

    private void init() {
        mapUtils.init();
        strategy.init(utils);
        fabrication.init(general, strategy, mapUtils, utils);
        general.init(utils, mapUtils);
    }

    private void runAI() {
        utils.updateIncomes();
        fabrication.run();
    }
}

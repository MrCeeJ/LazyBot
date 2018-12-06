package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.ClientError;
import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
class LazyBot extends S2Agent {
    private Strategy strategy = new Strategy(this);
    private Fabrication fabrication = new Fabrication(this);
    private General general = new General(this);
    private MapUtils mapUtils = new MapUtils(this);
    private Utils utils = new Utils(this);
    private BuildUtils buildUtils = new BuildUtils(this);
    private int previousMinerals = 0;

    private void init() {
        mapUtils.init(utils);
        strategy.init(utils, buildUtils);
        buildUtils.init(utils, mapUtils);
        fabrication.init(strategy, utils, buildUtils);
        general.init(utils, mapUtils, fabrication);
    }

    private void runAI() {
        utils.updateIncomes();
        fabrication.run();
    }

    @Override
    public void onGameStart() {
        log.info("Hello world of Starcraft II bots! LazyBot here!");
        init();
    }

    @Override
    public void onStep() {
        int minerals = observation().getMinerals();
        if (observation().getGameLoop() % 200 == 0 || (minerals % 25 == 0 && minerals != previousMinerals)) {
            log.info("Game loop count :" + observation().getGameLoop());
            log.info("Minerals :" + observation().getMinerals() + " (" + utils.mineralRate + "/min)");
            log.info("Vespene :" + observation().getVespene() + " (" + utils.vespeneRate + "/min)");
            previousMinerals = minerals;
        }
        runAI();
    }

    @Override
    public void onUnitCreated(UnitInPool unit) {
        general.onUnitCreated(unit);
    }

    @Override
    public void onBuildingConstructionComplete(UnitInPool units) {
        general.onBuildingConstructionComplete(units);
    }

    @Override
    public void onUnitIdle(UnitInPool unitInPool) {
        general.onUnitIdle(unitInPool);
    }

    @Override
    public void onError(List<ClientError> clientErrors, List<String> protocolErrors) {
        clientErrors.forEach(log::error);
        protocolErrors.forEach(log::error);
    }

    @Override
    public void onUnitEnterVision(UnitInPool unitInPool) {
    }

    @Override
    public void onGameFullStart() {
    }

    @Override
    public void onGameEnd() {
    }

    @Override
    public void onUnitDestroyed(UnitInPool unitInPool) {
    }

    @Override
    public void onNydusDetected() {
    }

    @Override
    public void onNuclearLaunchDetected() {
    }

}

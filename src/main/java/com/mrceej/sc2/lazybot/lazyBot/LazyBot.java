package com.mrceej.sc2.lazybot.lazyBot;

import com.github.ocraft.s2client.bot.ClientError;
import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.mrceej.sc2.lazybot.state.State;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.MapUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public
class LazyBot extends S2Agent {
    private final Strategy strategy = new Strategy(this);
    private final Fabrication fabrication = new Fabrication(this);
    private final General general = new General(this);
    private final MapUtils mapUtils = new MapUtils(this);
    private final Utils utils = new Utils(this);
    private final BuildUtils buildUtils = new BuildUtils(this);
    private int previousMinerals = 0;
    private List<State> unitStates;

    private void init() {
        unitStates = new ArrayList<>();
        mapUtils.init(utils);
        strategy.init(utils, buildUtils);
        buildUtils.init(utils, mapUtils, general);
        fabrication.init(strategy, utils, buildUtils);
        general.init(unitStates, utils, mapUtils, fabrication);
    }

    private void runAI() {
        utils.updateIncomes();
        updateUnitStates();
        fabrication.run();
    }

    private void updateUnitStates() {
        for (State state : unitStates) {
            state.updateState();
        }
    }

    @Override
    public void onGameStart() {
        log.info("Hello world of Starcraft II bots! lazyBot here!");
        init();
    }

    @Override
    public void onStep() {
        int minerals = observation().getMinerals();
        if (observation().getGameLoop() % 200 == 0 || (minerals % 25 == 0 && minerals != previousMinerals)) {
            log.info("Game loop count :" + observation().getGameLoop());
            log.info("Minerals :" + observation().getMinerals() + " (" + utils.mineralRate + "/min)");
            log.info("Vespene :" + observation().getVespene() + " (" + utils.vespeneRate + "/min)");
            log.info("Supply : ("+observation().getFoodUsed()+" / "+observation().getFoodCap()+")");
            previousMinerals = minerals;
        }
        runAI();
    }

    @Override
    public void onUnitCreated(UnitInPool unit) {
        unitStates.add(State.createState(unit));
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

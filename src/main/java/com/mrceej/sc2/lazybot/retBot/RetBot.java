package com.mrceej.sc2.lazybot.retBot;

import com.github.ocraft.s2client.bot.ClientError;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.bot.setting.PlayerSettings;
import com.github.ocraft.s2client.protocol.game.Race;
import com.mrceej.sc2.lazybot.Combat.General;
import com.mrceej.sc2.lazybot.Combat.Overmind;
import com.mrceej.sc2.lazybot.state.State;
import com.mrceej.sc2.lazybot.strategy.CjBot;
import com.mrceej.sc2.lazybot.strategy.ReactiveFabricator;
import com.mrceej.sc2.lazybot.strategy.Strategy;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.MapUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import com.mrceej.sc2.lazybot.utils.ZergUtils;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class RetBot extends CjBot {

    private final Strategy strategy = new Strategy(this);
    private final ReactiveFabricator fabricator = new ReactiveFabricator(this);
    private final Overmind overmind = new Overmind(this);
    private final MapUtils mapUtils = new MapUtils(this);
    private final Utils utils = new Utils(this);
    private final BuildUtils buildUtils = new BuildUtils(this);
    private final ZergUtils zergUtils = new ZergUtils(this);
    private int previousMinerals = 0;
    private List<State> unitStates;


    public RetBot(PlayerSettings opponent) {
        super(opponent, Race.ZERG);
    }

    private void init() {
        unitStates = new ArrayList<>();
        mapUtils.init(utils, buildUtils);
        zergUtils.init(utils, buildUtils, mapUtils);
        strategy.init(utils, buildUtils, zergUtils);
        buildUtils.init(utils, mapUtils);
        fabricator.init(strategy, utils, buildUtils);
        overmind.init(unitStates, utils, mapUtils, buildUtils, zergUtils);
    }

    private void runAI() {
        utils.updateIncomes();
        updateUnitStates();
        fabricator.run();
    }

    private void updateUnitStates() {
        for (State state : unitStates) {
            state.updateState();
        }
    }


    @Override
    public void onGameStart() {
        log.info("Hello world of Starcraft II bots! RetBot here!");
        init();
    }

    @Override
    public void onStep() {
        int minerals = observation().getMinerals();
        if (observation().getGameLoop() % 1000 == 0 || (minerals % 25 == 0 && minerals != previousMinerals)) {
            log.info("Game loop count :" + observation().getGameLoop());
            log.info("Minerals :" + observation().getMinerals() + " (" + utils.mineralRate + "/min)");
            log.info("Vespene :" + observation().getVespene() + " (" + utils.vespeneRate + "/min)");
            log.info("Supply : (" + observation().getFoodUsed() + " / " + observation().getFoodCap() + ")");
            previousMinerals = minerals;
        }
        runAI();
    }

    @Override
    public void onUnitCreated(UnitInPool unit) {
        unitStates.add(State.createState(unit));
        overmind.onUnitCreated(unit);
    }

    @Override
    public void onBuildingConstructionComplete(UnitInPool unit) {
        overmind.onBuildingConstructionComplete(unit);
    }

    @Override
    public void onUnitIdle(UnitInPool unitInPool) {
        overmind.onUnitIdle(unitInPool);
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

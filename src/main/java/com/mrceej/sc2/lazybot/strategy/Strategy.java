package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.protocol.game.Race;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import com.mrceej.sc2.lazybot.utils.ZergUtils;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Strategy {

    private final CjBot agent;
    private Utils utils;
    private BuildUtils buildUtils;
private ZergUtils zergUtils;
    private Doctrine doctrine;

    public Strategy(CjBot agent) {
        this.agent = agent;
    }

    public void init(Utils utils, BuildUtils buildUtils, ZergUtils zergUtils) {
        this.utils = utils;
        this.buildUtils = buildUtils;
        this.zergUtils = zergUtils;
        String opponentName = agent.getOpponentName();
        Race opponentRace = agent.getOpponentRace();

        log.info("Playing against :" + opponentName + " (" + opponentRace.name() + ")");

        if (opponentName.equals("RusherMcRushBot")) {
            log.info("Alert!! Cheese possible! Building safe");
//            this.doctrine = new RushDefence();
        }
        if (agent.playerRace.equals(Race.ZERG)) {
            this.doctrine = new ZvTMacro(agent, utils, buildUtils, zergUtils);
        } else {
            switch (opponentRace) {
                case ZERG:
                    this.doctrine = new TvZMacro(agent, utils, buildUtils);
                    break;
                case TERRAN:
//                this.doctrine = new TvTmacro();
                    break;
                case PROTOSS:
//                this.doctrine = new TvPmacro();
                    break;
                case RANDOM:
                case NO_RACE:
                default:
                    this.doctrine = new TvZMacro(agent, utils, buildUtils);
                    break;
            }
        }
    }

    void update() {

    }

    public Doctrine getDoctrine() {
        return doctrine;
    }


}

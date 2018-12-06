package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import lombok.extern.log4j.Log4j2;
import com.mrceej.sc2.lazybot.strategy.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
class Strategy {

    private enum Mode {basic, simple, rush, basicBuildOrder}

    private S2Agent agent;
    private List<Doctrine> doctrines;
    private Mode mode;

    Strategy(S2Agent agent) {
        this.agent = agent;
        this.doctrines = new ArrayList<>();

        mode = Mode.basicBuildOrder;
    }
    void init(Utils utils, BuildUtils buildUtils) {
        switch (mode) {
            case basicBuildOrder:
                doctrines.add(new BasicBuildOrder(agent, utils, buildUtils));
                break;
            case rush:
                doctrines.add(new TvPTimingAttack(agent, utils, buildUtils));
                break;
            case basic:
                doctrines.add(new SimpleSupply(agent, utils, buildUtils));
                doctrines.add(new Economic(agent, utils, buildUtils));
                doctrines.add(new Macro(agent, utils, buildUtils));
                doctrines.add(new Tech(agent, utils, buildUtils));
                break;
            default:
            case simple:
                doctrines.add(new SimpleSupply(agent, utils, buildUtils));
                doctrines.add(new SimpleEconomyWithGas(agent, utils, buildUtils));
                doctrines.add(new SimpleMacro(agent, utils, buildUtils));
                doctrines.add(new Tech(agent, utils, buildUtils));
                break;
        }
    }

    List<Doctrine> getPriority() {
        Collections.sort(doctrines);
        return doctrines;
    }


}

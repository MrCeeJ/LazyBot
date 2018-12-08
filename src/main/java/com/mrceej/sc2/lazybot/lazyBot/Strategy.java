package com.mrceej.sc2.lazybot.lazyBot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import lombok.extern.log4j.Log4j2;
import com.mrceej.sc2.lazybot.strategy.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
public
class Strategy {

    private enum Mode {basic, simple, rush, basicBuildOrder}

    private final S2Agent agent;
    private final List<Doctrine> doctrines;
    private final Mode mode;

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

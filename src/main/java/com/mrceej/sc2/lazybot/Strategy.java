package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import lombok.extern.log4j.Log4j2;
import com.mrceej.sc2.lazybot.strategy.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
class Strategy {

    private enum Mode {basic, simple, rush}

    private S2Agent agent;
    private List<Doctrine> doctrines;
    private Mode mode;

    Strategy(S2Agent agent) {
        this.agent = agent;
        this.doctrines = new ArrayList<>();

        mode = Mode.simple;
    }
    void init(Utils utils) {
        switch (mode) {
            case rush:
                doctrines.add(new TvPTimingAttack());
                break;
            case basic:
                doctrines.add(new SimpleSupply(agent, utils));
                doctrines.add(new Economic(agent, utils));
                doctrines.add(new Macro(agent, utils));
                doctrines.add(new Tech(agent, utils));
                break;
            default:
            case simple:
                doctrines.add(new SimpleSupply(agent, utils));
                doctrines.add(new SimpleEconomic(agent, utils));
                doctrines.add(new SimpleMacro(agent, utils));
                doctrines.add(new Tech(agent, utils));
                break;
        }
    }

    List<Doctrine> getPriority() {
        Collections.sort(doctrines);
        return doctrines;
    }


}

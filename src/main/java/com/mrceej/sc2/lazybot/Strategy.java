package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import lombok.extern.log4j.Log4j2;
import com.mrceej.sc2.lazybot.strategy.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
class Strategy {

    private S2Agent agent;
    private Utils utils;
    private Economic eco;
    private Macro macro;
    private Tech tech;
    private TvPTimingAttack tvpTimingAttack;

    private Mode mode;

    private enum Mode {basic, rush}


    Strategy(S2Agent agent) {
        this.agent = agent;
        this.eco = new Economic(agent);
        this.macro = new Macro(agent);
        this.tech = new Tech(agent);
        this.tvpTimingAttack = new TvPTimingAttack();

    }

    void init(Utils utils) {
        this.utils = utils;
        eco.init(utils);
        macro.init(utils);
        tech.init(utils);
        mode = Mode.basic;

    }

    List<Doctrine> getPriority() {
        switch (mode) {
            case rush:
                return new ArrayList<>(List.of(tvpTimingAttack));
            case basic:
            default:
                List<Doctrine> s = new ArrayList<>(List.of(eco, macro, tech));
                Collections.sort(s);
                return s;
        }
    }


}

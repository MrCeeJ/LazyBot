package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

public abstract class Doctrine implements Comparable<Doctrine> {


    final S2Agent agent;
    protected final Utils utils;
    protected final BuildUtils buildUtils;

    double urgency = 100d;

    @Setter
    @Getter
    UnitType constructionDesire = null;

    public Doctrine(S2Agent agent, Utils utils, BuildUtils buildUtils) {
        this.agent = agent;
        this.utils = utils;
        this.buildUtils = buildUtils;
    }

    public int compareTo(@NotNull Doctrine d) {
        return Double.compare(this.getUrgency(), d.getUrgency());
    }

    private double getUrgency() {
        this.urgency = calculateUrgency();
        return urgency;
    }

    protected double calculateUrgency(){
        return 0;
    }

    public abstract Units getConstructionOrder(int minerals, int gas);

    public abstract String getName();

    public abstract void debugStatus();
}

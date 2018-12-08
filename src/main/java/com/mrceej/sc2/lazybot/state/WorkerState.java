package com.mrceej.sc2.lazybot.state;

import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Ability;

public class WorkerState extends State {

    Ability ability;
    UnitInPool target;

    enum state {BUILDING, MINING}

    WorkerState(UnitInPool unit){
     super(unit);
    }

    @Override
    public void updateState() {

    }
}

package com.mrceej.sc2.lazybot.state;

import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Units;

public class State {

    private final UnitInPool unit;

    State(UnitInPool unit) {
        this.unit = unit;
    }

    public static State createState(UnitInPool unit) {

        Units type = (Units) unit.unit().getType();
        switch (type) {
            case TERRAN_SCV:
                return new WorkerState(unit);

            default:
                return new State(unit);
        }
    }

    public void updateState() {

    }
}

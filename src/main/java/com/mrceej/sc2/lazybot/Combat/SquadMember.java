package com.mrceej.sc2.lazybot.Combat;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.UnitType;
import com.github.ocraft.s2client.protocol.data.UnitTypeData;
import com.github.ocraft.s2client.protocol.unit.Unit;
import lombok.Getter;
import lombok.Setter;


public class SquadMember {

    @Getter @Setter
    private Unit unit;
    @Getter @Setter
    private UnitType type;
    @Getter
    private int mineralCost;
    @Getter
    private int gasCost;

    SquadMember(Unit unit, UnitType type){
        this.unit = unit;
        this.type = type;
        //this.mineralCost = UnitTypeData.from(SC2APIProtocol.Data.UnitTypeData.getDefaultInstance()).getMineralCost();
       // this.gasCost = type.
    }
}

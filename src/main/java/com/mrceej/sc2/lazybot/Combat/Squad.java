package com.mrceej.sc2.lazybot.Combat;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.mrceej.sc2.lazybot.MapUtils;
import com.mrceej.sc2.lazybot.Utils;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class Squad {

    private final Utils utils;
    private final MapUtils mapUtils;
    private final S2Agent agent;
    private int value ;
    @Getter
    private Orders orders;
    @Getter
    private List<UnitInPool> UnitInPools;

    public Squad(S2Agent agent, Utils utils, MapUtils mapUtils) {
        this.agent = agent;
        this.utils = utils;
        this.mapUtils = mapUtils;
        this.UnitInPools = new ArrayList<>();
        this.value = 0;
        this.orders = Orders.DEFEND;
    }

    private void updateValue() {
        int val = 0;
        for (UnitInPool u : UnitInPools) {
            val += utils.getMineralCost(u);
            val += (2 * utils.getGasCost(u));
        }
        this.value = val;
        logValue();
    }

    private void logValue() {
        log.info("Squad contains :" + getContentsAsString());
        log.info("Total value :" + value);
    }

    private String getContentsAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (UnitInPool u : UnitInPools) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(u.unit().getType().toString());
        }
        sb.append("]");
        return sb.toString();
    }

    public enum Orders {DEFEND, ATTACK}

    public void addUnitAndGiveOrders(UnitInPool UnitInPool) {
        UnitInPools.add(UnitInPool);

        if (isReadyToAttack()) {
            this.orders = Orders.ATTACK;
            giveAttackOrder();
        } else {
            giveDefaultOrder(UnitInPool);
        }
    }

    private boolean isReadyToAttack() {
        updateValue();
        return (value >= 1000);
    }

    private void giveDefaultOrder(UnitInPool unit) {
        agent.actions().unitCommand(unit.unit(), Abilities.MOVE, mapUtils.getCCLocation(), false);
    }

    private void giveAttackOrder() {
        for (UnitInPool u : UnitInPools) {
            mapUtils.findEnemyPosition().ifPresent(point2d ->
                    agent.actions().unitCommand(u.unit(), Abilities.ATTACK_ATTACK, point2d, false));
        }
    }

}

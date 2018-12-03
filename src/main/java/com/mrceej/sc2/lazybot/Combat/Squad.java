package com.mrceej.sc2.lazybot.Combat;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Abilities;
import com.github.ocraft.s2client.protocol.unit.Unit;
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
    private List<Unit> units;

    public Squad(S2Agent agent, Utils utils, MapUtils mapUtils) {
        this.agent = agent;
        this.utils = utils;
        this.mapUtils = mapUtils;
        this.units = new ArrayList<>();
        this.value = 0;
        this.orders = Orders.DEFEND;
    }

    private void updateValue() {
        int val = 0;
        for (Unit u : units) {
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
        for (Unit u : units) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(u.getType().toString());
        }
        sb.append("]");
        return sb.toString();
    }

    public enum Orders {DEFEND, ATTACK}

    public void addUnitAndGiveOrders(Unit unit) {
        units.add(unit);

        if (isReadyToAttack()) {
            this.orders = Orders.ATTACK;
            giveAttackOrder();
        } else {
            giveDefaultOrder(unit);
        }
    }

    private boolean isReadyToAttack() {
        updateValue();
        return (value >= 1000);
    }

    private void giveDefaultOrder(Unit unit) {
        agent.actions().unitCommand(unit, Abilities.MOVE, mapUtils.getCCLocation(), false);
    }

    private void giveAttackOrder() {
        for (Unit u : units) {
            mapUtils.findEnemyPosition().ifPresent(point2d ->
                    agent.actions().unitCommand(u, Abilities.ATTACK_ATTACK, point2d, false));
        }
    }

}

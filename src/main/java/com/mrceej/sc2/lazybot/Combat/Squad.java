package com.mrceej.sc2.lazybot.Combat;

import com.github.ocraft.s2client.protocol.unit.Unit;
import com.mrceej.sc2.lazybot.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Squad {

    private final Utils utils;
    private  int value;
    private Orders orders;
    private List<Unit> units ;

    public Squad(Utils utils) {
        this.utils = utils;
        this.units = new ArrayList<>();
    }

    public int getValue() {
        int val = 0;
        for (Unit u : units) {
            val += utils.getMineralCost(u);
            val += (2*utils.getGasCost(u));
        }
        return val;
    }

    public List<Unit> getUnits() {
        return units;
    }

    private enum Orders {defend, attack}

    public void add(Unit unit) {
        this.units.add(unit);
    }

    Orders getOrders() {
        return this.orders;
    }

    void setOrders(Orders o) {
        this.orders = o;
    }

    List<Unit> getUnts(){
        return units;
    }

}

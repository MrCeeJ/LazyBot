package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.protocol.data.Units;

public interface Doctrine extends Comparable<Doctrine> {

    double urgency = 100d;

    default int compareTo(Doctrine d) {
        return Double.compare(this.getUrgency(), d.getUrgency());
    }

    default double getUrgency() {
        this.calculateUrgency();
        return urgency;
    }

     void calculateUrgency();

    Units getConstructionOrder(int minerals, int gas);

    String getName();

    void debugStatus();
}

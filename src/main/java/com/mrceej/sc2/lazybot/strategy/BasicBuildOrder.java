package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.Utils;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;

import static com.github.ocraft.s2client.protocol.data.Units.*;
import static java.util.Map.entry;

@Log4j2
public class BasicBuildOrder extends Doctrine {

    private int currentOrderCount = 1;

    public BasicBuildOrder(S2Agent agent, Utils utils) {
        super(agent, utils);
    }

    private List<Units> unitPriority = List.of(TERRAN_SCV, TERRAN_MARINE, TERRAN_HELLION, TERRAN_BANSHEE);

    private Map<Integer, Units> buildOrderMap = Map.ofEntries(
            entry(1, TERRAN_SUPPLY_DEPOT),
            entry(2, TERRAN_BARRACKS),
            entry(3, TERRAN_REFINERY),
            entry(4, TERRAN_ORBITAL_COMMAND),
            entry(5, TERRAN_COMMAND_CENTER),
            entry(6, TERRAN_SUPPLY_DEPOT),
            entry(7, TERRAN_FACTORY),
            entry(8, TERRAN_FACTORY_REACTOR),
            entry(9, TERRAN_COMMAND_CENTER),
            entry(10, TERRAN_ORBITAL_COMMAND),
            entry(11, TERRAN_REFINERY),
            entry(12, TERRAN_STARPORT),
            entry(13, TERRAN_STARPORT_TECHLAB),
            entry(14, TERRAN_ORBITAL_COMMAND),
            entry(15, TERRAN_BARRACKS)
    );

    /*
     14	  0:17	  Supply Depot
      15	  0:41	  Barracks
      16	  0:46	  Refinery
      19	  1:28	  Reaper, Orbital Command
      20	  1:41	  Command Center
      20	  1:51	  Supply Depot
      21	  2:00	  Marine
      23	  2:13	  Factory
      24	  2:23	  Barracks Reactor
      25	  2:40	  Command Center
      26	  2:52	  Orbital Command
      27	  3:00	  Refinery
      27	  3:04	  Hellion x2
      32	  3:16	  Starport
      33	  3:26	  Barracks Tech Lab
      34	  3:31	  Hellion x2
      42	  3:52	  Hellion x2
      45	  3:56	  Orbital Command
      45	  3:59	  Banshee
      51	  4:13	  Barracks Tech Lab
      51	  4:16	  Supply Depot
      54	  4:24	  Barracks x2
      54	  4:30	  Factory Reactor
      56	  4:31	  Marine
      56	  4:32	  Stimpack
      58	  4:42	  Viking
      61	  4:46	  Orbital Command
      61	  4:47	  Refinery x2, Supply Depot
      61	  4:51	  Marine
      64	  5:00	  Engineering Bay x2
      66	  5:09	  Marine
      66	  5:10	  Supply Depot
      69	  5:18	  Marine x2
      72	  5:21	  Siege Tank
      72	  5:24	  Starport Reactor
      77	  5:31	  Marine
      77	  5:33	  Terran Infantry Weapons Level 1
      77	  5:35	  Terran Infantry Armor Level 1
      79	  5:41	  Marine x3
      85	  5:54	  Siege Tank
      92	  6:05	  Medivac x2
      103	  6:21	  Barracks x2
      109	  6:28	  Siege Tank
      114	  6:33	  Armory
      120	  6:48	  Refinery x2
      120	  6:52	  Factory
      120	  6:53	  Medivac x2
      128	  7:01	  Combat Shield
      131	  7:09	  Siege Tank
      131	  7:11	  Barracks Reactor x2
      138	  7:28	  Terran Vehicle Weapons Level 1
      142	  7:37	  Command Center
      143	  7:51	  Factory Tech Lab
      150	  7:53	  Siege Tank
      157	  8:29	  Barracks x2
      157	  8:35	  Barracks

     */
    @Override
    double calculateUrgency() {
        return 1d;
    }

    @Override
    public Units getConstructionOrder(int minerals, int gas) {

        for (Units u : unitPriority) {
            if (canBuildAdditionalUnit(u, minerals, gas)) {
                log.info("BBO wants to build :" + u);
                setConstructionDesire(u);
                return u;
            }
        }

        if (buildOrderMap.size() >= currentOrderCount) {
            Units building = buildOrderMap.get(currentOrderCount);
            if (utils.getMineralCost(building) <= minerals && utils.getGasCost(building) <= gas) {
                currentOrderCount++;
                log.info("BBO wants to build :" + building);
                setConstructionDesire(building);
                return building;
            }
        }
        return INVALID;
    }

    @Override
    public String getName() {
        return "Basic Build Order";
    }

    @Override
    public void debugStatus() {
        log.info("Build Order Location : " + currentOrderCount);
        log.info("last order : " + getConstructionDesire());
    }
}

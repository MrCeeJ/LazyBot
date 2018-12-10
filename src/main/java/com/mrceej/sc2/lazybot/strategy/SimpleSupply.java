package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import com.mrceej.sc2.lazybot.utils.BuildUtils;
import com.mrceej.sc2.lazybot.utils.Utils;
import lombok.extern.log4j.Log4j2;

import static com.github.ocraft.s2client.protocol.data.Units.*;

@Log4j2
public class SimpleSupply extends Doctrine {

    public SimpleSupply(S2Agent agent, Utils utils, BuildUtils buildUtils) {
        super(agent, utils, buildUtils);

    }

    @Override
    public double calculateUrgency(){
        return 0;
    }

    @Override
    public Units getConstructionOrder(int minerals, int gas) {

        if (agent.observation().getFoodCap() < 200) {
            int supplyBuffer = 2;
            if (supplyBuffer + agent.observation().getFoodUsed() + utils.getMaxSupplyProduction() > agent.observation().getFoodCap() + utils.getSupplyInProgress()) {
                setConstructionDesire(TERRAN_SUPPLY_DEPOT);
                if (minerals < 100) {
                    return INVALID;
                } else {
                    return Units.TERRAN_SUPPLY_DEPOT;
                }
            }
        }
        // no need to build
        return null;
    }

    @Override
    public String getName() {
        return "BasicSupply";
    }

    @Override
    public void debugStatus() {
        log.info("Supply : " + agent.observation().getFoodUsed());
        log.info("Supply cap :" + agent.observation().getFoodCap());
        log.info("Units in production :" + utils.getMaxSupplyProduction());
        log.info("Supply in production :" + utils.getSupplyInProgress());
    }

}

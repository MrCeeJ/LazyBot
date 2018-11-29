import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.gateway.UnitInPool;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class LazyBot extends S2Agent {
    private Strategy strategy = new Strategy(this);
    private Fabrication fabrication = new Fabrication(this);
    private Military military = new Military(this);
    private Utils utils = new Utils(this);
    private MapUtils mapUtils = new MapUtils(this);
    private BuildingUtils buildingUtils = new BuildingUtils(this);

    public void onGameStart() {
       log.debug("Hello world of Starcraft II bots! LazyBot here!");
        init();
    }

    public void onStep() {
        if (observation().getGameLoop() % 50 == 0) {
            log.debug("Game loop count :" + observation().getGameLoop());
            log.debug("Minerals :" + observation().getMinerals() + " ("+utils.mineralRate+"/min)");
            log.debug("Vespene :" + observation().getVespene() + " ("+utils.vespeneRate+"/min)");
        } else if (observation().getGameLoop() % 200 == 0) {
            log.debug("Game loop count :" + observation().getGameLoop());
        }
        runAI();
    }

    @Override
    public void onUnitCreated(UnitInPool unit) {
        military.onUnitCreated(unit);
    }

    @Override
    public void onUnitIdle(UnitInPool unitInPool) {
        military.onUnitIdle(unitInPool);
    }

    private void init() {
        mapUtils.init();
        strategy.init(utils);
        fabrication.init(utils, military, strategy, mapUtils, buildingUtils);
        military.init(utils, buildingUtils);
    }

    private void runAI() {
        utils.updateIncomes();
        fabrication.run();
    }
}

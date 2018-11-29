import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.data.Units;
import lombok.extern.log4j.Log4j2;

@Log4j2
class Strategy {

    private S2Agent agent;
    private Utils utils;

    Strategy(S2Agent agent) {
        this.agent = agent;

    }

    enum Mode {
        TECH, ECONOMY, MACRO
    }

    void init(Utils utils) {
        this.utils = utils;
    }

    Mode getMode() {
        // score econ, macro, tech, return most needed option
        double ecoScore = getEcoScore();
        double macroScore = getMacroScore();
        double techScore = getTechScore();

        if (ecoScore < macroScore) {
            if (ecoScore < techScore) {
                return Mode.ECONOMY;
            } else if (macroScore < techScore) {
                return Mode.MACRO;
            } else {
                return Mode.TECH;
            }
        } else if (macroScore < techScore) {
            return Mode.MACRO;
        } else {
            return Mode.TECH;
        }
    }

    private double getEcoScore() {
        int totalSCVs = 116;
        int currentSCVs = utils.countUnitType(Units.TERRAN_SCV);
        double score = 100d * currentSCVs / totalSCVs;
        log.debug("Economy Score : " + score);
        return score;
    }

    private double getMacroScore() {
        double income = utils.mineralRate;
        double expenditure = utils.countUnitType(Units.TERRAN_BARRACKS) * Utils.MARINE_COST_PER_MIN + Utils.WORKER_COST_PER_MIN;
        double score = 100d * expenditure / income;
        log.debug("Macro Score : " + score);
        return score;
    }

    private double getTechScore() {
        double score = 100;
        log.debug("Tech Score : " + score);
        return score;
    }
}

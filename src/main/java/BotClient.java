import com.github.ocraft.s2client.bot.S2Coordinator;
import com.github.ocraft.s2client.protocol.game.BattlenetMap;
import com.github.ocraft.s2client.protocol.game.Difficulty;
import com.github.ocraft.s2client.protocol.game.Race;

public class BotClient {

    public static void main(String[] args) {
        LazyBot bot = new LazyBot();
        S2Coordinator s2Coordinator = S2Coordinator.setup()
                .loadSettings(args)
                .setParticipants(
                        S2Coordinator.createParticipant(Race.TERRAN, bot),
                        S2Coordinator.createComputer(Race.ZERG, Difficulty.EASY))
                .launchStarcraft()
                .startGame(BattlenetMap.of("Cloud Kingdom LE"));

        while (s2Coordinator.update()) {
        }

        s2Coordinator.quit();
    }
}

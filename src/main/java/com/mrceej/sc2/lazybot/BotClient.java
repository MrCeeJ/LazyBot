package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.S2Coordinator;
import com.github.ocraft.s2client.bot.setting.PlayerSettings;
import com.github.ocraft.s2client.protocol.game.BattlenetMap;
import com.github.ocraft.s2client.protocol.game.Difficulty;
import com.github.ocraft.s2client.protocol.game.Race;
import com.mrceej.sc2.lazybot.lazyBot.LazyBot;
import com.mrceej.sc2.lazybot.retBot.RetBot;

class BotClient {

    private static final Race preferredRace = Race.ZERG;
    private static final Difficulty preferredDifficulty = Difficulty.HARD;

    public static void main(String[] args) {

        PlayerSettings opponent = getComputerOpponent();
        S2Agent playerBot = getPlayerBot(opponent);
        Race playerRace = getPlayerRace();

        S2Coordinator s2Coordinator = S2Coordinator.setup()
                .loadSettings(args)
                .setParticipants(S2Coordinator.createParticipant(playerRace, playerBot),opponent)
                .launchStarcraft()
                .startGame(BattlenetMap.of("Cloud Kingdom LE"));

        //noinspection StatementWithEmptyBody
        while (s2Coordinator.update()) {
        }
        s2Coordinator.quit();
    }

    private static Race getPlayerRace() {
        return Race.ZERG;
    }

    private static S2Agent getPlayerBot(PlayerSettings opponent) {
        if(getPlayerRace().equals(Race.ZERG)) {
            return new RetBot(opponent);
        }
        return new LazyBot(opponent);
    }

    private static PlayerSettings getComputerOpponent() {
        return S2Coordinator.createComputer(preferredRace, preferredDifficulty);
    }
}

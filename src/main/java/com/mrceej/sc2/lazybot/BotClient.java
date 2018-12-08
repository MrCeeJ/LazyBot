package com.mrceej.sc2.lazybot;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.S2Coordinator;
import com.github.ocraft.s2client.bot.setting.PlayerSettings;
import com.github.ocraft.s2client.protocol.game.BattlenetMap;
import com.github.ocraft.s2client.protocol.game.Difficulty;
import com.github.ocraft.s2client.protocol.game.Race;
import com.mrceej.sc2.lazybot.lazyBot.LazyBot;

class BotClient {

    public static void main(String[] args) {

        PlayerSettings  opponent = getComputerOpponent();
        S2Agent bot = getPlayerBot();
        Race race = getPlayerRace();
        S2Coordinator s2Coordinator = S2Coordinator.setup()
                .loadSettings(args)
                .setParticipants(S2Coordinator.createParticipant(race, bot),
                        opponent)
                .launchStarcraft()
                .startGame(BattlenetMap.of("Cloud Kingdom LE"));

        while (s2Coordinator.update()) {
        }

        s2Coordinator.quit();
    }

    private static Race getPlayerRace() {
        return Race.TERRAN;
    }

    private static S2Agent getPlayerBot() {

        // Check for opponents here
        return new LazyBot();
    }

    private static PlayerSettings getComputerOpponent() {
        //                        S2Coordinator.createComputer(Race.ZERG, Difficulty.MEDIUM_HARD))
//                      S2Coordinator.createComputer(Race.TERRAN, Difficulty.MEDIUM_HARD))
        return S2Coordinator.createComputer(Race.PROTOSS, Difficulty.HARDER);
    }
}

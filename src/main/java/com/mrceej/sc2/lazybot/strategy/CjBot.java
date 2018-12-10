package com.mrceej.sc2.lazybot.strategy;

import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.bot.setting.PlayerSettings;
import com.github.ocraft.s2client.protocol.game.Race;
import lombok.Getter;

public class CjBot extends S2Agent {

    @Getter
    final Race playerRace;
    @Getter
    final Race opponentRace;
    @Getter
    final String opponentName;


    public CjBot(PlayerSettings opponent, Race playerRace) {
        this.opponentName = opponent.getPlayerName()!=null?opponent.getPlayerName() : "AI-"+opponent.getDifficulty().name();
        this.opponentRace = opponent.getRace();
        this.playerRace = playerRace;
    }
}

package com.peter;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class HSPlayer {

    public ServerPlayerEntity player;
    public boolean isHider = false;
    public boolean isSeeker = false;

    public String nextHint = "";
    public boolean lateHint = false;

    private GameManager manager;

    public HSPlayer(ServerPlayerEntity player, GameManager manager) {
        this.player = player;
        this.manager = manager;
    }

    public String getName() {
        return player.getEntityName();
    }

    public void makeHider() {
        HideAndSeek.LOGGER.info("making player "+getName()+" hider");
        isHider = true;
        isSeeker = false;
        HideAndSeek.LOGGER.info("s1");
        try {
            HideAndSeek.scoreboard.addPlayerToTeam(getName(), manager.hiderTeam);
        } catch (Exception e) {
            HideAndSeek.LOGGER.error(e.toString());
            HideAndSeek.LOGGER.error(e.getStackTrace().toString());
        }
        HideAndSeek.LOGGER.info("s2");
        manager.timeBar.addPlayer(player);
        HideAndSeek.LOGGER.info("Made player "+getName()+" hider");
    }

    public void makeSeeker() {
        isHider = false;
        isSeeker = true;
        HideAndSeek.scoreboard.removePlayerFromTeam(player.getEntityName(), manager.hiderTeam);
        manager.timeBar.addPlayer(player);
    }

    public void clear() {
        isHider = false;
        isSeeker = false;
        HideAndSeek.scoreboard.removePlayerFromTeam(player.getEntityName(), manager.hiderTeam);
        manager.timeBar.removePlayer(player);
    }

    public void setHint(String hint) {
        nextHint = hint;
        if (lateHint) {
            manager.sendMessageToAllPlayers("Hint for " + getName() + ": " + hint+"\n");
            sendMessage("Don't forget to set your next hint");
        }
        lateHint = false;
    }

    public void sendMessage(Text message) {
        player.sendMessage(message);
    }

    public void sendMessage(String message) {
        sendMessage(Text.of(message));
    }

    public void gameStart() {
        nextHint = "";
    }
}

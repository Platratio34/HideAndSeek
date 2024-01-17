package com.peter;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class HSPlayer {

    public ServerPlayerEntity player;
    public boolean isHider = false;
    public boolean isSeeker = false;

    public String nextHint = "";
    public boolean lateHint = false;

    public boolean found = false;

    private GameManager manager;

    public HSPlayer(ServerPlayerEntity player, GameManager manager) {
        this.player = player;
        this.manager = manager;
    }

    public String getName() {
        return player.getEntityName();
    }

    public void makeHider() {
        isHider = true;
        isSeeker = false;
        HideAndSeek.scoreboard.addPlayerToTeam(getName(), manager.hiderTeam);
        manager.timeBar.addPlayer(player);
    }

    public void makeSeeker() {
        isHider = false;
        isSeeker = true;
        if(HideAndSeek.scoreboard.getPlayerTeam(player.getEntityName()) == manager.hiderTeam)
            HideAndSeek.scoreboard.removePlayerFromTeam(player.getEntityName(), manager.hiderTeam);
        HideAndSeek.scoreboard.addPlayerToTeam(getName(), manager.seekerTeam);
        manager.timeBar.addPlayer(player);
    }

    public void clear() {
        isHider = false;
        isSeeker = false;
        if(HideAndSeek.scoreboard.getPlayerTeam(player.getEntityName()) == manager.hiderTeam)
            HideAndSeek.scoreboard.removePlayerFromTeam(player.getEntityName(), manager.hiderTeam);
        if(HideAndSeek.scoreboard.getPlayerTeam(player.getEntityName()) == manager.seekerTeam)
            HideAndSeek.scoreboard.removePlayerFromTeam(player.getEntityName(), manager.seekerTeam);
        manager.timeBar.removePlayer(player);
    }

    public void setHint(String hint) {
        nextHint = hint;
        if (lateHint) {
            manager.sendMessageToAllPlayers(ChatColor.BLUE+"Hint for " + getName() + ": " + ChatColor.WHITE + hint+"\n");
            sendMessage(ChatColor.GOLD + "Don't forget to set your next hint");
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
        found = false;
        lateHint = false;
    }

    public void handleDisconnect() {
        clear();
    }
}

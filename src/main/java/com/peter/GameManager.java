package com.peter;

import java.util.HashMap;
import java.util.UUID;

import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo.Map;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.scoreboard.AbstractTeam.VisibilityRule;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class GameManager implements ServerTickEvents.StartTick {

    private final String TIME_FORMAT = "%s%02d:%02.0f";

    public HashMap<UUID, HSPlayer> players;

    public boolean running = false;
    public float time = 0;
    private long startTime = 0;

    public Team hiderTeam;
    public CommandBossBar timeBar;

    public float hideTime = 2 * 60f;

    public float[] hintTimes = new float[] {
            120, // testing
            5 * 60,
            8 * 60,
            10 * 60,
            12 * 60,
            14 * 60,
            16 * 60,
            18 * 60,
            20 * 60,
            22 * 60,
            24 * 60,
            26 * 60,
            28 * 60,
            30 * 60,
            32 * 60,
            34 * 60,
            36 * 60,
            38 * 60,
            40 * 60,
            42 * 60,
            44 * 60,
            46 * 60,
            48 * 60,
            50 * 60,
            52 * 60,
            54 * 60,
            56 * 60,
            58 * 60,
            60 * 60
    };
    private int nextHintIndex = 0;

    public GameManager() {
        players = new HashMap<UUID, HSPlayer>();
    }

    public HSPlayer getPlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (!players.containsKey(uuid)) {
            players.put(uuid, new HSPlayer(player, this));
        }
        return players.get(uuid);
    }

    public void start() {
        hiderTeam.setNameTagVisibilityRule(VisibilityRule.HIDE_FOR_OTHER_TEAMS);
        startTime = System.currentTimeMillis();
        running = true;
        nextHintIndex = 0;
        timeBar.setVisible(true);

        sendMessageToAllPlayers(ChatColor.GREEN+"Hide and Seek Starting.\n\n"+ChatColor.GOLD+"You have " + formatTime(hideTime)
                + " to hide before seekers are released");
    }

    public void stop() {
        running = false;
        hiderTeam.setNameTagVisibilityRule(VisibilityRule.ALWAYS);
        timeBar.setVisible(false);

        sendMessageToAllPlayers(ChatColor.GREEN+"Game ended at " + formatTime(time, "%s%02d:%02.1f"));
    }

    private String formatTime(float s) {
        return formatTime(s, TIME_FORMAT);
    }

    private String formatTime(float s, String formatString) {
        String sign = "";
        if (s < 0) {
            sign = "-";
            s *= -1;
        }
        if (s < 60) {
            return String.format(formatString, sign, 0, s);
        }
        int m = (int) (s / 60);
        s %= 60;
        return String.format(formatString, sign, m, s);
    }

    public void sendMessageToAllPlayers(Text message) {
        for (Map.Entry<UUID, HSPlayer> entry : players.entrySet()) {
            HSPlayer p = entry.getValue();
            if (!(p.isHider || p.isSeeker))
                continue; // This player is not currently playing
            p.sendMessage(message);
        }
    }

    public void sendMessageToAllPlayers(String message) {
        sendMessageToAllPlayers(Text.of(message));
    }

    public void sendMessageToAllHiders(Text message) {
        for (Map.Entry<UUID, HSPlayer> entry : players.entrySet()) {
            HSPlayer p = entry.getValue();
            if (!p.isHider)
                continue;
            p.sendMessage(message);
        }
    }

    public void sendMessageToAllHiders(String message) {
        sendMessageToAllHiders(Text.of(message));
    }

    public void sendMessageToAllSeekers(Text message) {
        for (Map.Entry<UUID, HSPlayer> entry : players.entrySet()) {
            HSPlayer p = entry.getValue();
            if (!p.isSeeker)
                continue;
            p.sendMessage(message);
        }
    }

    public void sendMessageToAllSeekers(String message) {
        sendMessageToAllSeekers(Text.of(message));
    }

    @Override
    public void onStartTick(MinecraftServer server) {
        if (!running)
            return;

        long runTimeM = System.currentTimeMillis() - startTime;
        float lTime = time;
        time = (runTimeM / 1000f) - hideTime;

        if (hintTimes[nextHintIndex] > lTime && hintTimes[nextHintIndex] <= time) {
            // Do the next hint
            // Check w/ lTime is for if the hint should have been done between the last tick and this one
            sendMessageToAllPlayers(ChatColor.GREEN+"Hint time! (" + formatTime(time) + ")");

            String hintText = "\n";
            for (Map.Entry<UUID, HSPlayer> entry : players.entrySet()) {
                HSPlayer p = entry.getValue();
                if (!p.isHider)
                    continue;
                if (p.nextHint.equals("")) {
                    p.lateHint = true;
                    p.sendMessage(ChatColor.RED+"You did not enter your hint, please do it quickly");
                    continue;
                }
                // sendMessageToAllPlayers("Hint for " + p.getName() + ": " + p.nextHint);
                hintText += "Hint for " + p.getName() + ": " + p.nextHint + "\n";
                p.nextHint = "";
            }
            hintText += "\n"+ChatColor.GOLD+"Next hint at " + formatTime(hintTimes[nextHintIndex + 1]) + " in "
                    + formatTime(hintTimes[nextHintIndex + 1] - hintTimes[nextHintIndex]);
            sendMessageToAllPlayers(hintText);

            sendMessageToAllHiders(ChatColor.GOLD+"Don't forget to set your next hint");

            nextHintIndex++;
        }

        if (time < 0) {
            timeBar.setName(Text.of(String.format("Hide Time: %s", formatTime(time))));

            float p = -time / hideTime;
            timeBar.setPercent(p);
            if (time > -10) {
                timeBar.setColor(Color.RED);
            } else if (time > -30) {
                timeBar.setColor(Color.YELLOW);
                if (lTime < -30) {
                    sendMessageToAllPlayers(ChatColor.GOLD+"30 seconds remaining to hide");
                }
            } else {
                timeBar.setColor(Color.GREEN);
            }
            timeBar.setPercent(-time / hideTime);
        } else if (lTime < 0 && time >= 0) {
            // Time for seeker to start
            sendMessageToAllPlayers(
                ChatColor.GREEN+"Ready or not, here they come\nSeekers start looking\n\n"+ChatColor.WHITE+"First hint in "
                            + formatTime(hintTimes[0]));
            sendMessageToAllHiders(ChatColor.GOLD+"Don't forget to set your first hint with "+ChatColor.WHITE+"/hs hint <hint>");

        } else {
            timeBar.setName(Text.of(String.format("Seek Time: %s", formatTime(time))));
            float timeToNext = hintTimes[nextHintIndex] - time;
            float timeBetweenHints = 0;
            if (nextHintIndex == 0) {
                timeBetweenHints = hintTimes[0];
            } else {
                timeBetweenHints = hintTimes[nextHintIndex] - hintTimes[nextHintIndex - 1];
            }
            float p = timeToNext / timeBetweenHints;
            timeBar.setPercent(p);
            if (timeToNext < 10) {
                timeBar.setColor(Color.RED);
                if ((hintTimes[nextHintIndex] - lTime) > 10) {
                    sendMessageToAllPlayers(ChatColor.GOLD+"10 seconds to next hint");
                }
            } else if (timeToNext < 30) {
                timeBar.setColor(Color.YELLOW);
                if ((hintTimes[nextHintIndex] - lTime) > 30) {
                    for (Map.Entry<UUID, HSPlayer> entry : players.entrySet()) {
                        HSPlayer pl = entry.getValue();
                        if (!pl.isHider)
                            continue;
                        if (pl.nextHint.equals("")) {
                            pl.sendMessage(ChatColor.GOLD+"Don't forget to set your next hint, 30 seconds left");
                        }
                    }
                }
            } else {
                timeBar.setColor(Color.GREEN);
            }
        }
    }
}

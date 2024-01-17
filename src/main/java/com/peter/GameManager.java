package com.peter;

import java.util.HashMap;
import java.util.UUID;

import org.slf4j.Logger;

import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo.Map;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.scoreboard.AbstractTeam.VisibilityRule;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GameManager implements ServerTickEvents.StartTick {

    private final String TIME_FORMAT = "%s%02d:%02.0f";
    private final String NO_RADAR_EFFECT_NAME = "xaerominimap:no_entity_radar";
    private StatusEffect NO_RADAR_EFFECT;

    public HashMap<UUID, HSPlayer> players;

    public boolean running = false;
    public float time = 0;
    private long startTime = 0;

    public Team hiderTeam;
    public Team seekerTeam;
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

    private double centerX = 0.5;
    private double centerY = 101;
    private double centerZ = 0.5;

    private Logger logger;

    public GameManager() {
        players = new HashMap<UUID, HSPlayer>();
        logger = HideAndSeek.LOGGER;
        hideTime = 30; // dev only
    }

    public HSPlayer getPlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (!players.containsKey(uuid)) {
            players.put(uuid, new HSPlayer(player, this));
        }
        return players.get(uuid);
    }

    public void start() {
        if (NO_RADAR_EFFECT == null) {
            NO_RADAR_EFFECT = Registries.STATUS_EFFECT.get(new Identifier(NO_RADAR_EFFECT_NAME));
        }
        hiderTeam.setNameTagVisibilityRule(VisibilityRule.HIDE_FOR_OTHER_TEAMS);
        startTime = System.currentTimeMillis();
        running = true;
        nextHintIndex = 0;
        timeBar.setVisible(true);
        
        for (Map.Entry<UUID, HSPlayer> entry : players.entrySet()) {
            HSPlayer p = entry.getValue();
            p.gameStart();
        }

        sendMessageToAllPlayers(ChatColor.GREEN+"Hide and Seek Starting.\n\n"+ChatColor.GOLD+"You have " + formatTime(hideTime)
                + " to hide before seekers are released");
    }

    public void stop() {
        running = false;
        hiderTeam.setNameTagVisibilityRule(VisibilityRule.ALWAYS);
        timeBar.setVisible(false);

        for (Map.Entry<UUID, HSPlayer> entry : players.entrySet()) {
            HSPlayer pl = entry.getValue();
            if (!pl.isSeeker)
                continue;
            if(pl.player.hasStatusEffect(NO_RADAR_EFFECT)) {
                pl.player.removeStatusEffect(NO_RADAR_EFFECT);
            }
        }

        sendMessageToAllPlayers(ChatColor.GREEN+"Game ended at " + formatTime(time, "%s%02d:%02.1f"));
    }

    private String formatTime(float s) {
        return formatTime(s, TIME_FORMAT);
    }

    private String formatTime(float s, String formatString) {
        String sign = "";
        if (s < 0) {
            // sign = "-";
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
            sendMessageToAllPlayers(ChatColor.GREEN + "Hint time! (" + formatTime(time) + ")");

            String hintText = "\n";
            for (Map.Entry<UUID, HSPlayer> entry : players.entrySet()) {
                HSPlayer p = entry.getValue();
                if (!p.isHider)
                    continue;
                if (p.nextHint.equals("")) {
                    p.lateHint = true;
                    p.sendMessage(ChatColor.RED + "You did not enter your hint, please do it quickly");
                    continue;
                }
                hintText += ChatColor.BLUE + "Hint for " + p.getName() + ": " + ChatColor.WHITE + p.nextHint + "\n";
                p.nextHint = "";
            }
            hintText += "\n" + ChatColor.GOLD + "Next hint at " + formatTime(hintTimes[nextHintIndex + 1]) + " in "
                    + formatTime(hintTimes[nextHintIndex + 1] - hintTimes[nextHintIndex]);
            sendMessageToAllPlayers(hintText);

            sendMessageToAllHiders(ChatColor.GOLD + "Don't forget to set your next hint");

            nextHintIndex++;
        }
        
        for (Map.Entry<UUID, HSPlayer> entry : players.entrySet()) {
            HSPlayer pl = entry.getValue();
            if (!pl.isSeeker)
                continue;
            if(!pl.player.hasStatusEffect(NO_RADAR_EFFECT)) {
                pl.player.addStatusEffect(new StatusEffectInstance(NO_RADAR_EFFECT, StatusEffectInstance.INFINITE, 1, true, false));
            }
        }

        if (time < 0) {
            timeBar.setName(Text.of(String.format("Hide Time: %s", formatTime(time))));

            float p = -time / hideTime;
            timeBar.setPercent(p);
            if (time > -10) {
                timeBar.setColor(Color.RED);
                if (lTime <= -10) {
                    sendMessageToAllPlayers(ChatColor.GOLD+"10 seconds remaining to hide");
                }
            } else if (time > -30) {
                timeBar.setColor(Color.YELLOW);
                if (lTime <= -30) {
                    sendMessageToAllPlayers(ChatColor.GOLD+"30 seconds remaining to hide");
                }
            } else {
                timeBar.setColor(Color.GREEN);
            }
            timeBar.setPercent(-time / hideTime);
            for (Map.Entry<UUID, HSPlayer> entry : players.entrySet()) {
                HSPlayer pl = entry.getValue();
                if (!pl.isSeeker)
                    continue;
                pl.player.teleport(centerX, centerY, centerZ);
            }

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
                if ((hintTimes[nextHintIndex] - lTime) >= 10) {
                    sendMessageToAllPlayers(ChatColor.GOLD+"10 seconds to next hint");
                    for (Map.Entry<UUID, HSPlayer> entry : players.entrySet()) {
                        HSPlayer pl = entry.getValue();
                        if (!pl.isHider)
                            continue;
                        if (pl.nextHint.equals("")) {
                            pl.sendMessage(ChatColor.RED+"Set you next hit, 10 seconds remaining");
                        }
                    }
                }
            } else if (timeToNext < 30) {
                timeBar.setColor(Color.YELLOW);
                if ((hintTimes[nextHintIndex] - lTime) >= 30) {
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

    public boolean checkDamage(Entity entity, DamageSource source) {
        if (!(source.getAttacker() instanceof ServerPlayerEntity)) {
            // if (source.getAttacker() != null)
            //     logger.info("Skipping damage from " + source.getAttacker().getName());
            // else logger.info("Skipping damage from unknown");
            return false;
        }
        if (!(entity instanceof ServerPlayerEntity)) {
            // logger.info("Skipping damage for " + entity.getName());
            return false;
        }
        logger.info("Thing-ing");
        return false;
    }

    public boolean checkDamage(PlayerEntity source, Entity entity) {
        if (!running)
            return false;
        if (!(entity instanceof ServerPlayerEntity)) {
            // logger.info("Skipping damage for " + entity.getName());
            return false;
        }
        UUID sUuid = source.getUuid();
        if (!players.containsKey(sUuid))
            return false;
        UUID dUuid = ((PlayerEntity)entity).getUuid();
        if (!players.containsKey(dUuid))
            return false;
        HSPlayer sPlayer = players.get(sUuid);
        if (!sPlayer.isSeeker)
            return true;
        HSPlayer dPlayer = players.get(dUuid);
        if (!dPlayer.isHider)
            return true;
        if (time < 0)
            return true;
        if (dPlayer.found)
            return true;
        dPlayer.found = true;
        dPlayer.sendMessage(ChatColor.GREEN + "You have been found");
        int remaining = 0;
        for (Map.Entry<UUID, HSPlayer> entry : players.entrySet()) {
            HSPlayer p = entry.getValue();
            if (p.isHider && !p.found) {
                remaining++;
            }
        }
        if (remaining <= 0) {
            sendMessageToAllPlayers(ChatColor.GREEN+"All players have been found");
            stop();
        } else {
            sendMessageToAllSeekers(ChatColor.GREEN+"One found, " + remaining + " to go");
        }
        return true;
    }

    public void handleDisconnect(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (!players.containsKey(uuid)) {
            return;
        }
        HSPlayer hsPlayer = players.get(uuid);
        players.remove(uuid);
        hsPlayer.handleDisconnect();
        logger.info("Removed player " + player.getName());
    }
}

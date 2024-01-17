package com.peter;

import java.util.Map;
import java.util.UUID;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.boss.BossBarManager;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.minecraft.server.command.CommandManager.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

public class HideAndSeek implements ModInitializer {

	public static final String TEAM_NAME = "hs.hiders";
	public static final Identifier TIME_BAR_ID = new Identifier("hideandseek.timebar");

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("hideandseek");

	public static MinecraftServer server;

	public static Scoreboard scoreboard;

	public static BossBarManager bossBarManager;

	public static GameManager manager;

	@Override
	public void onInitialize() {

		manager = new GameManager();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("hs")
					.then(argument("action", StringArgumentType.string())
							.executes(context -> {
								return actionCommand(context);
							})
							.then(argument("argument", StringArgumentType.greedyString())
									.executes(context -> {
										return actionCommand(context);
									}))));
			LOGGER.info("H&S command registered");
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			HideAndSeek.server = server;
			scoreboard = server.getScoreboard();

			Team team = scoreboard.getTeam(TEAM_NAME);
			if (team == null) {
				team = scoreboard.addTeam(TEAM_NAME);
			}
			if (team == null) {
				LOGGER.error("Could not make hider team");
				return;
			}
			manager.hiderTeam = team;

			bossBarManager = server.getBossBarManager();

			manager.timeBar = bossBarManager.add(TIME_BAR_ID, Text.of(""));
			manager.timeBar.setVisible(false);

			LOGGER.info("H&S teams and boss bar setup");
		});

		ServerTickEvents.START_SERVER_TICK.register(manager);

		LOGGER.info("Loaded Hide & Seek");
		LOGGER.info("        _ ");
		LOGGER.info("|__|   |_ ");
		LOGGER.info("|  | &  _|");
		LOGGER.info("          ");
	}

	private void cmdSendFeedback(CommandContext<ServerCommandSource> context, String message) {
		context.getSource().sendFeedback(
				() -> Text.literal(message),
				false);
	}

	private int actionCommand(CommandContext<ServerCommandSource> context) {
		String action = StringArgumentType.getString(context, "action");
		ServerPlayerEntity player = context.getSource().getPlayer();
		HSPlayer hsPlayer = null;
		if (player != null) {
			hsPlayer = manager.getPlayer(player);
		}
		if (action.equals("hider")) {
			LOGGER.info("trying to make player hider");
			if (player == null) {
				cmdSendFeedback(context, ChatColor.RED+"Must be a player to be a hider");
				return -1;
			}

			if (hsPlayer.isHider) {
				cmdSendFeedback(context, ChatColor.GOLD+"You are already a hider");
				return 0;
			}
			if (hsPlayer.isSeeker) {
				cmdSendFeedback(context, ChatColor.RED+"You can not be both a seeker and hider");
				return 0;
			}

			hsPlayer.makeHider();
			cmdSendFeedback(context, ChatColor.GREEN+"You are now a hider");
			return 1;
		} else if (action.equals("seeker")) {
			if (player == null) {
				cmdSendFeedback(context, ChatColor.RED+"Must be a player to be a hider");
				return -1;
			}

			if (hsPlayer.isSeeker) {
				cmdSendFeedback(context, ChatColor.GOLD+"You are already a seeker");
				return 0;
			}
			if (hsPlayer.isHider) {
				cmdSendFeedback(context, ChatColor.RED+"You can not be both a seeker and hider");
				return 0;
			}

			hsPlayer.makeSeeker();
			cmdSendFeedback(context, ChatColor.GREEN + "You are now a seeker");
			return 1;
		} else if (action.equals("clear")) {
			if (player == null) {
				cmdSendFeedback(context, ChatColor.RED+"Must be a player to be a hider");
				return -1;
			}

			if (!(hsPlayer.isSeeker || hsPlayer.isHider)) {
				cmdSendFeedback(context, ChatColor.GREEN+"You were not a hider or seeker");
				return 0;
			}
			hsPlayer.clear();
			cmdSendFeedback(context, ChatColor.RED+"You are no longer a hider or seeker");
			return 1;
		} else if (action.equals("list")) {
			String hiderNames = "";
			String seekerNames = "";
			boolean hc = false;
			boolean sc = false;
			for (Map.Entry<UUID, HSPlayer> entry : manager.players.entrySet()) {
				HSPlayer p = entry.getValue();
				if (p.isHider) {
					if (hc) {
						hiderNames += ", ";
					}
					hiderNames += p.getName();
					hc = true;
				}
				if (p.isSeeker) {
					if (sc) {
						seekerNames += ", ";
					}
					seekerNames += p.getName();
					sc = true;
				}
			}
			cmdSendFeedback(context, "Current seeker(s): " + seekerNames + "\nCurrent hider(s): " + hiderNames);
			return 1;
		} else if (action.equals("start")) {
			manager.start();
			cmdSendFeedback(context, ChatColor.GREEN+"Game starting . . .");
			return 1;
		} else if (action.equals("stop")) {
			if (!manager.running) {
				cmdSendFeedback(context, ChatColor.RED+"Game not running");
				return 0;
			}
			manager.stop();
			return 1;
		} else if (action.equals("hint")) {
			if (player == null) {
				cmdSendFeedback(context, ChatColor.RED+"Must be a player to give a hint");
				return -1;
			}

			if (!hsPlayer.isHider) {
				cmdSendFeedback(context, ChatColor.RED+"You must be a hider to give a hint");
				return -1;
			}

			String hint = StringArgumentType.getString(context, "argument");
			if (!hsPlayer.lateHint)
				cmdSendFeedback(context, ChatColor.GREEN+"Hint set to "+ChatColor.WHITE+"\"" + hint + "\"");
			hsPlayer.setHint(hint);
			return 1;
		}
		cmdSendFeedback(context, ChatColor.RED+"Unknown action \"" + action + "\"");
		return -1;
	}

}
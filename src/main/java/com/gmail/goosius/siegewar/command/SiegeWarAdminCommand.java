package com.gmail.goosius.siegewar.command;

import com.gmail.goosius.siegewar.Messaging;
import com.gmail.goosius.siegewar.SiegeController;
import com.gmail.goosius.siegewar.TownOccupationController;
import com.gmail.goosius.siegewar.enums.SiegeRemoveReason;
import com.gmail.goosius.siegewar.enums.SiegeWarPermissionNodes;
import com.gmail.goosius.siegewar.metadata.NationMetaDataController;
import com.gmail.goosius.siegewar.metadata.TownMetaDataController;
import com.gmail.goosius.siegewar.objects.BattleSession;
import com.gmail.goosius.siegewar.objects.Siege;
import com.gmail.goosius.siegewar.settings.Settings;
import com.gmail.goosius.siegewar.utils.SiegeWarBattleSessionUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarTownPeacefulnessUtil;
import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeMgmt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SiegeWarAdminCommand implements TabExecutor {

	private static final List<String> siegewaradminTabCompletes = Arrays.asList("battlesession","install","nation","reload","revoltimmunity","siege","siegeimmunity","town");
	private static final List<String> siegewaradminSiegeImmunityTabCompletes = Arrays.asList("town","nation","alltowns");
	private static final List<String> siegewaradminRevoltImmunityTabCompletes = Arrays.asList("town","nation","alltowns");
	private static final List<String> siegewaradminSiegeTabCompletes = Arrays.asList("setbalance","end","setplundered","setinvaded","remove");
	private static final List<String> siegewaradminTownTabCompletes = Arrays.asList("setpeaceful", "setoccupied");
	private static final List<String> siegewaradminNationTabCompletes = Arrays.asList("setplundergained","setplunderlost","settownsgained","settownslost");
	private static final List<String> siegewaradminBattleSessionTabCompletes = Arrays.asList("end","start");

	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

		switch (args[0].toLowerCase()) {
		case "siegeimmunity":
			if (args.length == 2)
				return NameUtil.filterByStart(siegewaradminSiegeImmunityTabCompletes, args[1]);
			
			if (args.length == 3) {
				switch (args[1].toLowerCase()) {
				case "town":
					return getTownyStartingWith(args[2], "t");
				case "nation":
					return getTownyStartingWith(args[2], "n");
				case "alltowns":
					return Arrays.asList("0","1","2","3","4","5","6","permanent");
				}
			}
			
			if (args.length == 4) {
				if (args[1].equalsIgnoreCase("town") || args[1].equalsIgnoreCase("nation"))
					return Arrays.asList("0","1","2","3","4","5","6","permanent");
			}
		case "revoltimmunity":
			if (args.length == 2)
				return NameUtil.filterByStart(siegewaradminRevoltImmunityTabCompletes, args[1]);

			if (args.length == 3) {
				switch (args[1].toLowerCase()) {
				case "town":
					return getTownyStartingWith(args[2], "t");
				case "nation":
					return getTownyStartingWith(args[2], "n");
				case "alltowns":
					return Arrays.asList("0","1","2","3","4","5","6","permanent");
				}
			}

			if (args.length == 4) {
				if (args[1].equalsIgnoreCase("town") || args[1].equalsIgnoreCase("nation"))
					return Arrays.asList("0","1","2","3","4","5","6","permanent");
			}
		case "siege":
			if (args.length == 2)
				return NameUtil.filterByStart(new ArrayList<>(SiegeController.getNamesOfSiegedTowns()), args[1]);

			if (args.length == 3)
				return NameUtil.filterByStart(siegewaradminSiegeTabCompletes, args[2]);

			if (args.length == 4 ) {
				if (args[2].equalsIgnoreCase("addcontrol") || args[2].equalsIgnoreCase("removecontrol"))
					return getTownyStartingWith(args[3], "r");
				if (args[2].equalsIgnoreCase("setplundered"))
					return Arrays.asList("true","false");
				if (args[2].equalsIgnoreCase("setinvaded"))
					return Arrays.asList("true","false");
			}
		case "town":
			if (args.length == 2)
				return getTownyStartingWith(args[1], "t");

			if (args.length == 3)
				return NameUtil.filterByStart(siegewaradminTownTabCompletes, args[2]);

			if (args.length == 4) {
				if (args[2].equalsIgnoreCase("setoccupied")) {
					return Arrays.asList("true","false");
				}
				if (args[2].equalsIgnoreCase("setpeaceful")) {
					return Arrays.asList("true","false");
				}
			}
		case "nation":
			if (args.length == 2)
				return getTownyStartingWith(args[1], "n");
			
			if (args.length == 3)
				return NameUtil.filterByStart(siegewaradminNationTabCompletes, args[2]);
		case "battlesession":
			if (args.length == 2)
				return NameUtil.filterByStart(siegewaradminBattleSessionTabCompletes, args[1]);
		default:
			if (args.length == 1)
				return NameUtil.filterByStart(siegewaradminTabCompletes, args[0]);
			else
				return Collections.emptyList();
		}
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		parseSiegeWarAdminCommand(sender, args);
		return true;
	}

	private void parseSiegeWarAdminCommand(CommandSender sender, String[] args) {
		/*
		 * Parse Command.
		 */
		if (args.length > 0) {
			if (sender instanceof Player && !((Player)sender).hasPermission(SiegeWarPermissionNodes.SIEGEWAR_COMMAND_SIEGEWARADMIN.getNode(args[0]))) {
				Messaging.sendErrorMsg(sender, Translatable.of("msg_err_command_disable"));
				return;
			}
			switch (args[0]) {
			case "reload":
				parseSiegeWarReloadCommand(sender);
				break;
			case "siegeimmunity":
				parseSiegeWarSiegeImmunityCommand(sender, StringMgmt.remFirstArg(args));
				break;
			case "revoltimmunity":
				parseSiegeWarRevoltImmunityCommand(sender, StringMgmt.remFirstArg(args));
				break;
			case "siege":
				parseSiegeWarSiegeCommand(sender, StringMgmt.remFirstArg(args));
				break;
			case "town":
				parseSiegeWarTownCommand(sender, StringMgmt.remFirstArg(args));
				break;
			case "nation":
				parseSiegeWarNationCommand(sender, StringMgmt.remFirstArg(args));
				break;
			case "install":
				parseInstallCommand(sender);
				break;
			case "battlesession":
				parseSiegeWarBattleSessionCommand(sender, StringMgmt.remFirstArg(args));
				break;

			/*
			 * Show help if no command found.
			 */
			default:
				showHelp(sender);
			}
		} else {
			if (sender instanceof Player
					&& !((Player)sender).hasPermission(SiegeWarPermissionNodes.SIEGEWAR_COMMAND_SIEGEWARADMIN.getNode())) {
				Messaging.sendErrorMsg(sender, Translatable.of("msg_err_command_disable"));
				return;
			}
			showHelp(sender);
		}
	}

	private void parseInstallCommand(CommandSender sender) {
		setupTownyPermsFile(sender);
		setupTownyConfigFile(sender);
		Messaging.sendMsg(sender, Translatable.of("msg.installation.complete"));
	}

	private void setupTownyPermsFile(CommandSender sender) {
		CommentedConfiguration file = TownyPerms.getTownyPermsFile();
		List<String> groupNodes = new ArrayList<>();
		String townpoints = "siegewar.town.siege.battle.points";
		String nationpoints = "siegewar.nation.siege.battle.points";

		// Add nodes to mayor rank.
		groupNodes = TownyPerms.getPermsOfGroup("towns.mayor");
		if (!groupNodes.contains("siegewar.town.siege.*"))
			groupNodes.add("siegewar.town.siege.*");
		if (!groupNodes.contains("siegewar.command.siegewar.town.*"))
			groupNodes.add("siegewar.command.siegewar.town.*");
		file.set("towns.mayor", groupNodes);
		
		// Add nodes to the town assistant rank.
		if (TownyPerms.mapHasGroup("towns.ranks.assistant")) {
			groupNodes = TownyPerms.getPermsOfGroup("towns.ranks.assistant");
			if (!groupNodes.contains("siegewar.command.siegewar.town.*"))
				groupNodes.add("siegewar.command.siegewar.town.*");
			file.set("towns.ranks.assistant", groupNodes);
		}

		// Create new ranks
		file.createSection("towns.ranks.guard");
		file.createSection("nations.ranks.private");
		file.createSection("nations.ranks.sergeant");
		file.createSection("nations.ranks.lieutenant");
		file.createSection("nations.ranks.captain");
		file.createSection("nations.ranks.major");
		file.createSection("nations.ranks.colonel");
		file.createSection("nations.ranks.general");

		// Populate town guard rank.
		groupNodes = TownyPerms.getPermsOfGroup("towns.ranks.guard");
		groupNodes.add(townpoints);
		file.set("towns.ranks.guard", groupNodes);
		
		// Populate nation ranks.
		groupNodes = TownyPerms.getPermsOfGroup("nations.ranks.private");
		groupNodes.add(nationpoints);
		groupNodes.add("towny.nation.siege.pay.grade.100");
		file.set("nations.ranks.private", groupNodes);

		groupNodes = TownyPerms.getPermsOfGroup("nations.ranks.sergeant");
		groupNodes.add(nationpoints);
		groupNodes.add("towny.nation.siege.pay.grade.150");
		file.set("nations.ranks.sergeant", groupNodes);

		groupNodes = TownyPerms.getPermsOfGroup("nations.ranks.lieutenant");
		groupNodes.add(nationpoints);
		groupNodes.add("towny.nation.siege.pay.grade.200");
		file.set("nations.ranks.lieutenant", groupNodes);

		groupNodes = TownyPerms.getPermsOfGroup("nations.ranks.captain");
		groupNodes.add(nationpoints);
		groupNodes.add("towny.nation.siege.pay.grade.250");
		file.set("nations.ranks.captain", groupNodes);

		groupNodes = TownyPerms.getPermsOfGroup("nations.ranks.major");
		groupNodes.add(nationpoints);
		groupNodes.add("towny.nation.siege.pay.grade.300");
		file.set("nations.ranks.major", groupNodes);

		groupNodes = TownyPerms.getPermsOfGroup("nations.ranks.colonel");
		groupNodes.add(nationpoints);
		groupNodes.add("towny.nation.siege.pay.grade.400");
		file.set("nations.ranks.colonel", groupNodes);

		groupNodes = TownyPerms.getPermsOfGroup("nations.ranks.general");
		groupNodes.add("siegewar.nation.siege.*");
		groupNodes.add("towny.command.nation.rank.private");
		groupNodes.add("towny.command.nation.rank.sergeant");
		groupNodes.add("towny.command.nation.rank.lieutenant");
		groupNodes.add("towny.command.nation.rank.captain");
		groupNodes.add("towny.command.nation.rank.major");
		groupNodes.add("towny.command.nation.rank.colonel");
		groupNodes.add("towny.nation.siege.pay.grade.500");
		file.set("nations.ranks.general", groupNodes);
	
		// Add nodes to king rank.
		groupNodes = TownyPerms.getPermsOfGroup("nations.king");
		if (!groupNodes.contains("siegewar.nation.siege.*"))
			groupNodes.add("siegewar.nation.siege.*");
		if (!groupNodes.contains("siegewar.command.siegewar.nation.*"))
			groupNodes.add("siegewar.command.siegewar.nation.*");
		file.set("nations.king", groupNodes);
		
		// Add nodes to the nation assistant rank.
		if (TownyPerms.mapHasGroup("nations.ranks.assistant")) {
			groupNodes = TownyPerms.getPermsOfGroup("nations.ranks.assistant");
			if (!groupNodes.contains("siegewar.command.siegewar.nation.*"))
				groupNodes.add("siegewar.command.siegewar.nation.*");
			file.set("nations.ranks.assistant", groupNodes);
		}
		file.save();
		Messaging.sendMsg(sender, Translatable.of("msg.townyperms.installation.complete"));
	}

	private void setupTownyConfigFile(CommandSender sender) {
		CommentedConfiguration file = TownySettings.getConfig();
		file.set("economy.bankruptcy.enabled", "true");
		file.set("town_ruining.town_ruins.enabled", "true");
		file.set("town_ruining.town_ruins.min_duration_hours", "24");
		file.save();		
		Messaging.sendMsg(sender, Translatable.of("msg.townyconfig.installation.complete"));
	}

	private void showHelp(CommandSender sender) {
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/siegewaradmin"));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "reload", Translatable.of("admin_help_1").forLocale(sender)));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "installperms", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siegeimmunity town [town_name] [hours|permanent]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siegeimmunity nation [nation_name] [hours|permanent]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siegeimmunity alltowns [hours|permanent]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "revoltimmunity town [town_name] [hours]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "revoltimmunity nation [nation_name] [hours]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "revoltimmunity alltowns [hours]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siege [town_name] setbalance [points]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siege [town_name] end", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siege [town_name] setplundered [true/false]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siege [town_name] remove", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "town [town_name] setpeaceful [true/false]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "town [town_name] setoccupied [true/false]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "nation [nation_name] setplundergained [amount]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "nation [nation_name] setplunderlost [amount]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "nation [nation_name] settownsgained [amount]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "nation [nation_name] settownslost [amount]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "battlesession [start/end]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siegeduration addhours [1,2,3,4,5...]", "Add a number of hours to every siege."));
	}

	private void showBattleSessionHelp(CommandSender sender) {
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/swa battlesession"));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "battlesession [start/end]", ""));
	}
	
	private void showSiegeImmunityHelp(CommandSender sender) {
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/swa siegeimmunity"));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siegeimmunity town [town_name] [hours]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siegeimmunity nation [nation_name] [hours]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siegeimmunity alltowns [hours]", ""));
	}

	private void showRevoltImmunityHelp(CommandSender sender) {
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/swa revoltimmunity"));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "revoltimmunity town [town_name] [hours]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "revoltimmunity nation [nation_name] [hours]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "revoltimmunity alltowns [hours]", ""));
	}

	private void showSiegeHelp(CommandSender sender) {
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/swa siege"));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siege [town_name] setbalance [points]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siege [town_name] end", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siege [town_name] setplundered [true/false]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "siege [town_name] remove", ""));
	}

	private void showTownHelp(CommandSender sender) {
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/swa town"));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "town [town_name] setpeaceful [true/false]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "town [town_name] setoccupied [true/false]", ""));
	}

	private void showNationHelp(CommandSender sender) {
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/swa nation"));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "nation [nation_name] setplundergained [amount]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "nation [nation_name] setplunderlost [amount]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "nation [nation_name] settownsgained [amount]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/swa", "nation [nation_name] settownslost [amount]", ""));
	}

	private void parseSiegeWarReloadCommand(CommandSender sender) {
		if (Settings.loadSettingsAndLang()) {
			Messaging.sendMsg(sender, Translatable.of("config_and_lang_file_reloaded_successfully"));
			return;
		}
		
		Messaging.sendErrorMsg(sender, Translatable.of("config_and_lang_file_could_not_be_loaded"));
	}

	private void parseSiegeWarBattleSessionCommand(CommandSender sender, String[] args) {
		if (args.length == 0) {
			showBattleSessionHelp(sender);
			return;
		}

		BattleSession battleSession = BattleSession.getBattleSession();

		if (args[0].equalsIgnoreCase("start")) {
			if (battleSession.isActive()) {
				Messaging.sendMsg(sender, Translatable.of("msg_err_battle_session_active"));
				return;
			}
			SiegeWarBattleSessionUtil.startBattleSession();
			Messaging.sendMsg(sender, Translatable.of("msg_battle_session_force_start"));
		} else if (args[0].equalsIgnoreCase("end")) {
			if (!battleSession.isActive()) {
				Messaging.sendMsg(sender, Translatable.of("msg_err_battle_session_inactive"));
				return;
			}
			SiegeWarBattleSessionUtil.endBattleSession();
			Messaging.sendMsg(sender, Translatable.of("msg_battle_session_force_end"));
		} else {
			showBattleSessionHelp(sender);
		}
	}

	private void parseSiegeWarSiegeImmunityCommand(CommandSender sender, String[] args) {
		if (args.length < 2) {
			showSiegeImmunityHelp(sender);
			return;
		}

		try {
			if (args[0].equalsIgnoreCase("alltowns") && !args[1].equalsIgnoreCase("permanent")) {
				Integer.parseInt(args[1]);
			} else if (!args[0].equalsIgnoreCase("alltowns") && !args[2].equalsIgnoreCase("permanent")){
				Integer.parseInt(args[2]);
			}
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			Messaging.sendMsg(sender, Translatable.of("msg_error_must_be_num_or_permanent"));
			showSiegeImmunityHelp(sender);
			return;
		}

		String timeDuration;
		if (args.length >= 3 && args[0].equalsIgnoreCase("town")) {
			//town {townname} {hours}
			Town town = TownyUniverse.getInstance().getTown(args[1]);
			if (town == null) {
				Messaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", args[1]));
				return;
			}
			if (args[2].equalsIgnoreCase("permanent")) {
				TownMetaDataController.setSiegeImmunityEndTime(town, -1l);
				timeDuration = Translatable.of("msg_permanent").forLocale(sender);
			} else {
				TownMetaDataController.setSiegeImmunityEndTime(town, System.currentTimeMillis() + (long)(Long.parseLong(args[2]) * TimeMgmt.ONE_HOUR_IN_MILLIS));
				timeDuration = Long.parseLong(args[2]) + Translatable.of("msg_hours").forLocale(sender);
			}
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_set_siege_immunities_town", town.getName(), timeDuration));
			Messaging.sendMsg(sender, Translatable.of("msg_set_siege_immunities_town", town.getName(), timeDuration));
		} else if (args.length >= 3 && args[0].equalsIgnoreCase("nation")) {
			//nation {nationname} {hours}
			Nation nation = TownyUniverse.getInstance().getNation(args[1]);
			if (nation == null) {
				Messaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", args[1]));
				return;
			}
			long endTime;
			if (args[2].equalsIgnoreCase("permanent")) {
				endTime = -1l;
				timeDuration = Translatable.of("msg_permanent").forLocale(sender);
			} else {
				endTime = System.currentTimeMillis() + (long)(Long.parseLong(args[2]) * TimeMgmt.ONE_HOUR_IN_MILLIS);
				timeDuration = Long.parseLong(args[2]) + Translatable.of("msg_hours").forLocale(sender);
			}
			for (Town town : nation.getTowns()) {
				TownMetaDataController.setSiegeImmunityEndTime(town, endTime);
			}
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_set_siege_immunities_nation", nation.getName(), timeDuration));
			Messaging.sendMsg(sender, Translatable.of("msg_set_siege_immunities_nation", nation.getName(), timeDuration));

		} else if (args[0].equalsIgnoreCase("alltowns")) {
			//all towns
			long endTime;
			if (args[1].equalsIgnoreCase("permanent")) {
				endTime = -1l;
				timeDuration = Translatable.of("msg_permanent").forLocale(sender);
			} else {
				endTime = System.currentTimeMillis() + (long)(Long.parseLong(args[1]) * TimeMgmt.ONE_HOUR_IN_MILLIS);
				timeDuration = Long.parseLong(args[1]) + Translatable.of("msg_hours").forLocale(sender);
			}
			for (Town town : new ArrayList<>(TownyUniverse.getInstance().getTowns()))  {
				TownMetaDataController.setSiegeImmunityEndTime(town, endTime);
			}
			Messaging.sendGlobalMessage(Translatable.of("msg_set_siege_immunities_all", timeDuration));

		} else {
			showSiegeImmunityHelp(sender);
		}
	}

	private void parseSiegeWarRevoltImmunityCommand(CommandSender sender, String[] args) {
		if (args.length < 2) {
			showRevoltImmunityHelp(sender);
			return;
		}

		try {
			if (args[0].equalsIgnoreCase("alltowns") && !args[1].equalsIgnoreCase("permanent"))
				Integer.parseInt(args[1]);
			else if (!args[0].equalsIgnoreCase("alltowns") && !args[2].equalsIgnoreCase("permanent"))
				Integer.parseInt(args[2]);
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			Messaging.sendMsg(sender, Translatable.of("msg_error_must_be_num"));
			showRevoltImmunityHelp(sender);
			return;
		}

		String timeDuration;
		if (args.length >= 3 && args[0].equalsIgnoreCase("town")) {
			//town {townname} {hours}
			Town town = TownyUniverse.getInstance().getTown(args[1]);
			if (town == null) {
				Messaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", args[1]));
				return;
			}
			
			if (args[2].equalsIgnoreCase("permanent")) {
				TownMetaDataController.setRevoltImmunityEndTime(town, -1L);
				timeDuration = Translatable.of("msg_permanent").forLocale(sender);
			} else  {
				TownMetaDataController.setRevoltImmunityEndTime(town, System.currentTimeMillis() + (long)(Long.parseLong(args[2]) * TimeMgmt.ONE_HOUR_IN_MILLIS));
				timeDuration = Long.parseLong(args[2]) + Translatable.of("msg_hours").forLocale(sender);
			}
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_set_revolt_immunities_town", town, timeDuration));
			Messaging.sendMsg(sender, Translatable.of("msg_set_revolt_immunities_town", town, timeDuration));

		} else if (args.length >= 3 && args[0].equalsIgnoreCase("nation")) {
			//nation {nationname} {hours}
			Nation nation = TownyUniverse.getInstance().getNation(args[1]);
			if (nation == null) {
				Messaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", args[1]));
				return;
			}
			long endTime;
			if (args[2].equalsIgnoreCase("permanent")) {
				endTime = -1l;
				timeDuration = Translatable.of("msg_permanent").forLocale(sender);
			} else {
				endTime = System.currentTimeMillis() + (long)(Long.parseLong(args[2]) * TimeMgmt.ONE_HOUR_IN_MILLIS);
				timeDuration = Long.parseLong(args[2]) + Translatable.of("msg_hours").forLocale(sender);
			}
			
			for (Town town : nation.getTowns()) {
				TownMetaDataController.setRevoltImmunityEndTime(town, endTime);
			}
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_set_revolt_immunities_nation", nation, timeDuration));
			Messaging.sendMsg(sender, Translatable.of("msg_set_revolt_immunities_nation", nation, timeDuration));

		} else if (args[0].equalsIgnoreCase("alltowns")) {
			//all towns
			long endTime;
			if (args[1].equalsIgnoreCase("permanent")) {
				endTime = -1l;
				timeDuration = Translatable.of("msg_permanent").forLocale(sender);
			} else {
				endTime = System.currentTimeMillis() + (long)(Long.parseLong(args[1]) * TimeMgmt.ONE_HOUR_IN_MILLIS);
				timeDuration = Long.parseLong(args[1]) + Translatable.of("msg_hours").forLocale(sender);
			}
			for (Town town : new ArrayList<>(TownyUniverse.getInstance().getTowns()))  {
				TownMetaDataController.setRevoltImmunityEndTime(town, endTime);
			}
			Messaging.sendGlobalMessage(Translatable.of("msg_set_revolt_immunities_all", timeDuration));

		} else {
			showRevoltImmunityHelp(sender);
		}
	}

	private void parseSiegeWarSiegeCommand(CommandSender sender, String[] args) {
		if (args.length >= 2) {
			Town town = TownyUniverse.getInstance().getTown(args[0]);
			if (town == null) {
				Messaging.sendErrorMsg(sender, Translatable.of("msg_err_town_not_registered", args[0]));
				return;
			}
			if (!SiegeController.hasSiege(town)) {
				Messaging.sendErrorMsg(sender, Translatable.of("msg_err_siege_war_no_siege_on_target_town", town.getName()));
				return;
			}
			List<String> commandsNotAllowedOnActiveSieges = Arrays.asList("setplundered","setinvaded");

			if (SiegeController.hasActiveSiege(town) && commandsNotAllowedOnActiveSieges.contains(args[1].toLowerCase(Locale.ROOT))) {
				Messaging.sendErrorMsg(sender, Translatable.of("msg_err_command_not_allowed_on_active_siege", town.getName()));
				return;
			}
			Siege siege = SiegeController.getSiege(town);

			switch(args[1].toLowerCase()) {
				case "setbalance":
					if (args.length < 3) {
						showSiegeHelp(sender);
					}
					try {
						Integer.parseInt(args[2]);
					} catch (NumberFormatException e) {
						Messaging.sendErrorMsg(sender, Translatable.of("msg_error_must_be_num"));
						return;
					}

					int newPoints = Integer.parseInt(args[2]);
					siege.setSiegeBalance(newPoints);
					SiegeController.saveSiege(siege);
					Messaging.sendMsg(sender, Translatable.of("msg_swa_set_siege_balance_success", newPoints, town.getName()));
					return;

				case "end":
					SiegeController.endSiegeWithTimedWin(siege);
					return;
				case "setplundered":
					boolean plundered = Boolean.parseBoolean(args[2]);
					siege.setTownPlundered(plundered);
					SiegeController.saveSiege(siege);
					Messaging.sendMsg(sender, Translatable.of("msg_swa_set_plunder_success", Boolean.toString(plundered).toUpperCase(), town.getName()));
					return;
				case "setinvaded":
					boolean invaded = Boolean.parseBoolean(args[2]);
					if(invaded) {
						siege.setTownInvaded(true);
						TownOccupationController.setTownOccupation(town, (Nation)siege.getAttacker());
					} else {
						siege.setTownInvaded(false);
						TownOccupationController.removeTownOccupation(town);
					}
					SiegeController.saveSiege(siege);
					Messaging.sendMsg(sender, Translatable.of("msg_swa_set_invade_success", Boolean.toString(invaded).toUpperCase(), town.getName()));
					return;
				case "remove":
					//Remove siege from system
					SiegeController.removeSiege(siege, SiegeRemoveReason.ADMIN_COMMAND);
					Messaging.sendGlobalMessage(Translatable.of("msg_swa_remove_siege", siege.getTown().getName()));
					Messaging.sendMsg(sender, Translatable.of("msg_swa_remove_siege_success"));
					return;
			}

		} else
			showSiegeHelp(sender);
	}

	private void parseSiegeWarTownCommand(CommandSender sender, String[] args) {
		if (args.length == 3) {
			Town town = TownyUniverse.getInstance().getTown(args[0]);
			if (town == null) {
				Messaging.sendErrorMsg(sender, Translatable.of("msg_err_town_not_registered", args[0]));
				return;
			}
			switch(args[1].toLowerCase()) {
				case "setpeaceful":
					//set peaceful flag
					boolean peaceful = Boolean.parseBoolean(args[2]);
					SiegeWarTownPeacefulnessUtil.setTownPeacefulness(town, peaceful);
					SiegeWarTownPeacefulnessUtil.setDesiredTownPeacefulness(town, peaceful);
					SiegeWarTownPeacefulnessUtil.setTownPeacefulnessChangeCountdownDays(town, 0);
					//Send message
					Messaging.sendMsg(sender, Translatable.of("msg_swa_town_peacefulness_change_success", town.getName(), Boolean.toString(peaceful)));
					break;
				case "setoccupied":
					boolean occupied = Boolean.parseBoolean(args[2]);
					if(occupied) {
						if(town.hasNation()) {
							TownOccupationController.setTownOccupation(town, town.getNationOrNull());
							Messaging.sendMsg(sender, Translatable.of("msg_swa_town_occupation_change_success", town.getName(), occupied));
							break;
						} else {
							Messaging.sendErrorMsg(sender, Translatable.of("msg_err_cannot_set_occupation_without_nation", town.getName()));
							break;
						}
					} else {
						TownOccupationController.removeTownOccupation(town);
						Messaging.sendMsg(sender, Translatable.of("msg_swa_town_occupation_change_success", town.getName(), occupied));
						break;
					}
				default:
					showSiegeHelp(sender);
			}
		} else
			showTownHelp(sender);
	}

	private void parseSiegeWarNationCommand(CommandSender sender, String[] args) {
		if (args.length >= 3) {
			Nation nation = TownyUniverse.getInstance().getNation(args[0]);
			if (nation == null) {
				Messaging.sendErrorMsg(sender, Translatable.of("msg_err_nation_not_registered", args[0]));
				return;
			}

			int amount = 0;
			try {
				amount = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				Messaging.sendMsg(sender, Translatable.of("msg_error_must_be_num"));
				showNationHelp(sender);
				return;
			}

			switch(args[1].toLowerCase()) {
				case "setplundergained":
					NationMetaDataController.setTotalPlunderGained(nation, amount);
					Messaging.sendMsg(sender, Translatable.of("msg_swa_set_plunder_gained_success", amount, nation.getName()));
					return;
				case "setplunderlost":
					NationMetaDataController.setTotalPlunderLost(nation, amount);
					Messaging.sendMsg(sender, Translatable.of("msg_swa_set_plunder_lost_success", amount, nation.getName()));
					return;
				case "settownsgained":
					NationMetaDataController.setTotalTownsGained(nation, amount);
					Messaging.sendMsg(sender, Translatable.of("msg_swa_set_towns_gained_success", amount, nation.getName()));
					return;
				case "settownslost":
					NationMetaDataController.setTotalTownsLost(nation, amount);
					Messaging.sendMsg(sender, Translatable.of("msg_swa_set_towns_lost_success", amount, nation.getName()));
					return;
				default:
					showNationHelp(sender);
					return;
			}
		} else
			showNationHelp(sender);
	}

	/**
	 * Returns a List<String> containing strings of resident, town, and/or nation names that match with arg.
	 * Can check for multiple types, for example "rt" would check for residents and towns but not nations or worlds.
	 *
	 * @param arg the string to match with the chosen type
	 * @param type the type of Towny object to check for, can be r(esident), t(own), n(ation), w(orld), or any combination of those to check
	 * @return Matches for the arg with the chosen type
	 */
	static List<String> getTownyStartingWith(String arg, String type) {

		List<String> matches = new ArrayList<>();
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (type.contains("r")) {
			matches.addAll(townyUniverse.getResidentsTrie().getStringsFromKey(arg));
		}

		if (type.contains("t")) {
			matches.addAll(townyUniverse.getTownsTrie().getStringsFromKey(arg));
		}

		if (type.contains("n")) {
			matches.addAll(townyUniverse.getNationsTrie().getStringsFromKey(arg));
		}

		if (type.contains("w")) { // There aren't many worlds so check even if arg is empty
			matches.addAll(NameUtil.filterByStart(NameUtil.getNames(townyUniverse.getWorldMap().values()), arg));
		}

		return matches;
	}
}


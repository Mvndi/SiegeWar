package com.gmail.goosius.siegewar.playeractions;

import com.gmail.goosius.siegewar.Messaging;
import com.gmail.goosius.siegewar.SiegeWar;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.event.town.TownMayorChangeEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.BukkitTools;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TakeFullControl {
	public static boolean isAvailable(Player player, Town town) {
		Resident leader = TownyUniverse.getInstance().getResident(player.getUniqueId());
		int days = SiegeWarSettings.getTakeFullControlInactiveDays();
		return days > 0 && player.hasPermission("siegewar.command.siegewar.takefullcontrol")
			&& town != null && town.hasNation() && leader != null && leader.isKing()
			&& town.getNationOrNull().isKing(leader)
			&& town.getMayor() != null && !town.getMayor().isNPC() && !town.getMayor().isOnline()
			&& town.getMayor().getLastOnline() > 0
			&& System.currentTimeMillis() - town.getMayor().getLastOnline() >= TimeUnit.DAYS.toMillis(days);
	}

	public static void notifyOnLogin(Player player) {
		if (!player.isOnline())
			return;
		Resident leader = TownyUniverse.getInstance().getResident(player.getUniqueId());
		if (leader == null || !leader.isKing())
			return;
		List<Town> towns = leader.getTownOrNull().getNationOrNull().getTowns().stream()
			.filter(town -> isAvailable(player, town)).collect(Collectors.toList());
		if (towns.isEmpty())
			return;
		player.playSound(player.getLocation(), "minecraft:block.note_block.pling", 1.0F, 1.0F);
		Messaging.sendMsg(player, Translatable.of("sw_take_full_control_login", towns.size()));
		for (Town town : towns)
			player.sendMessage(TownyComponents.miniMessage(Translatable.of("sw_take_full_control_login_town", town.getName()).forLocale(player))
				.clickEvent(ClickEvent.runCommand("/sw takefullcontrol " + town.getName())));
	}

	private static boolean isCandidate(Town town, Resident resident) {
		return resident != null && town.hasResident(resident) && !resident.isNPC() && !resident.equals(town.getMayor());
	}

	public static List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length < 2 || !(sender instanceof Player player))
			return Collections.emptyList();
		if (args.length == 2)
			return NameUtil.filterByStart(TownyUniverse.getInstance().getTowns().stream()
				.filter(town -> isAvailable(player, town)).map(Town::getName).collect(Collectors.toList()), args[1]);
		Town town = TownyUniverse.getInstance().getTown(args[1]);
		if (args.length == 3 && isAvailable(player, town))
			return NameUtil.filterByStart(town.getResidents().stream().filter(resident -> isCandidate(town, resident))
				.map(Resident::getName).collect(Collectors.toList()), args[2]);
		return Collections.emptyList();
	}

	public static void request(Player player, String[] args) {
		try {
			if (args.length < 1 || args.length > 2) {
				Messaging.sendMsg(player, "/sw takefullcontrol <town> [new-mayor]");
				return;
			}
			Town town = TownyUniverse.getInstance().getTown(args[0]);
			if (town == null)
				throw new TownyException(Translatable.of("msg_err_town_not_registered", args[0]));
			checkAvailable(player, town);
			if (args.length == 1) {
				if (player.isConversing()) {
					Messaging.sendErrorMsg(player, Translatable.of("sw_take_full_control_already_prompted"));
					return;
				}
				new ConversationFactory(SiegeWar.getSiegeWar())
					.withPrefix(context -> Translation.of("siegewar_plugin_prefix") + Colors.White)
					.withLocalEcho(false)
					.withEscapeSequence("cancel")
					.withFirstPrompt(new StringPrompt() {
						@Override
						public String getPromptText(ConversationContext context) {
							return Translatable.of("sw_take_full_control_choose", town.getName()).forLocale(player);
						}

						@Override
						public Prompt acceptInput(ConversationContext context, String input) {
							if (input != null)
								SiegeWar.getSiegeWar().getScheduler().runLater(player, () -> {
									if (player.isOnline())
										request(player, new String[]{town.getName(), input.trim()});
								}, 1L);
							return Prompt.END_OF_CONVERSATION;
						}
					}).buildConversation(player).begin();
				return;
			}
			Resident replacement = TownyUniverse.getInstance().getResident(args[1]);
			checkCandidate(town, replacement);
			Resident oldMayor = town.getMayor();
			Messaging.sendMsg(player, Translatable.of("sw_take_full_control_confirm", oldMayor.getName(), replacement.getName(), town.getName()));
			Confirmation.runOnAccept(() -> {
				try {
					// Recheck eligibility: the mayor or nation may have changed during confirmation.
					checkAvailable(player, town);
					checkCandidate(town, replacement);
					if (!oldMayor.equals(town.getMayor()) || TownyUniverse.getInstance().getTown(town.getUUID()) != town)
						throw new TownyException(Translatable.of("msg_err_command_disable"));
					BukkitTools.ifCancelledThenThrow(new TownMayorChangeEvent(player, oldMayor, replacement));
					town.setMayor(replacement);
					Towny.getPlugin().deleteCache(oldMayor);
					Towny.getPlugin().deleteCache(replacement);
					town.save();
					Translatable message = Translatable.of("sw_take_full_control_success", player.getName(), replacement.getName(), town.getName());
					TownyMessaging.sendPrefixedNationMessage(town.getNationOrNull(), message);
				} catch (TownyException e) {
					Messaging.sendErrorMsg(player, e.getMessage(player));
				}
			}).sendTo(player);
		} catch (TownyException e) {
			Messaging.sendErrorMsg(player, e.getMessage(player));
		}
	}

	private static void checkAvailable(Player player, Town town) throws TownyException {
		if (!isAvailable(player, town))
			throw new TownyException(Translatable.of("sw_take_full_control_unavailable", SiegeWarSettings.getTakeFullControlInactiveDays()));
	}

	private static void checkCandidate(Town town, Resident resident) throws TownyException {
		if (resident == null || !town.hasResident(resident))
			throw new TownyException(Translatable.of("sw_take_full_control_not_town_resident", town.getName()));
		if (!isCandidate(town, resident))
			throw new TownyException(Translatable.of("sw_take_full_control_invalid_candidate"));
	}
}

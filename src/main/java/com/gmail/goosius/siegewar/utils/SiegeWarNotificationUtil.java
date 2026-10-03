package com.gmail.goosius.siegewar.utils;

import com.gmail.goosius.siegewar.Messaging;
import com.gmail.goosius.siegewar.SiegeController;
import com.gmail.goosius.siegewar.SiegeWar;
import com.gmail.goosius.siegewar.TownOccupationController;
import com.gmail.goosius.siegewar.metadata.TownMetaDataController;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.gmail.goosius.siegewar.objects.Siege;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;

import com.palmergames.util.TimeMgmt;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SiegeWarNotificationUtil {

	public static void notifyTownFinancesOnLogin(Player player) {
		if (!player.isOnline() || !SiegeWarSettings.getWarSiegeEnabled() || !TownyEconomyHandler.isActive())
			return;
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		Town town = resident == null ? null : resident.getTownOrNull();
		if (town == null || town.isRuined())
			return;
		boolean activeSiege = SiegeController.hasActiveSiege(town);
		boolean debt = SiegeWarSettings.isPlunderPaidOutOverDays() && TownMetaDataController.hasPlunderDebt(town);
		if (!activeSiege && !debt)
			return;
		String sound = SiegeWarSettings.getSiegeLoginWarningSound();
		if (!sound.isEmpty())
			player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
		double balance = town.getAccount().getHoldingBalance();
		double plunder = debt ? TownMetaDataController.getDailyPlunderDebt(town) : 0;
		double reserve = 0;
		if (activeSiege && !SiegeController.getSiege(town).isTownPlundered()) {
			double potentialPlunder = SiegeWarSettings.getWarSiegePlunderAmountPerPlot()
				* town.getNumTownBlocks() * SiegeWarMoneyUtil.getMoneyMultiplier(town);
			if (SiegeWarSettings.isPlunderPaidOutOverDays())
				plunder = (int) potentialPlunder / Math.max(1, SiegeWarSettings.plunderDays());
			else
				reserve = potentialPlunder;
		}
		double taxes = TownySettings.getTownUpkeepCost(town);
		Nation nation = town.getNationOrNull();
		if (TownySettings.isTaxingDaily() && town.hasUpkeep() && nation != null
			&& (!town.isCapital() || TownySettings.doCapitalsPayNationTax())) {
			double nationTax = nation.isTaxPercentage()
				? Math.min(Math.max(0, balance) * nation.getTaxes() / 100, nation.getMaxPercentTaxAmount())
				: nation.getTaxes();
			if (town.isConquered() && nation.getTaxes() != 0)
				nationTax += nation.getConqueredTax();
			taxes += Math.max(0, nationTax);
		}
		if (SiegeWarSettings.getMaxOccupationTaxPerPlot() > 0)
			taxes += Math.max(0, TownOccupationController.getNationOccupationTax(town));
		if (activeSiege)
			Messaging.sendErrorMsg(player, Translatable.of("sw_login_town_under_siege", town.getName()));
		double dailyCost = Math.max(0, taxes) + Math.max(0, plunder);
		if (dailyCost > 0) {
			long days = (long) Math.floor(Math.max(0, balance - reserve) / dailyCost);
			Messaging.sendMsg(player, Translatable.of("sw_login_siege_finances", days,
				TownyEconomyHandler.getFormattedBalance(dailyCost), TownyEconomyHandler.getFormattedBalance(reserve)));
		} else {
			Messaging.sendMsg(player, Translatable.of("sw_login_siege_finances_no_daily_cost",
				TownyEconomyHandler.getFormattedBalance(reserve)));
		}
	}

	/**
	 * This is a record of which players have received proximity siege zone warnings
	 */
	private static Map<Player, Set<Siege>> siegeZoneProximityWarningsReceivedMap = new HashMap<>();

	/**
	 * Send all siegezone proximity warnings
	 * 
	 * Each player in an active siegezone gets a warning
	 * 
	 * A player is not warned if they are already on the warningsReceived map
	 * The warningsReceived map is cleared every hour
	 * 
	 */
	public static void sendSiegeZoneProximityWarnings() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			sendSiegeZoneProximityWarning(player);
		}
	}

	public static void sendSiegeZoneProximityWarning(Player player) {
		Siege activeSiegeAtPlayerLocation = SiegeController.getActiveSiegeAtLocation(player);
		if (activeSiegeAtPlayerLocation != null) {
			sendSiegeZoneProximityWarning(player, activeSiegeAtPlayerLocation);
		}
	}

	public static void sendSiegeZoneProximityWarning(Player player, @NotNull Siege activeSiegeAtPlayerLocation) {
		//Check if player is on warnings-received map
		if (!siegeZoneProximityWarningsReceivedMap.containsKey(player)) {
			//Player is not on the warnings-received map
			Set<Siege> warningsReceivedSet = new HashSet<>();
			warningsReceivedSet.add(activeSiegeAtPlayerLocation);
			siegeZoneProximityWarningsReceivedMap.put(player, warningsReceivedSet);
			Messaging.sendErrorMsg(player, Translatable.of("msg_siege_zone_proximity_warning"));
		} else {
			//Player is already on the warnings-receieved map
			Set<Siege> warningsReceivedSet = siegeZoneProximityWarningsReceivedMap.get(player);
			if (!warningsReceivedSet.contains(activeSiegeAtPlayerLocation)) {
				//Player has not received a warning for this siege
				warningsReceivedSet.add(activeSiegeAtPlayerLocation);
				Messaging.sendErrorMsg(player, Translatable.of("msg_siege_zone_proximity_warning"));
			}
		}
	}

	public static void clearSiegeZoneProximityWarningsReceived() {
		siegeZoneProximityWarningsReceivedMap.clear();
	}

	public static void informSiegeParticipants(Siege siege, Translatable... message) {

		try {
			//Build list of who to inform
			Nation nation;
			Set<Nation> nationsToInform = new HashSet<>();
			Set<Town> townsToInform= new HashSet<>();

			//Attackers
			if(siege.getAttacker() instanceof Nation) {
				//Attacker is a nation
				nation = (Nation)siege.getAttacker();
				nationsToInform.add(nation);
				nationsToInform.addAll(nation.getMutualAllies());
			} else if (((Town)siege.getAttacker()).hasNation()) {
				//Attacker is a nation town
				nation = ((Town)siege.getAttacker()).getNation();
				nationsToInform.add(nation);
				nationsToInform.addAll(nation.getMutualAllies());
			} else {
				//Attacker is a non-nation town
				townsToInform.add((Town)siege.getAttacker());
			}

			//Defenders
			if(siege.getDefender() instanceof Nation) {
				//Defender is a nation
				nation = (Nation)siege.getDefender();
				nationsToInform.add(nation);
				nationsToInform.addAll(nation.getMutualAllies());
			} else if (((Town)siege.getDefender()).hasNation()) {
				//Defender is a nation town
				nation = ((Town)siege.getDefender()).getNation();
				nationsToInform.add(nation);
				nationsToInform.addAll(nation.getMutualAllies());
			} else {
				//Defender is a non-nation town
				townsToInform.add((Town)siege.getDefender());
			}

			//Inform required towns and nations
			for(Nation nationToInform: nationsToInform) {
				for (Translatable line : message) 
					if (line != null)
						TownyMessaging.sendPrefixedNationMessage(nationToInform, line);
			}
			for(Town townToInform: townsToInform) {
				for (Translatable line : message)
					if (line != null)
						TownyMessaging.sendPrefixedTownMessage(townToInform, line);
			}

		} catch (Exception e) {
			SiegeWar.severe("Problem informing siege participants");
			SiegeWar.severe("Message : " + message);
			e.printStackTrace();
		}
	}

	public static void notifyPlayerOfBattleSessionChatRestriction(Player player, String channelName) {
		String formattedDisableTime = TimeMgmt.getFormattedTimeValue(SiegeWarSettings.getToxicityReductionChatRestorationAfterBattleSessionMillis());
		String langStringKey = "msg_err_no_"+ channelName + "_chat_in_battle_session";
		Translatable message = Translatable.of(langStringKey, formattedDisableTime);
		String discordLink = SiegeWarSettings.getToxicityReductionServerDiscordLink();
		if (!discordLink.isEmpty())
			message.append(Translatable.of("msg_can_also_chat_in_discord", discordLink));
		Messaging.sendMsg(player, message);

	}
}

package com.gmail.goosius.siegewar.utils;


import com.gmail.goosius.siegewar.Messaging;
import com.gmail.goosius.siegewar.TownOccupationController;
import com.gmail.goosius.siegewar.enums.SiegeWarPermissionNodes;
import com.gmail.goosius.siegewar.metadata.TownMetaDataController;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.gmail.goosius.siegewar.settings.Translation;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeTools;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class TownPeacefulnessUtil {

	/**
	 * This method adjust the peacefulness counters of all towns, where required
	 */
	public static void updateTownPeacefulnessCounters() {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		List<Town> towns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only adjust counter for this town if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDataSource().hasTown(town.getName()) 
				&& !town.isRuined()
				&& town.isNeutral() != TownMetaDataController.getDesiredPeacefulnessSetting(town))
				updateTownPeacefulnessCounters(town);
		}
	}

	public static void updateTownPeacefulnessCounters(Town town) {
		String message;

		int days = TownMetaDataController.getPeacefulnessChangeConfirmationCounterDays(town); 
		if (days > 1) {
			TownMetaDataController.setPeacefulnessChangeDays(town, --days);
			return;
		}
		TownMetaDataController.setPeacefulnessChangeDays(town, 0);
		town.setNeutral(!town.isNeutral());

		if (town.isNeutral() && !SiegeWarSettings.getWarCommonPeacefulTownsAllowedToTogglePVP()) 
			SiegeWarTownUtil.disableTownPVP(town);	

		if (SiegeWarSettings.getWarSiegeEnabled()) {
			if (town.isNeutral()) {
				message = Translation.of("msg_town_became_peaceful", town.getFormattedName());

				//If town is occupied, record the occupier
				if(TownOccupationController.isTownOccupied(town)) {
					TownMetaDataController.setPrePeacefulOccupierUUID(town, TownOccupationController.getTownOccupier(town).getUUID().toString());
				}
			} else {
				message = Translation.of("msg_town_became_non_peaceful", town.getFormattedName());

				/*
				 * If the town was occupied before turning peaceful,
				 * return it to the previous occupier.
				 * If the previous occupier is now the town's home nation, do not re-occupy.
				 */
				try {
					String prePeacefulOccupierUUID = TownMetaDataController.getPrePeacefulOccupierUUID(town);
					if(prePeacefulOccupierUUID != null) {
						Nation prePeacefulOccupierNation = TownyUniverse.getInstance().getNation(UUID.fromString(prePeacefulOccupierUUID));
							if (!(town.hasNation() && town.getNation() == prePeacefulOccupierNation)) {
								TownOccupationController.setTownOccupation(town, prePeacefulOccupierNation);
								TownMetaDataController.removePrePeacefulOccupierUUID(town);
								message += Translation.of("msg_town_returned_to_pre_peaceful_occupier",prePeacefulOccupierNation.getName());
						}
					}
				} catch (Throwable t) {
					System.out.println("Issue with re-assigning pre-peaceful occupier for town " + town.getName());
					t.printStackTrace();
				}
			}
		} else {
			if (town.isNeutral()) {
				message = Translation.of("msg_town_became_peaceful", town.getFormattedName());
			} else {
				message = Translation.of("msg_town_became_non_peaceful", town.getFormattedName());
			}
		}
		TownyMessaging.sendPrefixedTownMessage(town, message);
		town.save();
	}

	/**
	 * This method punishes any peaceful players who are in siege-zones
	 * (except for their own town OR any peaceful town)
	 * 
	 * A player is peaceful if they
	 * 1. Are resident in a peaceful town
	 * 2. Are resident in a declared (but not confirmed) peaceful town
	 *
	 * The punishment is a status effect (e.g. poison, nausea)
	 * The punishment is refreshed every 20 seconds, until the player leaves the siege-zone
	 */
	public static void punishPeacefulPlayersInActiveSiegeZones() {
		for(final Player player: BukkitTools.getOnlinePlayers()) {
			try {
				//Don't apply to towny admins
				if(TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player))
					continue;

				//Dont apply if player has the immunity perm
				if (TownyUniverse.getInstance().getPermissionSource().testPermission(player, SiegeWarPermissionNodes.SIEGEWAR_IMMUNE_TO_WAR_NAUSEA.getNode()))
					continue;

				//Don't apply to non-peaceful players
				Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
				if(resident == null || !(resident.hasTown()&& resident.getTown().isNeutral()))
					continue;

				//Don't punish if the player is in a peaceful town
				TownBlock townBlockAtPlayerLocation = TownyAPI.getInstance().getTownBlock(player.getLocation());
				if(townBlockAtPlayerLocation != null
					&& townBlockAtPlayerLocation.getTown().isNeutral())
				{
					continue;
				}

				//Don't punish if the player is in their own town
				if(resident.hasTown()
					&& townBlockAtPlayerLocation != null
					&& resident.getTown() == townBlockAtPlayerLocation.getTown())
				{
					continue;
				}

				//Punish if the player is in a siege zone
				if(SiegeWarDistanceUtil.isLocationInActiveSiegeZone(player.getLocation())) {
					TownyMessaging.sendMsg(player, Translation.of("msg_war_siege_peaceful_player_punished_for_being_in_siegezone"));
					final int effectDurationTicks = (int)(TimeTools.convertToTicks(TownySettings.getShortInterval() + 5));
					Towny.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(Towny.getPlugin(), new Runnable() {
						public void run() {
							List<PotionEffect> potionEffects = new ArrayList<>();
							potionEffects.add(new PotionEffect(PotionEffectType.CONFUSION, effectDurationTicks, 4));
							potionEffects.add(new PotionEffect(PotionEffectType.POISON, effectDurationTicks, 4));
							potionEffects.add(new PotionEffect(PotionEffectType.WEAKNESS, effectDurationTicks, 4));
							potionEffects.add(new PotionEffect(PotionEffectType.SLOW, effectDurationTicks, 2));
							player.addPotionEffects(potionEffects);
							player.setHealth(1);
						}
					});
				}
			} catch (Exception e) {
				try {
					System.err.println("Problem punishing peaceful player in siege zone - " + player.getName());
				} catch (Exception e2) {
					System.err.println("Problem punishing peaceful player in siege zone (could not read player name)");
				}
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method is a cleanup of peaceful town occupation statuses
	 * 
	 * Cycle peaceful towns
	 * - If town's has the correct occupation status, skip and go to next town
	 * - If town's has an incorrect occupation status, update occupation status.
	 *
	 * Rules for each peaceful town:
	 *
	 * 1. If there are no guardian towns nearby,
	 *    the peaceful town remains unoccupied.
	 *
	 * 2. If there are guardian towns nearby,
	 *    then the following rules apply:
	 *
	 *    2.1 If peaceful town has no nation:
	 *        Town is peacefully occupied,
	 *        by the nation with the biggest presence (num townblocks) in the local area.
	 *
	 *    2.2. If peaceful town has a nation,
	 *         an automatic contest occurs between:
	 *    	   i. The home nation, and
	 *         ii. Foreign nations who consider the home nation an enemy.
	 *
	 * 	       The winner is the nation with the biggest presence (num townblocks) in the local area.
	 *
	 * 	       If the home nation wins, the town is unoccupied.
	 *         If a foreign nation wins, the town is peacefully occupied
	 */
	public static void evaluatePeacefulTownOccupationAssignments() {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Town> towns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town peacefulTown;
		int modifiedTowns = 0;
		boolean townTransferred;
		Nation prevailingNation;

		while (townItr.hasNext()) {
			peacefulTown = townItr.next();

			try {
				//Skip if town is non-peaceful
				if (!peacefulTown.isNeutral())
					continue;

				//Skip if town is ruined
				if(peacefulTown.isRuined())
					continue;

				//Find nearby guardian nations
				Map<Nation, Integer> guardianNations = findNearbyGuardianNations(peacefulTown);

				//Filter out guardian nations which cannot exert influence
				guardianNations = filterOutGuardianNationsWithNoInfluence(guardianNations, peacefulTown);

				if(guardianNations.size() == 0) {
					//There are no influencing guardian nations nearby
					//Ensure peaceful town is unoccupied
					townTransferred = ensureTownIsPeacefullyUnoccupied(peacefulTown);
				} else {
					//There is one or more influencing guardian nations nearby
					//Get nation with biggest influence (the 'prevailing nation')
					prevailingNation = getNationWithLargestInfluence(guardianNations);

					//Set peaceful town occupier based on prevailing nation
					if (peacefulTown.hasNation() && peacefulTown.getNation() == prevailingNation) {
						//Prevailing nation is the town's home nation
						if (TownOccupationController.isTownOccupied(peacefulTown)
								&& TownOccupationController.getTownOccupier(peacefulTown) != prevailingNation) {
							//If the peaceful town is occupied by the foreign nation, release town now.
							townTransferred = ensureTownIsPeacefullyUnoccupied(peacefulTown);
						} else {
							//If the peaceful town is unoccupied OR occupied by the prevailing nation, do nothing.
							townTransferred = false;
						}
					} else {
						//Prevailing nation is not the town's home nation. occupy town now
						townTransferred = ensureTownIsPeacefullyOccupied(peacefulTown, prevailingNation);
					}
				}

				if (townTransferred)
					modifiedTowns += 1;

			} catch (Exception e) {
				try {
					System.err.println("Problem evaluating peaceful town nation assignment for - " + peacefulTown.getName());
				} catch (Exception e2) {
					System.err.println("Problem evaluating peaceful town nation assignment (could not read town name)");
				}
				e.printStackTrace();
			}
		}
		//Send a global message with how many towns were modified.
		if (modifiedTowns > 0) {
			boolean one = modifiedTowns == 1;
			Messaging.sendGlobalMessage(Translation.of("msg_peaceful_town_total_switches", modifiedTowns, one ? "" : "s", one ? "has" : "have"));
		}
	}

	 /*
	 * Filter out guardian nations with no influence
	 * - If the town has no nation, all guardian nations have influence
	 * - If the town has a nation, only the home nation & enemy foreign nations have an influence
	 */
	private static Map<Nation,Integer> filterOutGuardianNationsWithNoInfluence(Map<Nation,Integer> guardianNations, Town peacefulTown) throws NotRegisteredException {
		Map<Nation,Integer> result = new HashMap<>(guardianNations);
		if(peacefulTown.hasNation()) {
			for(Map.Entry<Nation,Integer> mapEntry: guardianNations.entrySet()) {
				if(mapEntry.getKey() != peacefulTown.getNation()
					&& !mapEntry.getKey().hasEnemy(peacefulTown.getNation())) {
						result.remove(mapEntry.getKey());
				}
			}
		}
		return result;
	}

	private static Nation getNationWithLargestInfluence(Map<Nation, Integer> guardianNations) {
		Map.Entry<Nation, Integer> winningEntry = null;
		for(Map.Entry<Nation,Integer> mapEntry: guardianNations.entrySet()) {
			if(winningEntry == null) {
				winningEntry = mapEntry;
			} else {
				if(mapEntry.getValue() > winningEntry.getValue()) {
					winningEntry = mapEntry;
				}
			}
		}
		return winningEntry.getKey();
	}

	private static boolean ensureTownIsPeacefullyUnoccupied(Town peacefulTown) throws NotRegisteredException {
		if(!TownOccupationController.isTownOccupied(peacefulTown)) {
			return false;
		}

		//Get current occupier
		Nation currentOccupier = TownOccupationController.getTownOccupier(peacefulTown);
		//Remove occupation
		TownOccupationController.removeTownOccupation(peacefulTown);

		//Send messages
		//Send to peaceful town
		TownyMessaging.sendPrefixedTownMessage(peacefulTown, Translation.of("msg_your_town_peacefully_released", currentOccupier.getName()));
		//Send to occupier
		TownyMessaging.sendPrefixedNationMessage(currentOccupier, Translation.of("msg_foreign_town_peacefully_released", peacefulTown.getName()));
		//Send to nation of peaceful town
		if(peacefulTown.hasNation())
			TownyMessaging.sendPrefixedNationMessage(TownyAPI.getInstance().getTownNationOrNull(peacefulTown), Translation.of("msg_home_town_peacefully_released", peacefulTown.getName(), currentOccupier.getName()));

		return true;
	}

	private static boolean ensureTownIsPeacefullyOccupied(Town peacefulTown, Nation newOccupier) throws NotRegisteredException {
		if(TownOccupationController.isTownOccupied(peacefulTown)) {
			//Town is already occupied
			Nation currentOccupier = TownOccupationController.getTownOccupier(peacefulTown);

			//Check if town is already occupied by correct nation
			if (currentOccupier == newOccupier)
				return false;

			//Remove current occupation
			ensureTownIsPeacefullyUnoccupied(peacefulTown);
		}

		//Set new  occupation
		TownOccupationController.setTownOccupation(peacefulTown, newOccupier);

		//Send messages
		//Send to peaceful town
		TownyMessaging.sendPrefixedTownMessage(peacefulTown, Translation.of("msg_your_town_peacefully_occupied", newOccupier.getName()));
		//Send to new occupier
		TownyMessaging.sendPrefixedNationMessage(newOccupier, Translation.of("msg_foreign_town_peacefully_occupied", peacefulTown.getName()));
		//Send to nation of peaceful town
		if(peacefulTown.hasNation())
			TownyMessaging.sendPrefixedNationMessage(TownyAPI.getInstance().getTownNationOrNull(peacefulTown), Translation.of("msg_home_town_peacefully_occupied", peacefulTown.getName(), newOccupier.getName()));

		return true; //Town switched
	}

	public static Map<Nation, Integer> findNearbyGuardianNations(Town peacefulTown) {
		Map<Nation, Integer> guardianNations = new HashMap<>();
		Nation guardianNation;
		int numTownBlocks;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		try {
			int guardianTownMinPlotsRequirement = SiegeWarSettings.getPeacefulTownsGuardianTownPlotsRequirement();
			int guardianTownMaxDistanceRequirementTownblocks = SiegeWarSettings.getPeacefulTownsGuardianTownMinDistanceRequirement();
	
			//Identify Guardian nations
			List<Town> candidateTowns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
			for(Town candidateTown: candidateTowns) {
				if(!candidateTown.isNeutral()
					&& (candidateTown.hasNation() || TownOccupationController.isTownOccupied(candidateTown))
					&& candidateTown.getTownBlocks().size() >= guardianTownMinPlotsRequirement
					&& SiegeWarDistanceUtil.areTownsClose(peacefulTown, candidateTown, guardianTownMaxDistanceRequirementTownblocks)) {

						guardianNation = TownOccupationController.isTownOccupied(candidateTown) ? TownOccupationController.getTownOccupier(candidateTown) : candidateTown.getNation();
						numTownBlocks = candidateTown.getTownBlocks().size();

						if(guardianNations.containsKey(guardianNation)) {
							guardianNations.put(guardianNation, guardianNations.get(guardianNation) + numTownBlocks);
						} else {
							guardianNations.put(guardianNation, numTownBlocks);
						}
				}
			}
		} catch (Exception e) {
			try {
				System.err.println("Problem getting valid guardian towns for - " + peacefulTown.getName());
			} catch (Exception e2) {
				System.err.println("Problem getting valid guardian towns (could not read peaceful town name)");
			}
			e.printStackTrace();
		}

		//Return result
		return guardianNations;
	}
}

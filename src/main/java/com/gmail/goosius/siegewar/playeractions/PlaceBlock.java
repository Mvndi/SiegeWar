package com.gmail.goosius.siegewar.playeractions;

import com.gmail.goosius.siegewar.Messaging;
import com.gmail.goosius.siegewar.SiegeController;
import com.gmail.goosius.siegewar.TownOccupationController;
import com.gmail.goosius.siegewar.enums.SiegeStatus;
import com.gmail.goosius.siegewar.objects.Siege;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.gmail.goosius.siegewar.settings.Translation;
import com.gmail.goosius.siegewar.utils.SiegeWarBlockUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarDistanceUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarNationUtil;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is fired from the SiegeWarActionListener's TownyBuildEvent listener.
 *
 * The class evaluates the event, and determines if it is siege related.
 * 
 * If the place block event is determined to be a siege action,
 * this class then calls an appropriate class/method in the 'playeractions' package
 *
 * @author Goosius
 */
public class PlaceBlock {
	
	/**
	 * Evaluates a block placement request.
	 * If the block is a standing banner or chest, this method calls an appropriate private method.
	 *
	 * @param player The player placing the block
	 * @param block The block about to be placed
	 * @param event The event object related to the block placement    	
	 */
	public static void evaluateSiegeWarPlaceBlockRequest(Player player, Block block, TownyBuildEvent event) {
		
		try {
			//Enforce Anti-Trap warfare build block if below siege banner altitude.
			if (SiegeWarSettings.isTrapWarfareMitigationEnabled()
					&& SiegeWarDistanceUtil.isLocationInActiveTimedPointZoneAndBelowSiegeBannerAltitude(event.getBlock().getLocation())) {
				event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.DARK_RED + Translation.of("msg_err_cannot_alter_blocks_below_banner_in_timed_point_zone")));
				event.setCancelled(true);
				return;
			}

			Material mat = block.getType();

			//Standing Banner placement
			if (Tag.BANNERS.isTagged(mat) && !mat.toString().contains("_W")) {
				try {
					evaluatePlaceStandingBanner(player, block);
				} catch (TownyException e1) {
					Messaging.sendErrorMsg(player, e1.getMessage());
				}
				return;
			}
	
			//Chest placement
			if (mat == Material.CHEST || mat == Material.TRAPPED_CHEST) {
				try {
					evaluatePlaceChest(player, block);
				} catch (TownyException e) {
					Messaging.sendErrorMsg(player, e.getMessage());
				}
				return;
			}
	
			//Check for forbidden siegezone block placement
			if(SiegeWarSettings.isWarSiegeZoneBlockPlacementRestrictionsEnabled() 
					&& TownyAPI.getInstance().isWilderness(block) 
					&& SiegeWarDistanceUtil.isLocationInActiveSiegeZone(block.getLocation())
					&& SiegeWarSettings.getWarSiegeZoneBlockPlacementRestrictionsMaterials().contains(mat))
					throw new TownyException(Translation.of("msg_war_siege_zone_block_placement_forbidden"));
			
		} catch (TownyException e) {
			event.setCancelled(true);
			event.setMessage(e.getMessage());
		}
	}

	/**
	 * Evaluates placing a standing banner
	 * @throws TownyException if the banner will not be allowed.
	 */
	private static void evaluatePlaceStandingBanner(Player player, Block block) throws TownyException {
		//Ensure the the banner is placed in wilderness
		if (!TownyAPI.getInstance().isWilderness(block))
			return;

		//Ensure there ia an adjacent town
		List<TownBlock> adjacentTownBlocks = SiegeWarBlockUtil.getCardinalAdjacentTownBlocks(block);
		if (adjacentTownBlocks.size() == 0)
			return;

		//Ensure siege is enabled in this world
		if (!SiegeWarSettings.getWarSiegeWorlds().contains(block.getWorld().getName()))
			throw new TownyException(Translation.of("msg_err_siege_war_not_enabled_in_world"));

		//Ensure that there is just 1 town nearby (or else we don't know which town to affect
		if (adjacentTownBlocks.size() > 1)
			throw new TownyException(Translation.of("msg_err_siege_war_too_many_adjacent_cardinal_town_blocks"));

		//Ensure siege is enabled in this world
		if (!SiegeWarSettings.getWarSiegeWorlds().contains(block.getWorld().getName()))
			throw new TownyException(Translation.of("msg_err_siege_war_not_enabled_in_world"));

		//Ensure the player has a town
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		if (resident == null || !resident.hasTown())
			throw new TownyException(Translation.of("msg_err_siege_war_action_not_a_town_member"));
		Town residentsTown = resident.getTown();

		//Get resident's nation (if any, for convenience)
		Nation residentsNation = null;
		if (residentsTown.hasNation())
			residentsNation = residentsTown.getNation();

		if (isWhiteBanner(block)) {
			evaluatePlaceWhiteBannerNearTown(player, residentsTown, residentsNation, adjacentTownBlocks.get(0).getTown());
		} else {
			evaluatePlaceColouredBannerNearTown(player, residentsTown, residentsNation, adjacentTownBlocks.get(0).getTown());
		}
	}

		/**
         * Evaluates placing a white banner near a town
         *
         * Effects depend on the nature of the siege (if any) and the allegiances of the banner placer
		 *
         * @param player the player placing the banner
         * @param residentsTown the town of the player placing the banner
		 * @param residentsNation the nation of the player placing the banner (can be null)
		 * @param nearbyTown the nearby town
         */
	private static void evaluatePlaceWhiteBannerNearTown(Player player,
												   Town residentsTown,
												   Nation residentsNation,
												   Town nearbyTown) throws TownyException {

		//Ensure that the nearby town has an active siege
		if (!SiegeController.hasActiveSiege(nearbyTown))
			throw new TownyException(Translation.of("msg_err_town_has_no_active_siege_to_abandon_surrender"));

		//Get siege
		Siege siege = SiegeController.getSiege(nearbyTown);

		/*
		 * Check what type of action this qualifies as.
		 * Depending on the scenario, it may be 'abandonAttack' or 'surrenderDefence'
		 */
		switch (siege.getSiegeType()) {
			case CONQUEST:
				if (residentsNation != null && residentsNation == siege.getAttacker()) {
					AbandonAttack.processAbandonAttackRequest(player, siege);
				} else if (residentsTown == nearbyTown) {
					SurrenderDefence.processSurrenderDefenceRequest(player, siege);
				} else {
					throw new TownyException("msg_err_action_disable");
				}
				break;
			case LIBERATION:
				if (residentsNation != null && residentsNation == siege.getAttacker()) {
					AbandonAttack.processAbandonAttackRequest(player, siege);
				} else if (residentsNation != null && TownOccupationController.getTownOccupier(nearbyTown) == residentsNation) {
					SurrenderDefence.processSurrenderDefenceRequest(player, siege);
				} else {
					throw new TownyException("msg_err_action_disable");
				}
				break;
			case REVOLT:
				if (residentsTown == nearbyTown) {
					AbandonAttack.processAbandonAttackRequest(player, siege);
				} else if (residentsNation != null && TownOccupationController.getTownOccupier(nearbyTown) == residentsNation) {
					SurrenderDefence.processSurrenderDefenceRequest(player, siege);
				} else {
					throw new TownyException("msg_err_action_disable");
				}
				break;
			case SUPPRESSION:
				if (residentsNation != null && TownOccupationController.getTownOccupier(nearbyTown) == residentsNation) {
					AbandonAttack.processAbandonAttackRequest(player, siege);
					if (residentsTown == nearbyTown) {
						SurrenderDefence.processSurrenderDefenceRequest(player, siege);
					} else {
						throw new TownyException("msg_err_action_disable");
					}
					break;
				}
		}
	}

	/**
	 * Evaluates coloured banner near a town
	 *
	 * Effects depend on the allegiances of the town and of the banner placer
	 *
	 * @param player the player placing the banner
	 * @param residentsTown the town of the player placing the banner
	 * @param residentsNation the nation of the player placing the banner (can be null)
	 * @param nearbyTown the nearby town
	 */
	public static void evaluatePlaceColouredBannerNearTown(Player player,
														   Town residentsTown,
														   Nation residentsNation,
														   Town nearbyTown) throws TownyException {

		//Check whether nearby town has a current or recent siege
		Siege siege = null;
		if (!SiegeController.hasSiege(nearbyTown))
			siege = SiegeController.getSiege(nearbyTown);

		if(siege == null) {
			/*
			 * Start new siege request
			 * Check what type of siege this would be.
			 */
		} else {
			evaluateInvasionAttempt(player, residentsNation, nearbyTown, siege);
		}
	}



	private static void evaluateInvasionAttempt(Player player, Nation residentsNation, Town nearbyTown, Siege siege) throws TownyException {
		if(residentsNation == null)
			throw new TownyException("msg_err_action_disable");  //Can't invade if nationless

		if(residentsNation != siege.getAttacker())
			throw new TownyException("msg_err_action_disable");  //Can't invade unless you are the attacker

		if(siege.getStatus().isActive())
			throw new TownyException("msg_err_cannot_invade_siege_still_in_progress");

		if(residentsNation == TownOccupationController.getTownOccupier(nearbyTown))
			throw new TownyException("msg_err_cannot_invade_your_nation_already_occupies_town");

		if (siege.getStatus() != SiegeStatus.ATTACKER_WIN && siege.getStatus() != SiegeStatus.DEFENDER_SURRENDER)
			throw new TownyException(Translation.of("msg_err_cannot_invade_without_victory"));

		if (siege.isTownInvaded())
			throw new TownyException(Translation.of("msg_err_town_already_invaded"));

		if (TownySettings.getNationRequiresProximity() > 0) {
			Coord capitalCoord = residentsNation.getCapital().getHomeBlock().getCoord();
			Coord townCoord = nearbyTown.getHomeBlock().getCoord();
			if (!residentsNation.getCapital().getHomeBlock().getWorld().getName().equals(nearbyTown.getHomeBlock().getWorld().getName())) {
				throw new TownyException(Translation.of("msg_err_nation_homeblock_in_another_world"));
			}
			double distance;
			distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));
			if (distance > TownySettings.getNationRequiresProximity()) {
				throw new TownyException(String.format(Translation.of("msg_err_town_not_close_enough_to_nation"), nearbyTown.getName()));
			}
		}

		if (TownySettings.getMaxTownsPerNation() > 0) {
			int effectiveNumTowns = SiegeWarNationUtil.calculateEffectiveNumberOfTownsInNation(residentsNation);
			if (effectiveNumTowns >= TownySettings.getMaxTownsPerNation()){
				throw new TownyException(String.format(Translation.of("msg_err_nation_over_town_limit"), TownySettings.getMaxTownsPerNation()));
			}
		}

		InvadeTown.processInvadeTownRequest(player, siege);
	}



		/*
		// Fail early if this is not a siege-enabled world.
		if(!SiegeWarSettings.getWarSiegeWorlds().contains(block.getWorld().getName()))
			throw new TownyException(Translation.of("msg_err_siege_war_not_enabled_in_world"));
		
		List<TownBlock> nearbyCardinalTownBlocks = SiegeWarBlockUtil.getCardinalAdjacentTownBlocks(block);

		//Ensure that only one of the cardinal points has a townblock
		if(nearbyCardinalTownBlocks.size() > 1)
			throw new TownyException(Translation.of("msg_err_siege_war_too_many_adjacent_cardinal_town_blocks"));

		//Get nearby town
		Town town;
		try {
			town = nearbyCardinalTownBlocks.get(0).getTown();
		} catch (NotRegisteredException e) {
			return;
		}

		//Ensure that there is only one town adjacent
		List<TownBlock> adjacentTownBlocks = new ArrayList<>();
		adjacentTownBlocks.addAll(nearbyCardinalTownBlocks);
		adjacentTownBlocks.addAll(SiegeWarBlockUtil.getNonCardinalAdjacentTownBlocks(block));
		for(TownBlock adjacentTownBlock: adjacentTownBlocks) {
			try {
				if (adjacentTownBlock.getTown() != town)
					throw new TownyException(Translation.of("msg_err_siege_war_too_many_adjacent_towns"));
			} catch (NotRegisteredException nre) {}
		}

		//If the town has a siege where the player's nation is already attacking, 
		//attempt invasion, otherwise attempt attack
		if(SiegeController.hasSiege(town) && SiegeController.getSiege(town).getNation() == nation) {

			if (!SiegeWarSettings.getWarSiegeInvadeEnabled())
				return;

			if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, SiegeWarPermissionNodes.SIEGEWAR_NATION_SIEGE_INVADE.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));

			Siege siege = SiegeController.getSiege(town);
			if (siege.getStatus() != SiegeStatus.ATTACKER_WIN && siege.getStatus() != SiegeStatus.DEFENDER_SURRENDER)
				throw new TownyException(Translation.of("msg_err_cannot_invade_without_victory"));
			
			if(attackingTown == town)
				throw new TownyException(Translation.of("msg_err_cannot_invade_own_town"));
			
			InvadeTown.processInvadeTownRequest(nation, town, siege);

		} else {

			if (!SiegeWarSettings.getWarSiegeAttackEnabled())
				return;

			if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, SiegeWarPermissionNodes.SIEGEWAR_NATION_SIEGE_ATTACK.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			if (TownyEconomyHandler.isActive() && !nation.getAccount().canPayFromHoldings(SiegeWarMoneyUtil.getSiegeCost(town)))
				throw new TownyException(Translation.of("msg_err_no_money"));
	        
	        if(getNumActiveAttackSieges(nation) >= SiegeWarSettings.getWarSiegeMaxActiveSiegeAttacksPerNation())
				throw new TownyException(Translation.of("msg_err_siege_war_nation_has_too_many_active_siege_attacks"));
			
			if (attackingTown == town)
                throw new TownyException(Translation.of("msg_err_siege_war_cannot_attack_own_town"));
			
			if(SiegeWarBlockUtil.isSupportBlockUnstable(block))
				throw new TownyException(Translation.of("msg_err_siege_war_banner_support_block_not_stable"));

			AttackTown.processAttackTownRequest(
				attackingTown,
				nation,
				block,
				nearbyCardinalTownBlocks.get(0),
				town);
		}

*/

	/**
	 * Evaluates placing a chest.
	 * Determines if the event will be considered as a plunder request.
	 * @throws TownyException when the chest is not allowed to be placed.
	 */
	private static void evaluatePlaceChest(Player player, Block block) throws TownyException {
		if (!SiegeWarSettings.getWarSiegePlunderEnabled() || !TownyAPI.getInstance().isWilderness(block))
			return;
		
		if(!TownyEconomyHandler.isActive())
			throw new TownyException(Translation.of("msg_err_siege_war_cannot_plunder_without_economy"));
		
		List<TownBlock> nearbyTownBlocks = SiegeWarBlockUtil.getCardinalAdjacentTownBlocks(block);

		if (nearbyTownBlocks.size() > 1) //More than one town block nearby. Error
			throw new TownyException(Translation.of("msg_err_siege_war_too_many_adjacent_towns"));

		//Get nearby town
		Town town = null;
		try {
			town = nearbyTownBlocks.get(0).getTown();
		} catch (NotRegisteredException ignored) {}

		//If there is no siege, do normal block placement
		if(!SiegeController.hasSiege(town))
			return;

		//Attempt plunder.
		PlunderTown.processPlunderTownRequest(player, town);
	}
	
	private static boolean isWhiteBanner(Block block) {
		return block.getType() == Material.WHITE_BANNER  && ((Banner) block.getState()).getPatterns().size() == 0;
	}
	
	private static int getNumActiveAttackSieges(Nation nation) {
		int result = 0;
		for(Siege siege: SiegeController.getSieges()) {
			if(siege.getNation() == nation && siege.getStatus().isActive())
				result++;
		}
		return result;
	}

	private static boolean isValidPlacement(Block block) {
		if (!TownyAPI.getInstance().isWilderness(block)) {
			if (isWhiteBanner(block))
				return true;
		} else {
			if (SiegeWarBlockUtil.getCardinalAdjacentTownBlocks(block).size() > 0)
				return true;
		}
		return false;
	}
}


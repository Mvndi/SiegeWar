package com.gmail.goosius.siegewar.listeners;

import com.gmail.goosius.siegewar.SiegeController;
import com.gmail.goosius.siegewar.SiegeWar;
import com.gmail.goosius.siegewar.SiegeWarAPI;
import com.gmail.goosius.siegewar.TownOccupationController;
import com.gmail.goosius.siegewar.enums.SiegeSide;
import com.gmail.goosius.siegewar.enums.SiegeWarPermissionNodes;
import com.gmail.goosius.siegewar.hud.SiegeHUDManager;
import com.gmail.goosius.siegewar.objects.BattleSession;
import com.gmail.goosius.siegewar.objects.Siege;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.gmail.goosius.siegewar.tasks.SiegeWarTimerTaskController;
import com.gmail.goosius.siegewar.utils.SiegeWarBattleSessionUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarBlockProtectionUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarBlockUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarDistanceUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarImmunityUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarInventoryUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarMoneyUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarNationUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarTownPeacefulnessUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarNotificationUtil;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.event.NationRemoveAllyEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentRankEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentRankEvent;
import com.palmergames.bukkit.towny.event.TownyLoadedDatabaseEvent;
import com.palmergames.bukkit.towny.event.TranslationLoadEvent;
import com.palmergames.bukkit.towny.event.actions.TownyExplodingBlocksEvent;
import com.palmergames.bukkit.towny.event.damage.TownyExplosionDamagesEntityEvent;
import com.palmergames.bukkit.towny.event.damage.TownyFriendlyFireTestEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankAddEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankRemoveEvent;
import com.palmergames.bukkit.towny.event.player.PlayerKeepsInventoryEvent;
import com.palmergames.bukkit.towny.event.teleport.OutlawTeleportEvent;
import com.palmergames.bukkit.towny.event.time.NewHourEvent;
import com.palmergames.bukkit.towny.event.time.NewShortTimeEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.TranslationLoader;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.util.StringMgmt;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author LlmDl
 *
 */
public class SiegeWarTownyEventListener implements Listener {

	@SuppressWarnings("unused")
	private final SiegeWar plugin;
	
	public SiegeWarTownyEventListener(SiegeWar instance) {

		plugin = instance;
	}
	
	   /*
     * Siegewar has to be conscious of when Towny has loaded the Towny database.
     */
    @EventHandler(ignoreCancelled = true)
    public void onTownyDatabaseLoad(TownyLoadedDatabaseEvent event) {
    	SiegeWar.info("Towny database reload detected, reloading sieges...");
        SiegeController.loadAll();
    }
    
	/*
	 * When Towny is reloading the languages, make sure we're re-injecting our language strings. 
	 */
	@EventHandler(ignoreCancelled = true)
	public void onTownyLoadLanguages(TranslationLoadEvent event) {
		Plugin plugin = SiegeWar.getSiegeWar();
		Path langFolderPath = Paths.get(plugin.getDataFolder().getPath()).resolve("lang");
		TranslationLoader loader = new TranslationLoader(langFolderPath, plugin, SiegeWar.class);
		loader.load();
		Map<String, Map<String, String>> translations = loader.getTranslations();

		for (String language : translations.keySet())
			for (Map.Entry<String, String> map : translations.get(language).entrySet())
				event.addTranslation(language, map.getKey(), map.getValue());
	}

    @EventHandler(ignoreCancelled = true)
    public void onNewDay(NewDayEvent event) {
        if (SiegeWarSettings.getWarSiegeEnabled()) {
            if (SiegeWarSettings.isPlunderPaidOutOverDays()) {
                SiegeWarMoneyUtil.payDailyPlunderDebt();
            }
            if(SiegeWarSettings.getWarCommonPeacefulTownsEnabled()) {
                SiegeWarTownPeacefulnessUtil.updateTownPeacefulnessCounters();
            }
            if(SiegeWarSettings.getMaxOccupationTaxPerPlot() > 0) {
                TownOccupationController.collectNationOccupationTax();
            }
            SiegeWarNationUtil.updateNationDemoralizationCounters();
            SiegeWarMoneyUtil.calculateEstimatedTotalMoneyInEconomy(false);
            if (SiegeWarSettings.getTownPeacefulnessCost() > 0.0) {
                SiegeWarTownPeacefulnessUtil.chargeForPeacefulTowns();
            }
        }
    }

    /*
     * On NewHours SW makes some calculations.
     */
    @EventHandler(ignoreCancelled = true)
    public void onNewHour(NewHourEvent event) {
        if(SiegeWarSettings.getWarSiegeEnabled()) {
            SiegeWarImmunityUtil.evaluateExpiredImmunities();
            SiegeWarNotificationUtil.clearSiegeZoneProximityWarningsReceived();
        }
    }

    /*
     * On each ShortTime period, SW makes some calcuations.
     */
    @EventHandler(ignoreCancelled = true)
    public void onShortTime(NewShortTimeEvent event) {
        if (SiegeWarSettings.getWarSiegeEnabled()) {
            SiegeWarNotificationUtil.sendSiegeZoneProximityWarnings();
            SiegeWarTimerTaskController.evaluateBattleSessions();
            SiegeWarDistanceUtil.recalculatePlayersRegisteredToActiveSiegeZones();
            SiegeWarTimerTaskController.evaluateWarSickness();
            SiegeWarTimerTaskController.evaluateBannerControl();
            SiegeWarTimerTaskController.evaluateTimedSiegeOutcomes();
            SiegeHUDManager.updateHUDs();
            SiegeWarTimerTaskController.evaluateBeacons();
        }
    }

    /**
     * Process block explosion events coming from Towny 
     *
     * @param event the TownyExplodingBlocksEvent event
     * @throws TownyException if something is misconfigured
     */
    @EventHandler(priority = EventPriority.HIGH,ignoreCancelled = true)
    public void onBlockExploding(TownyExplodingBlocksEvent event) throws TownyException {
        if(!SiegeWarSettings.getWarSiegeEnabled())
            return;
        if (event.getEntity() != null && !TownyAPI.getInstance().getTownyWorld(event.getEntity().getWorld()).isWarAllowed())
            return;    
        List<Block> filteredExplodeList = event.getTownyFilteredBlockList();
        filteredExplodeList = filterExplodeListBySiegeBannerProtection(filteredExplodeList);
        filteredExplodeList = filterExplodeListByTrapWarfareMitigation(filteredExplodeList);
        event.setBlockList(filteredExplodeList);
    }

    /**
     * Filter a given explode list by siege banner protection
     * @param givenExplodeList given list of exploding blocks
     *
     * @return filtered list
     */
    private static List<Block> filterExplodeListBySiegeBannerProtection(List<Block> givenExplodeList) {       
        List<Block> filteredList = new ArrayList<>(givenExplodeList);
        for(Block block: givenExplodeList) {
            if (SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(block)) {
                //Remove block from final explode list
                filteredList.remove(block);
            } 
        }
        return filteredList;
    }

    /**
     * Filter a given explode list by trap warfare mitigation
     * @param givenExplodeList given list of exploding blocks
     *
     * @return filtered list
     */
    private static List<Block> filterExplodeListByTrapWarfareMitigation(List<Block> givenExplodeList) {
        if(!SiegeWarSettings.isWildernessTrapWarfareMitigationEnabled())
            return givenExplodeList;
        //For performance, just get the siege at the 1st block
        Siege siege;
        if(givenExplodeList.size() > 0) {
            siege = SiegeController.getActiveSiegeAtLocation(givenExplodeList.get(0).getLocation());
            if(siege == null)
               return givenExplodeList;
        } else {
            return givenExplodeList;
        }

        //Make convenience variables
        int townProtectionRadiusBlocks = SiegeWarSettings.getBesiegedTownTrapWarfareMitigationRadius();
        Location siegeBannerLocation = siege.getFlagLocation();

        //Filter exploding blocks
        List<Block> finalExplodeList = new ArrayList<>(givenExplodeList);

        for(Block block: givenExplodeList) {
            if(TownyAPI.getInstance().isWilderness(block)) {
                //Stop block exploding in wilderness
                if (SiegeWarBlockProtectionUtil.isWildernessLocationProtectedByTrapWarfareMitigation(
                        block.getLocation(),
                        siegeBannerLocation)) {
                    finalExplodeList.remove(block);
                }
            } else {
                //Stop block exploding in besieged town
                if (SiegeWarDistanceUtil.areLocationsCloseHorizontally(
                        block.getLocation(),
                        siegeBannerLocation,
                        townProtectionRadiusBlocks)) {
                    finalExplodeList.remove(block);
                }
            }
        }

        //Return final explode list
        return finalExplodeList;
    }

    /**
     * During a siege, players in the town are not protected from explosion damage.
     * (a related effect to how PVP protection is forced off)
     *
     * @param event the TownyExplosionDamagesEntityEvent event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onExplosionDamageEntity(TownyExplosionDamagesEntityEvent event) {
        if (!event.isCancelled())
            return;
        if(!SiegeWarSettings.getWarSiegeEnabled())
            return;
        if (!TownyAPI.getInstance().getTownyWorld(event.getEntity().getWorld()).isWarAllowed())
            return;
        if (!BattleSession.getBattleSession().isActive())
            return;
        if(!(event.getEntity() instanceof Player))
            return;
        if(event.getTown() == null)
            return;
        if(SiegeController.hasActiveSiege(event.getTown()))
            event.setCancelled(false);
    }
    
    /**
     * Prevent an outlaw being teleported away if the town they are outlawed in has an active siege.
     * @param event OutlawTeleportEvent thrown by Towny.
     */
    @EventHandler(ignoreCancelled = true)
    public void onOutlawTeleportEvent(OutlawTeleportEvent event) {
        if (SiegeWarSettings.getWarSiegeEnabled() 
                && event.getOutlawLocation() != null
                && event.getOutlawLocation().getWorld() != null
                && TownyAPI.getInstance().getTownyWorld(event.getOutlawLocation().getWorld()).isWarAllowed()
                && event.getTown() != null
                && SiegeController.hasActiveSiege(event.getTown())) {
    		event.setCancelled(true);
            }
    }

    /**
     * If friendly fire has cancelled damage in a SiegeZone,
     * undo the cancellation
     * @param event the event
     */
    @EventHandler(ignoreCancelled = true)
    public void on (TownyFriendlyFireTestEvent event) {
    	if (!event.isPVP()
    	        && SiegeWarSettings.getWarSiegeEnabled() 
                && TownyAPI.getInstance().getTownyWorld(event.getAttacker().getWorld()).isWarAllowed()
                && SiegeWarSettings.isStopTownyFriendlyFireProtection()
                && BattleSession.getBattleSession().isActive()
                && SiegeWarDistanceUtil.isPlayerRegisteredToActiveSiegeZone(event.getAttacker())) {
            event.setPVP(true);
        }
    }

    /**
     * Broadcast rank removal in SiegeZones.
     * 
     * @param event TownRemoveResidentEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(TownRemoveResidentRankEvent event) {
        tryBroadCastRankRemoval(event.getRank(), event.getResident());
    }

    /**
     * Broadcast rank removal in SiegeZones.
     * 
     * @param event NationRankRemoveEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(NationRankRemoveEvent event) {
        tryBroadCastRankRemoval(event.getRank(), event.getResident());
    }

    /**
     * Broadcast town removal in SiegeZones.
     * 
     * @param event TownRemoveResidentEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(TownRemoveResidentEvent event) {
        tryBroadCastTownRemoval(event.getResident(), event.getTown());
    }

    /**
     * Broadcast nation removal in SiegeZones.
     * 
     * @param event NationRemoveTownEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(NationRemoveTownEvent event) {
        tryBroadCastNationRemoval(event.getTown(), event.getNation());
    }

    /**
     * Broadcast nation ally removal in SiegeZones.
     * 
     * @param event NationRemoveAllyEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(NationRemoveAllyEvent event) {
        tryBroadCastNationAllyRemoval(event.getRemovedNation(), event.getNation());
    }

    /**
     * Check over the players in a nation when that nation has been removed as an
     * ally, to see if they're in a siegezone.
     * 
     * @param removedNation Nation which is no longer an ally.
     * @param nation        Nation which has removed an ally.
     */
    private void tryBroadCastNationAllyRemoval(Nation removedNation, Nation nation) {
        // Gather a list of player names who are located in siegezones.
        Map<Siege, List<String>> siegePlayerMap = getSiegesAndPlayerNames(
                TownyAPI.getInstance().getOnlinePlayersInNation(removedNation));

        for (Siege siege : siegePlayerMap.keySet())
            // Inform the players who are still a part of the Siege about the players in the Sieges' zones.
            SiegeWarNotificationUtil.informSiegeParticipants(siege,
                    // Message: 'Warning: %s was removed as an ally from nation %s while the player(s) %s were located in the siegezone at %s.'
                    Translatable.of("warn_nation_had_ally_removed", removedNation.getName(), nation.getName(),
                        StringMgmt.join(siegePlayerMap.get(siege), ", "), getDateAndTime()));
    }

    /**
     * Check over the players in a town to see if they're in a siegezone.
     * 
     * @param town   Town that got removed from the nation.
     * @param nation Nation that removed the town.
     */
    private void tryBroadCastNationRemoval(Town town, Nation nation) {
        Map<Siege, List<String>> siegePlayerMap = getSiegesAndPlayerNames(
                TownyAPI.getInstance().getOnlinePlayersInTown(town));
        for (Siege siege : siegePlayerMap.keySet()) {
            SiegeWarNotificationUtil.informSiegeParticipants(siege,
                    // Message: 'Warning: %s was removed from nation %s while the player(s) %s were located in the siegezone at %s.'
                    Translatable.of("warn_nation_had_town_removed", town.getName(), nation.getName(),
                            StringMgmt.join(siegePlayerMap.get(siege), ", "), getDateAndTime()));
        }
    }

    /**
     * Returns a map keyed with Sieges which has Players located in their SiegeZone.
     * 
     * @param playersOnlineInGovernment List of Players which are online from a
     *                                  select Town or Nation.
     * @return siegePlayerMap A map of Sieges and the names of the players located
     *         in those siege zones.
     */
    private Map<Siege, List<String>> getSiegesAndPlayerNames(List<Player> playersOnlineInGovernment) {
        Map<Siege, List<String>> siegePlayerMap = new HashMap<>();
        for (Player player : playersOnlineInGovernment) {
            Siege siege = SiegeController.getActiveSiegeAtLocation(player.getLocation());
            // Only put players into the map if they are an attacker or defender.
            if (siege != null && SiegeSide.getPlayerSiegeSide(siege, player).equals(SiegeSide.NOBODY)) {
                if (siegePlayerMap.containsKey(siege)) {
                    siegePlayerMap.get(siege).add(player.getName());
                } else {
                    siegePlayerMap.put(siege, Collections.singletonList(player.getName()));
                }
            }
        }
        return siegePlayerMap;
    }

    /**
     * Check if a player is in the SiegeZone when they were removed from the town.
     * 
     * @param resident Resident losing their town.
     * @param town     Town the resident was a part of.
     */
    private void tryBroadCastTownRemoval(Resident resident, Town town) {
        Siege siege = siegeAtPlayerLocation(resident);
        if (siege != null)
            SiegeWarNotificationUtil.informSiegeParticipants(siege,
                    // Message: 'Warning: %s was removed from town %s while in the siegezone at %s.'
                    Translatable.of("warn_resident_had_town_removed",
                    resident.getName(), town.getName(), getDateAndTime())); 
    }

	private String getDateAndTime() {
		return DateFormat.getInstance().format(Date.from(Instant.now()));
	}

    /**
     * Check if a player is in the SiegeZone and losing a SiegeWar rank and
     * broadcast the action to the public.
     * 
     * @param rank     Rank being taken from the resident.
     * @param resident Resident losing their rank.
     */
    private void tryBroadCastRankRemoval(String rank, Resident resident) {
        if (rank.contains("siegewar")) {
            Siege siege = siegeAtPlayerLocation(resident);
            if (siege != null)
                SiegeWarNotificationUtil.informSiegeParticipants(siege,
                        // Message: 'Warning: %s had their rank %s taken from them while in the siegezone at %s.'
                        Translatable.of("warn_resident_had_rank_removed", resident.getName(), rank, getDateAndTime()));
        }
    }

    /**
     * Return an active Siege based on the location of the Resident, if they are
     * online.
     * 
     * @param resident Resident losing their rank.
     * @return an active Siege or null.
     */
    @Nullable
    private Siege siegeAtPlayerLocation(Resident resident) {
        if (resident.isOnline())
            return SiegeController.getActiveSiegeAtLocation(resident.getPlayer().getLocation());
        return null;
    }

	/**
	 * Have SiegeWar have the ultimate say on whether siege zones have inventory
	 * keeping.
	 * 
	 * @param event {@link PlayerKeepsInventoryEvent} internal to Towny.
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onTownyKeepInventoryEvent(PlayerKeepsInventoryEvent event) {
		if (!SiegeWarDistanceUtil.isLocationInActiveSiegeZone(event.getPlayer().getLocation()))
			return;

		// Towny is already going to keep the inventory.
		if (!event.isCancelled()) {
			// But we dont want inventories saved.
			if (!SiegeWarSettings.isKeepInventoryOnSiegeZoneDeathEnabled()) {
				event.setCancelled(true);
				return;
			}
			// But we don't want defenders that are losing too greatly to keep their inventories.
			if (SiegeWarSettings.isDefendersDropInventoryWhenLosingEnabled()) {
				Siege siege = SiegeWarAPI.getActiveSiegeAtLocation(event.getLocation());
				if (!SiegeSide.isDefender(siege, event.getPlayer()))
					return;
				if (SiegeWarBattleSessionUtil.getSiegeBalanceIfSessionEndedNow(siege)
					< SiegeWarSettings.getDefendersDropInventoryWhenLosingThreshold())
					return;

				event.setCancelled(true);
				return;
			}
		}

		// Towny is going to drop the inventory, but we want inventories saved.
		if (event.isCancelled() && SiegeWarSettings.isKeepInventoryOnSiegeZoneDeathEnabled()) {

			Siege siege = SiegeWarAPI.getActiveSiegeAtLocation(event.getLocation());
			// But we don't want defenders that are losing too greatly to keep their inventories.
			if (SiegeWarSettings.isDefendersDropInventoryWhenLosingEnabled()
				&& SiegeSide.isDefender(siege, event.getPlayer())
				&& SiegeWarBattleSessionUtil.getSiegeBalanceIfSessionEndedNow(siege) >= SiegeWarSettings.getDefendersDropInventoryWhenLosingThreshold())
				return;

			SiegeWarInventoryUtil.degradeInventory(event.getPlayer());
			event.setCancelled(false);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onTownRankAdded(TownAddResidentRankEvent event) {
		String rank = event.getRank();

		if (!rankContainsStartConquestSiegeNode(TownyPerms.getTownRankPermissions(rank)))
			return;

		checkRankForBattleCommanderNodeAndCancel(rank, event.getResident(), event);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onNationRankAdded(NationRankAddEvent event) {
		String rank = event.getRank();

		if (!rankContainsStartConquestSiegeNode(TownyPerms.getNationRankPermissions(rank)))
			return;

		checkRankForBattleCommanderNodeAndCancel(rank, event.getResident(), event);
	}

	private boolean rankContainsStartConquestSiegeNode(List<String> nodes) {
		for (String node : nodes) {
			if (node.equalsIgnoreCase("siegewar.nation.siege.*"))
				return true;
			if (node.equalsIgnoreCase(SiegeWarPermissionNodes.SIEGEWAR_NATION_SIEGE_STARTCONQUESTSIEGE.getNode()))
				return true;
		}
		return false;
	}

	private void checkRankForBattleCommanderNodeAndCancel(String rank, Resident resident, CancellableTownyEvent event) {
		if (!resident.hasTown() || !hasSiege(resident))
			return;

		if (SiegeWarSettings.isBattleCommandersRanksBlockedDuringSieges()) {
			event.setCancelled(true);
			event.setCancelMessage(Translatable.of("siege_msg_err_cannot_assign_rank_during_active_siege", rank).forLocale(resident));
			return;
		}

		if (SiegeWarSettings.isBattleCommandersRanksBlockedDuringActiveBattleSession() && SiegeWarAPI.isBattleSessionActive()) {
			event.setCancelled(true);
			event.setCancelMessage(Translatable.of("siege_msg_err_cannot_assign_rank_during_active_battlesession", rank).forLocale(resident));
		}
	}

	private boolean hasSiege(Resident resident) {
		if (SiegeWarAPI.hasSiege(resident))
			return true;

		if (!resident.hasNation())
			return false;

		Nation nation = resident.getNationOrNull();
		return SiegeWarAPI.getActiveDefensiveSieges(nation).size() + SiegeWarAPI.getActiveOffensiveSieges(nation).size() > 0;
	}
}

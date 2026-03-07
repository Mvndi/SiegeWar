package com.gmail.goosius.siegewar.listeners;

import com.gmail.goosius.siegewar.PillageController;
import com.gmail.goosius.siegewar.SiegeWar;
import com.gmail.goosius.siegewar.events.SiegeEndEvent;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.actions.TownyActionEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.actions.TownySwitchEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MvndiPillageListener implements Listener {
    private ConcurrentHashMap<UUID, Long> buildDestoryCooldownMap;
    
    public MvndiPillageListener() {
        buildDestoryCooldownMap = new ConcurrentHashMap<>();
    }

    @EventHandler(ignoreCancelled = true)
    public void onSiegeEnd(SiegeEndEvent event) {
        if ("ATTACKERS".equals(event.getSiegeWinner())) {
            PillageController.getInstance().startPillaging(event.getSiege().getTown());
        } else {
            SiegeWar.info("No pillage for " + event.getSiege().getTown().getName()+ " because siege was won by " + event.getSiegeWinner() + " and not by " + event.getAttackerName());
        }
    }

    // Uncancel each towny event in a pillaging town
    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onBuildInPillagingTown(TownySwitchEvent event) {
        onTownyEvent(event, false);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onBuildInPillagingTown(TownyBuildEvent event) {
        onTownyEvent(event, true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onBuildInPillagingTown(TownyDestroyEvent event) {
        onTownyEvent(event, false);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onBuildInPillagingTown(TownyItemuseEvent event) {
        onTownyEvent(event, true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();		
		if (!TownyAPI.getInstance().isTownyWorld(block.getWorld()))
			return;

        TownBlock tb = TownyAPI.getInstance().getTownBlock(block.getLocation());
        if (tb == null)
            return;
        Town town = TownyAPI.getInstance().getTownOrNull(tb);
        if (town == null)
            return;

        onCancellableEvent(event, town, event.getPlayer(), true);
    }



    private void onTownyEvent(TownyActionEvent event, boolean isCooldownEvent) {
        if (event.getTownBlock() == null)
            return;
        try {
            onCancellableEvent(event, event.getTownBlock().getTown(), event.getPlayer(), isCooldownEvent);
        } catch (Exception e) {
            SiegeWar.severe("onTownyEvent: " + e.getLocalizedMessage());
        }
    }

    private void onCancellableEvent(Cancellable event, Town town, Player player, boolean isCooldownEvent) {
        if (player.getGameMode() == GameMode.CREATIVE)
            return;

        if (PillageController.getInstance().canPillage(town, player)) {

            if (!isCooldownEvent) {
                event.setCancelled(false);
                return;
            }
            if (isOnCooldown(player)) {
                player.sendMessage(pillageCooldownMsg(buildDestoryCooldownMap.getOrDefault(player.getUniqueId(), 0L), "destroy/build blocks"));
                event.setCancelled(true);
            } else {
                event.setCancelled(false);
                buildDestoryCooldownMap.put(player.getUniqueId(), System.nanoTime() + SiegeWarSettings.getBuildAndDestroyPillageCooldownNanoseconds());
            }
        }
    }

    private String pillageCooldownMsg(Long cooldown, String action) {
        return String.format("§cYou cannot %s in the pillage zone for §6%.2fs§c.",
                action, ((cooldown - System.nanoTime()) * 1.0e-9));
    }

    private boolean isOnCooldown(Player player) {
        return buildDestoryCooldownMap.getOrDefault(player.getUniqueId(), 0L) > System.nanoTime();
    }

    // Avoid storing players forever.
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if(!isOnCooldown(event.getPlayer()))
            buildDestoryCooldownMap.remove(event.getPlayer().getUniqueId());
    }

    // Avoid storing deleted town
    @EventHandler(ignoreCancelled = true)
    public void onTownDeleted(DeleteTownEvent event) {
        buildDestoryCooldownMap.remove(event.getTownUUID());
    }
}

package com.gmail.goosius.siegewar.listeners;

import com.gmail.goosius.siegewar.PillageController;
import com.gmail.goosius.siegewar.SiegeWar;
import com.gmail.goosius.siegewar.events.SiegeEndEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.palmergames.bukkit.towny.event.actions.TownyActionEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.actions.TownySwitchEvent;

public class MvndiPillageListener implements Listener {
    
    public MvndiPillageListener() {}

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
        onTownyEvent(event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onBuildInPillagingTown(TownyBuildEvent event) {
        onTownyEvent(event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onBuildInPillagingTown(TownyDestroyEvent event) {
        onTownyEvent(event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onBuildInPillagingTown(TownyItemuseEvent event) {
        onTownyEvent(event);
    }

    private void onTownyEvent(TownyActionEvent event) {
        if(!event.isCancelled()){
            return;
        }

        try {
            if (PillageController.getInstance().canPillage(event.getTownBlock().getTown())) {
                event.setCancelled(false);
            }
        } catch (Exception e) {
            SiegeWar.severe("onBuildInPillagingTown: " + e.getLocalizedMessage());
        }
    }
}

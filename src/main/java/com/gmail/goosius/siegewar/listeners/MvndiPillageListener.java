package com.gmail.goosius.siegewar.listeners;

import com.gmail.goosius.siegewar.SiegeWar;
import com.gmail.goosius.siegewar.events.SiegeEndEvent;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.palmergames.bukkit.towny.event.actions.TownyActionEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.actions.TownySwitchEvent;
import com.palmergames.bukkit.towny.object.Town;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MvndiPillageListener implements Listener {
    private Map<Town, Long> pillagingTowns;
    public MvndiPillageListener() {
        pillagingTowns = new ConcurrentHashMap<>();
    }
    @EventHandler(ignoreCancelled = true)
    public void onSiegeEnd(SiegeEndEvent event) {
        if ("ATTACKERS".equals(event.getSiegeWinner())) {
            startPillaging(event.getSiege().getTown());
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
            if (canPillage(event.getTownBlock().getTown())) {
                event.setCancelled(false);
            }
        } catch (Exception e) {
            SiegeWar.severe("onBuildInPillagingTown: " + e.getLocalizedMessage());
        }
    }

    private void startPillaging(Town town) {
        SiegeWar.info("Starting pillaging for " + town.getName());
        pillagingTowns.put(town, System.currentTimeMillis());
    }

    private void endPillaging(Town town) {
        SiegeWar.info("Stopping pillaging for " + town.getName());
        pillagingTowns.remove(town);
    }

    public boolean canPillage(Town town) {
        int siegeDurationPillage = SiegeWarSettings.getSiegeDurationPillage();
        // No pillage
        if(siegeDurationPillage <= 0) {
            return false;
        }

        // No pillage for that town
        long siegeEndedTime = pillagingTowns.getOrDefault(town, 0L);
        if (siegeEndedTime == 0) {
            return false;
        }
        
        long siegeDurationPillageMs = 60L * 1000L * siegeDurationPillage;

        // Pillage ended since last check
        if(siegeEndedTime + siegeDurationPillageMs < System.currentTimeMillis()) {
            endPillaging(town);
            return false;
        }
        // Pillage still active
        return true;
    }
}

package com.gmail.goosius.siegewar;

import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.palmergames.bukkit.towny.object.Town;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PillageController {
    private Map<Town, Long> pillagingTowns;
    private static PillageController instance;
    private PillageController() {
        pillagingTowns = new ConcurrentHashMap<>();
    }

    public static PillageController getInstance() {
        if(instance == null) {
            instance = new PillageController();
        }
        return instance;
    }


    public void startPillaging(Town town, int time) {
        SiegeWar.info("Starting pillaging for " + town.getName());
        pillagingTowns.put(town, System.currentTimeMillis());
        Bukkit.getAsyncScheduler().runDelayed(SiegeWar.getSiegeWar(), t -> endPillaging(town), time, TimeUnit.MINUTES);
    }

    public void startPillaging(Town town) {
        startPillaging(town, SiegeWarSettings.getSiegeDurationPillage());
    }

    public void endPillaging(Town town) {
        SiegeWar.info("Stopping pillaging for " + town.getName());
        pillagingTowns.remove(town);
    }

    public boolean canPillage(Town town, Player player) {
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
        if (siegeEndedTime + siegeDurationPillageMs < System.currentTimeMillis()) {
            endPillaging(town);
            return false;
        }
        // Pillage still active
        return true;
    }

}
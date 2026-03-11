package com.gmail.goosius.siegewar;

import com.gmail.goosius.siegewar.objects.Siege;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.palmergames.bukkit.towny.object.Town;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import com.palmergames.bukkit.towny.object.Translatable;

public class PillageController {
    private Map<Town, Long> pillagingTowns;
    private Map<Town, Integer> attackerKills;
    private Map<Town, Integer> defenderKills;
    private static PillageController instance;
    private PillageController() {
        pillagingTowns = new ConcurrentHashMap<>();
        attackerKills = new ConcurrentHashMap<>();
        defenderKills = new ConcurrentHashMap<>();
    }

    public static PillageController getInstance() {
        if(instance == null) {
            instance = new PillageController();
        }
        return instance;
    }


    public void startPillaging(Town town, int time) {
        SiegeWar.info("Starting pillaging of " + town.getName() + " for " + time + " minutes");
        if(time > 0) {
            Messaging.sendGlobalMessage(Translatable.of("msg_siege_war_pillage_started", town.getName(), time));
            pillagingTowns.put(town, System.currentTimeMillis() + (time * 60L * 1000L));
            Bukkit.getAsyncScheduler().runDelayed(SiegeWar.getSiegeWar(), t -> endPillaging(town), time, TimeUnit.MINUTES);
        } else {
            Messaging.sendGlobalMessage(Translatable.of("msg_siege_war_pillage_not_started", town.getName()));
        }
    }

    public void startPillaging(Siege siege) {
        int ratio = getTimeFromRatio(siege);
        startPillaging(siege.getTown(), ratio);
    }

    public void endPillaging(Town town) {
        Messaging.sendGlobalMessage(Translatable.of("msg_siege_war_pillage_ended", town.getName()));
        SiegeWar.info("Ending pillaging of " + town.getName());
        pillagingTowns.remove(town);
        attackerKills.remove(town);
        defenderKills.remove(town);
    }

    public boolean canPillage(Town town) {
        // No pillage for that town
        long siegePillageEndTime = pillagingTowns.getOrDefault(town, 0L);
        if (siegePillageEndTime == 0) {
            return false;
        }
        
        // Pillage ended since last check
        if (siegePillageEndTime < System.currentTimeMillis()) {
            endPillaging(town);
            return false;
        }
        // Pillage still active
        return true;
    }

    private int getTimeFromRatio(Siege siege) {
        try {
            // between 0 & 5 ratio
            float ratio = Math.min(5F, Math.max(1, (float) attackerKills.getOrDefault(siege.getTown(), 1)) / Math.max(1, (float) (defenderKills.getOrDefault(siege.getTown(), 1))));
            SiegeWar.info("Ratio for " + siege.getTown().getName() + " is " + ratio);
            return Math.round(SiegeWarSettings.getSiegeDurationPillage() * ratio);
        } catch (Exception e) {
            SiegeWar.getSiegeWar().getLogger().warning("Error calculating pillage time from ratio: " + siege.getTown().getName() + " using default value");
            return SiegeWarSettings.getSiegeDurationPillage();
        }
    }

    public void addAttackerKill(Town town) {
        attackerKills.merge(town, 1, Integer::sum);
        SiegeWar.info("Attacker kills for " + town.getName() + " is " + attackerKills.getOrDefault(town, -1));
    }

    public void addDefenderKill(Town town) {
        defenderKills.merge(town, 1, Integer::sum);
        SiegeWar.info("Defender kills for " + town.getName() + " is " + defenderKills.getOrDefault(town, -1));
    }

}
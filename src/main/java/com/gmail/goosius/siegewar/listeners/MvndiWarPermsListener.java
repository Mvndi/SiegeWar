package com.gmail.goosius.siegewar.listeners;

import com.gmail.goosius.siegewar.events.BattleSessionEndedEvent;
import com.gmail.goosius.siegewar.events.BattleSessionStartedEvent;
import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MvndiWarPermsListener implements Listener {
    private boolean previousForceFire;
    private boolean previousJailing;

    public MvndiWarPermsListener() {}

    @EventHandler(ignoreCancelled = true)
    public void onBattleSessionStarted(BattleSessionStartedEvent event) {
        for (World world : Bukkit.getWorlds()) {
            if (TownyAPI.getInstance().isTownyWorld(world)) {
                previousForceFire = TownyAPI.getInstance().getTownyWorld(world).isForceFire();
                previousJailing = TownyAPI.getInstance().getTownyWorld(world).isJailingEnabled();
                TownyAPI.getInstance().getTownyWorld(world).setForceFire(true);
                TownyAPI.getInstance().getTownyWorld(world).setJailingEnabled(false);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBattleSessionEnded(BattleSessionEndedEvent event) {
        for (World world : Bukkit.getWorlds()) {
            if (TownyAPI.getInstance().isTownyWorld(world)) {
                TownyAPI.getInstance().getTownyWorld(world).setForceFire(previousForceFire);
                TownyAPI.getInstance().getTownyWorld(world).setJailingEnabled(previousJailing);
            }
        }
    }
}

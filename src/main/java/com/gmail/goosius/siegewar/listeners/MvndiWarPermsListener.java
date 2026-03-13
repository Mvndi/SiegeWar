package com.gmail.goosius.siegewar.listeners;

import com.gmail.goosius.siegewar.SiegeWarAPI;
import com.gmail.goosius.siegewar.events.BattleSessionEndedEvent;
import com.gmail.goosius.siegewar.events.BattleSessionStartedEvent;
import com.palmergames.bukkit.towny.TownyAPI;
import java.util.LinkedList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;

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

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        World world = event.getBlock().getWorld();
        // Fire spread but does not burn block in town.
        if (TownyAPI.getInstance().isTownyWorld(world) && SiegeWarAPI.isBattleSessionActive()
                && !TownyAPI.getInstance().isWilderness(event.getBlock().getLocation())) {
            event.setCancelled(true);
            // Remove fire from all the next blocks else it spread endlessly
            for (Block block : getNextBlocks(event.getBlock())) {
                if (block.getType() == Material.FIRE) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

    private List<Block> getNextBlocks(Block block) {
        List<Block> nextBlocks = new LinkedList<>();
        // 1 block up
        nextBlocks.add(block.getRelative(BlockFace.UP));
        nextBlocks.addAll(getSideBlocks(block.getRelative(BlockFace.UP)));
        // same level
        nextBlocks.addAll(getSideBlocks(block));
        // 1 block down
        nextBlocks.add(block.getRelative(BlockFace.DOWN));
        nextBlocks.addAll(getSideBlocks(block.getRelative(BlockFace.DOWN)));
        return nextBlocks;
    }
    private List<Block> getSideBlocks(Block block) {
        return List.of(block.getRelative(BlockFace.NORTH), block.getRelative(BlockFace.SOUTH), block.getRelative(BlockFace.WEST),
                block.getRelative(BlockFace.EAST), block.getRelative(BlockFace.NORTH_WEST), block.getRelative(BlockFace.NORTH_EAST),
                block.getRelative(BlockFace.SOUTH_WEST), block.getRelative(BlockFace.SOUTH_EAST));
    }
}

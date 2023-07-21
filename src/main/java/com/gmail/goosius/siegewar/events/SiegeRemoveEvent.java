package com.gmail.goosius.siegewar.events;

import com.gmail.goosius.siegewar.objects.Siege;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is triggered after the town is saved and the siege is removed.
 * Note: If end-users want names of the town/attacker/defender,
 *       they should be careful about getting the name via town/nation objects,
 *       as these towns/nations may disappear after the siege.
 *       Rather they should use:
 *       event.getSiege().getAttackerName()
 *       event.getSiege().getDefenderName()
 *       event.getBesiegedTownName()
 */
public class SiegeRemoveEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Siege siege;
    private final String siegeType;
    private final String siegeWinner;
    private final String attackerName;
    private final String defenderName;
    private final String besiegedTownName;

    public SiegeRemoveEvent(Siege siege) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.siege = siege;
        this.siegeType = siege.getSiegeType().getName();
        this.siegeWinner = siege.getSiegeWinner().name();
        this.attackerName = siege.getAttackerName();
        this.defenderName = siege.getDefenderName();
        this.besiegedTownName = siege.getTown().getName();
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * @return Siege object
     */
    public Siege getSiege() {
        return siege;
    }

    /**
     * @return the siegeType as a String
     */
    public String getSiegeType() {
        return siegeType;
    }

    /**
     * @return the siegeWinner as a String
     */
    public String getSiegeWinnerName() {
        return siegeWinner;
    }

    /**
     * @return the attackerName
     */
    public String getAttackerName() {
        return attackerName;
    }

    /**
     * @return besiegedTownName;
     */
    public String getBesiegedTownName() {
        return besiegedTownName;
    }

    /**
     * @return the defenderName
     */
    public String getDefenderName() {
        return defenderName;
    }
}

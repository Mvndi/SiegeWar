package com.gmail.goosius.siegewar.events;

import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class SiegeCostCalculationEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private static final String INVALID = "Siege cost must be finite and nonnegative.";
    private final Town town;
    private double cost;

    public SiegeCostCalculationEvent(Town town, double cost) {
        super(!Bukkit.isPrimaryThread());
        this.town = town;
        setCost(cost);
    }

    public Town getTown() {
        return town;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double value) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(INVALID);
        }
        cost = value;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

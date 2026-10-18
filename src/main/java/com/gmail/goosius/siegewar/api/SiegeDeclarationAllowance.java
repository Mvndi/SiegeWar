package com.gmail.goosius.siegewar.api;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

public interface SiegeDeclarationAllowance {
    boolean isAvailable(Nation attacker, Town defender);

    void consume(Nation attacker, Town defender);
}

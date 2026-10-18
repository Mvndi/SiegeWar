package com.gmail.goosius.siegewar.utils;

import com.gmail.goosius.siegewar.api.SiegeDeclarationAllowance;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

public final class SiegeDeclarationLimits {
    private static final String EXHAUSTED = "No siege declaration allowance remains.";

    private SiegeDeclarationLimits() {}

    public static boolean isAtLimit(Nation attacker, Town defender) {
        return SiegeWarSettings.doesThisNationHaveTooManyActiveSieges(attacker)
                && allowance(attacker, defender).isEmpty();
    }

    public static void consumeIfRequired(Nation attacker, Town defender) {
        if (SiegeWarSettings.doesThisNationHaveTooManyActiveSieges(attacker)) {
            allowance(attacker, defender)
                    .orElseThrow(() -> new IllegalStateException(EXHAUSTED))
                    .consume(attacker, defender);
        }
    }

    private static Optional<SiegeDeclarationAllowance> allowance(Nation attacker, Town defender) {
        return Bukkit.getServicesManager()
                .getRegistrations(SiegeDeclarationAllowance.class)
                .stream()
                .map(RegisteredServiceProvider::getProvider)
                .filter(provider -> provider.isAvailable(attacker, defender))
                .findFirst();
    }
}

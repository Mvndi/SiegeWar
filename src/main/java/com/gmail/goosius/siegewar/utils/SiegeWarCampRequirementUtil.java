package com.gmail.goosius.siegewar.utils;

import com.gmail.goosius.siegewar.Messaging;
import com.gmail.goosius.siegewar.SiegeController;
import com.gmail.goosius.siegewar.enums.SiegeStatus;
import com.gmail.goosius.siegewar.objects.Siege;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.palmergames.bukkit.towny.object.Translatable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class SiegeWarCampRequirementUtil {

	private static final String SIEGE_CAMP_METADATA_KEY = "siege_camp_center_block";

	public static void endSiegesWithoutCamp() {
		List<DayOfWeek> daysWithoutCamp = SiegeWarSettings.getDaysWithoutCamp();
		if (daysWithoutCamp.isEmpty() || daysWithoutCamp.contains(LocalDate.now().getDayOfWeek()))
			return;

		for (Siege siege : SiegeController.getSieges()) {
			if (siege.getStatus() != SiegeStatus.IN_PROGRESS || hasSiegeCamp(siege))
				continue;

			Messaging.sendGlobalMessage(Translatable.of("msg_siege_ended_no_camp", siege.getTown().getName()));
			siege.setSiegeBalance(Math.min(siege.getSiegeBalance(), 0));
			SiegeController.endSiegeWithTimedWin(siege);
		}
	}

	private static boolean hasSiegeCamp(Siege siege) {
		return siege.getTown().hasMeta(SIEGE_CAMP_METADATA_KEY);
	}
}

package com.gmail.goosius.siegewar.timeractions;

import com.gmail.goosius.siegewar.enums.SiegeSide;
import com.gmail.goosius.siegewar.metadata.NationMetaDataController;
import com.gmail.goosius.siegewar.objects.Siege;
import com.gmail.goosius.siegewar.settings.SiegeWarSettings;
import com.gmail.goosius.siegewar.utils.SiegeWarMoneyUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarSiegeCompletionUtil;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

/**
 * This class is responsible for processing all types of attacker wins
 *
 * @author Goosius
 */
public class AttackerWin {

	/**
	 * This method sets up the attacker as the siege winner
	 * SiegeStatus will already have been set
	 *
	 * @param siege the siege
	 */
	public static void attackerWin(Siege siege) {
		siege.setSiegeWinner(SiegeSide.ATTACKERS);
		SiegeWarSiegeCompletionUtil.setCommonSiegeCompletionValues(siege);
		SiegeWarMoneyUtil.handleWarChest(siege, siege.getAttacker());
		recordTownWeekSiegeWin(siege);
	}

	private static void recordTownWeekSiegeWin(Siege siege) {
		if (!SiegeWarSettings.isCapitalSiegeWinRequirementEnabled())
			return;

		Town town = siege.getTown();
		Nation defenderNation = town.getNationOrNull();
		if (defenderNation == null || defenderNation.getCapital().equals(town))
			return;

		if (!(siege.getAttacker() instanceof Nation attackerNation) || defenderNation.equals(attackerNation))
			return;

		NationMetaDataController.incrementTownWeekSiegeWins(attackerNation, defenderNation);
	}
}

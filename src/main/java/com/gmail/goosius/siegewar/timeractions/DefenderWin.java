package com.gmail.goosius.siegewar.timeractions;

import com.gmail.goosius.siegewar.enums.SiegeSide;
import com.gmail.goosius.siegewar.enums.SiegeType;
import com.gmail.goosius.siegewar.metadata.NationMetaDataController;
import com.palmergames.bukkit.towny.object.Nation;
import java.time.LocalDate;
import com.gmail.goosius.siegewar.objects.Siege;
import com.gmail.goosius.siegewar.utils.SiegeWarMoneyUtil;
import com.gmail.goosius.siegewar.utils.SiegeWarSiegeCompletionUtil;

/**
 * This class is responsible for processing siege defender wins
 *
 * @author Goosius
 */
public class DefenderWin
{
	/**
	 * This method triggers siege values to be updated for a defender win
	 * SiegeStatus will already have been set
	 *
	 * @param siege the siege
	 */
    public static void defenderWin(Siege siege) {
        if (siege.getSiegeWinner() != SiegeSide.DEFENDERS
                && siege.getSiegeType() == SiegeType.CONQUEST && siege.getAttacker() instanceof Nation nation)
            NationMetaDataController.recordAttackLoss(nation, LocalDate.now());
    	siege.setSiegeWinner(SiegeSide.DEFENDERS);
		SiegeWarSiegeCompletionUtil.setCommonSiegeCompletionValues(siege);
		SiegeWarMoneyUtil.handleWarChest(siege, siege.getTown());
    }

}

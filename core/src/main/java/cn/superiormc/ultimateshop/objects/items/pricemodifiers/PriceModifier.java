package cn.superiormc.ultimateshop.objects.items.pricemodifiers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

public interface PriceModifier {

    BigDecimal getMultiplier(Player player, ItemStack itemStack, BigDecimal currentPrice);

    default BigDecimal getMultiplier(Player player,
                                     ItemStack itemStack,
                                     BigDecimal currentPrice,
                                     int tradeAmount) {
        return getMultiplier(player, itemStack, currentPrice);
    }
}

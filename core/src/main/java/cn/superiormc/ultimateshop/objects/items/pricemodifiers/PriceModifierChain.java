package cn.superiormc.ultimateshop.objects.items.pricemodifiers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PriceModifierChain {

    private final List<PriceModifier> modifiers;

    public PriceModifierChain(List<PriceModifier> modifiers) {
        this.modifiers = modifiers == null ? Collections.emptyList() : new ArrayList<>(modifiers);
    }

    public BigDecimal getMultiplier(Player player, ItemStack itemStack, BigDecimal basePrice) {
        return getMultiplier(player, itemStack, basePrice, 1);
    }

    public BigDecimal getMultiplier(Player player,
                                    ItemStack itemStack,
                                    BigDecimal basePrice,
                                    int tradeAmount) {
        BigDecimal multiplier = BigDecimal.ONE;
        BigDecimal currentPrice = basePrice.max(BigDecimal.ZERO);
        for (PriceModifier modifier : modifiers) {
            BigDecimal nextMultiplier = modifier.getMultiplier(
                    player, itemStack, currentPrice, Math.max(1, tradeAmount));
            if (nextMultiplier == null) {
                continue;
            }
            nextMultiplier = nextMultiplier.max(BigDecimal.ZERO);
            multiplier = multiplier.multiply(nextMultiplier);
            currentPrice = currentPrice.multiply(nextMultiplier);
        }
        return multiplier;
    }

    public List<PriceModifier> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }
}

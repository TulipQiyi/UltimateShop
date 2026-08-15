package cn.superiormc.ultimateshop.objects.items.pricemodifiers;

import cn.superiormc.ultimateshop.utils.MathUtil;
import cn.superiormc.ultimateshop.utils.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.math.BigDecimal;

public class DurabilityPriceModifier implements PriceModifier {

    private final ConfigurationSection section;

    public DurabilityPriceModifier(ConfigurationSection section) {
        this.section = section;
    }

    @Override
    public BigDecimal getMultiplier(Player player, ItemStack itemStack, BigDecimal currentPrice) {
        if (itemStack == null || itemStack.getType().getMaxDurability() <= 0
                || !(itemStack.getItemMeta() instanceof Damageable damageable)) {
            return BigDecimal.ONE;
        }

        BigDecimal maxDurability = BigDecimal.valueOf(itemStack.getType().getMaxDurability());
        BigDecimal damage = BigDecimal.valueOf(Math.max(0, damageable.getDamage()));
        BigDecimal remainingRatio = BigDecimal.ONE.subtract(damage.divide(maxDurability, 12, java.math.RoundingMode.HALF_UP));
        remainingRatio = remainingRatio.max(BigDecimal.ZERO).min(BigDecimal.ONE);

        BigDecimal coefficient = getDecimal(player, "deduction-coefficient", "1");
        BigDecimal multiplier = BigDecimal.ONE.subtract(BigDecimal.ONE.subtract(remainingRatio).multiply(coefficient));
        BigDecimal minimumMultiplier = getDecimal(player, "minimum-multiplier",
                section.getString("min-multiplier", "0"));
        multiplier = multiplier.max(minimumMultiplier);

        String minimumPriceValue = section.getString("minimum-price", section.getString("min-price"));
        if (minimumPriceValue != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal minimumPrice = parseDecimal(player, minimumPriceValue);
            multiplier = multiplier.max(minimumPrice.divide(currentPrice, 12, java.math.RoundingMode.HALF_UP));
        }
        return multiplier.max(BigDecimal.ZERO);
    }

    private BigDecimal getDecimal(Player player, String path, String defaultValue) {
        return parseDecimal(player, section.getString(path, defaultValue));
    }

    private BigDecimal parseDecimal(Player player, String value) {
        return MathUtil.doCalculate(TextUtil.withPAPI(value, player));
    }
}

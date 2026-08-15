package cn.superiormc.ultimateshop.objects.items.pricemodifiers;

import cn.superiormc.ultimateshop.utils.CommonUtil;
import cn.superiormc.ultimateshop.utils.NBTUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

public class NbtPriceModifier extends NumericValuePriceModifier {

    private final String key;

    private final String valueType;

    public NbtPriceModifier(ConfigurationSection section) {
        super(section);
        this.key = section.getString("key", "");
        this.valueType = section.getString("value-type", "AUTO");
    }

    @Override
    public BigDecimal getMultiplier(Player player, ItemStack itemStack, BigDecimal currentPrice) {
        return getMultiplier(player, itemStack, currentPrice, 1);
    }

    @Override
    public BigDecimal getMultiplier(Player player,
                                    ItemStack itemStack,
                                    BigDecimal currentPrice,
                                    int tradeAmount) {
        if (key.isEmpty() || itemStack == null || !CommonUtil.checkPluginLoad("NBTAPI")) {
            return BigDecimal.ONE;
        }
        Object value = NBTUtil.getNBTValue(itemStack, key, valueType);
        BigDecimal parsedValue = parseNumber(value == null ? null : value.toString());
        return applyValue(parsedValue, currentPrice, tradeAmount);
    }
}

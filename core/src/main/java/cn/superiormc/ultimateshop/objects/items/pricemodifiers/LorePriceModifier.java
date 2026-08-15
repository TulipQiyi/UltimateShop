package cn.superiormc.ultimateshop.objects.items.pricemodifiers;

import cn.superiormc.ultimateshop.UltimateShop;
import cn.superiormc.ultimateshop.utils.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class LorePriceModifier extends NumericValuePriceModifier {

    private final Pattern pattern;

    private final int valueGroup;

    private final boolean stripColor;

    public LorePriceModifier(ConfigurationSection section) {
        super(section);
        this.valueGroup = Math.max(0, section.getInt("value-group", 1));
        this.stripColor = section.getBoolean("strip-color", true);
        Pattern compiledPattern;
        try {
            int flags = section.getBoolean("case-sensitive", true) ? 0 : Pattern.CASE_INSENSITIVE;
            compiledPattern = Pattern.compile(section.getString("pattern",
                    "Item Value[：:]\\s*([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))"), flags);
        } catch (PatternSyntaxException ignored) {
            compiledPattern = null;
        }
        this.pattern = compiledPattern;
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
        if (pattern == null || itemStack == null || !itemStack.hasItemMeta()) {
            return BigDecimal.ONE;
        }
        ItemMeta meta = itemStack.getItemMeta();
        List<String> lore = UltimateShop.methodUtil.getItemLore(meta);
        if (lore == null) {
            return BigDecimal.ONE;
        }
        for (String line : lore) {
            if (line == null) {
                continue;
            }
            String text = stripColor ? TextUtil.clear(line) : line;
            Matcher matcher = pattern.matcher(text);
            if (!matcher.find() || valueGroup > matcher.groupCount()) {
                continue;
            }
            BigDecimal value = parseNumber(matcher.group(valueGroup));
            if (value != null) {
                return applyValue(value, currentPrice, tradeAmount);
            }
        }
        return BigDecimal.ONE;
    }
}

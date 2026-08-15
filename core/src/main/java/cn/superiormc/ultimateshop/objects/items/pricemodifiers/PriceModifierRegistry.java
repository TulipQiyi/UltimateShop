package cn.superiormc.ultimateshop.objects.items.pricemodifiers;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class PriceModifierRegistry {

    private static final Map<String, Function<ConfigurationSection, PriceModifier>> FACTORIES = new LinkedHashMap<>();

    static {
        register("durability", DurabilityPriceModifier::new);
        register("lore", LorePriceModifier::new);
        register("nbt", NbtPriceModifier::new);
        register("match_item", section -> new MythicChangerPriceModifier(section));
        // Legacy alias for configurations created before the type was renamed.
        register("mythic_changer", section -> new MythicChangerPriceModifier(section));
    }

    private PriceModifierRegistry() {
    }

    public static synchronized void register(String type,
                                             Function<ConfigurationSection, PriceModifier> factory) {
        if (type == null || type.isEmpty() || factory == null) {
            return;
        }
        FACTORIES.put(type.toLowerCase(Locale.ROOT), factory);
    }

    public static synchronized PriceModifier create(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", true)) {
            return null;
        }
        String type = section.getString("type", section.getName()).toLowerCase(Locale.ROOT);
        Function<ConfigurationSection, PriceModifier> factory = FACTORIES.get(type);
        return factory == null ? null : factory.apply(section);
    }

    public static PriceModifierChain createChain(ConfigurationSection section) {
        List<PriceModifier> modifiers = new ArrayList<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                PriceModifier modifier = create(section.getConfigurationSection(key));
                if (modifier != null) {
                    modifiers.add(modifier);
                }
            }
        }
        return new PriceModifierChain(modifiers);
    }
}

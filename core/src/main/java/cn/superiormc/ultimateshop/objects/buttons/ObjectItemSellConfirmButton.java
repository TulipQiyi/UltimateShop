package cn.superiormc.ultimateshop.objects.buttons;

import cn.superiormc.ultimateshop.methods.Items.BuildItem;
import cn.superiormc.ultimateshop.objects.buttons.subobjects.ObjectDisplayItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ObjectItemSellConfirmButton extends AbstractButton {

    public ObjectItemSellConfirmButton(ConfigurationSection config) {
        super(config);
        this.type = ButtonType.ITEM_SELL_CONFIRM;
    }

    @Override
    public ObjectDisplayItemStack getDisplayItem(Player player, int multi) {
        return new ObjectDisplayItemStack(buildDisplayItem(player, "", "", "1", 0, 0));
    }

    public ItemStack buildDisplayItem(Player player,
                                      String price,
                                      String originalPrice,
                                      String multiplier,
                                      int itemAmount,
                                      int unsellableAmount) {
        ConfigurationSection displaySection = config == null ? null : config.getConfigurationSection("display-item");
        if (displaySection == null) {
            return ObjectDisplayItemStack.getAir().getItemStack();
        }
        return BuildItem.buildItemStack(player,
                displaySection,
                displaySection.getInt("amount", 1),
                "price", price,
                "original-price", originalPrice,
                "multiplier", multiplier,
                "item-amount", String.valueOf(itemAmount),
                "unsellable-amount", String.valueOf(unsellableAmount));
    }
}

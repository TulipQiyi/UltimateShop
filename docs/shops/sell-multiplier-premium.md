# 💰Sell Multiplier - Premium

{% hint style="info" %}
Sell chest and sell stick provide multiplier feature, their multiplier and this feature are independent of each other, and the final price result will be superimposed.
{% endhint %}

`sell.multiplier` is a global sell bonus system. You can use it as tax for all players or bonus for VIP players. You can found it's config at `config.yml` file.

```yaml
sell:
  # Premium version only
  multiplier:
    enabled: false
    display-original-price: true
    # Support value: MAX, STACK
    # MAX mode: will use the maximum value as the result
    # STACK mode: As long as the player meets the conditions, it will be stacked and multiplied.
    mode: STACK
    value:
      default: 1
      rich: 0.9
      vip: 1.1
    value-conditions:
      # Tax
      rich:
        1:
          type: placeholder
          placeholder: '%vault_eco_balance%'
          rule: '>='
          value: 50000
      # Bonus for VIP
      vip:
        1:
          type: permission
          permission: 'group.vip'
```

* enabled: To use this feature, you have to make sure this option being set to `true`.
* display-original-price: If set to `false`, we will display price that has modified by the multiplier in shop GUI.
*   mode: Support value: **MAX** and **STACK**.

    * MAX: Use the biggest value among all matched multiplier entries and `default`.
    * STACK: Start from `default`, then multiply every matched value together.

    Example:

    * `default = 1`
    * `rich = 0.9`
    * `vip = 1.1`
    * Player matches both `rich` and `vip`
    * MAX mode final result = `1.1`
    * STACK mode final result = `1 * 0.9 * 1.1 = 0.99`
* value and value-conditions: The multiplier id in `value` must match the id in `value-conditions` exactly. Should use [Condition Format](../format/condition-format.md) in `value-conditions` option.

## Separate item-aware price modifiers

`sell.price-modifier` is independent from `sell.multiplier`. Its rules are global, but only products with `price-modifier: true` use them. Each matching modifier is merged into the transaction's final multiplier in configuration order.

```yaml
sell:
  price-modifier:
    item-sell-menu:
      # Products with `price-modifier: true` open this menu when clicked.
      enabled: true
      menu: item-sell
    durability:
      type: durability
      enabled: true
      deduction-coefficient: 1
      minimum-multiplier: 0.1
      # minimum-price: 1
    lore:
      type: lore
      enabled: false
      operation: SET
      pattern: 'Item Value[：:]\s*([+-]?(?:\d+(?:\.\d+)?|\.\d+))'
      value-group: 1
      strip-color: true
      minimum-value: 0
      maximum-value: 1000000
      maximum-number-length: 64
    nbt:
      type: nbt
      enabled: false
      operation: SET
      key: item_value
      value-type: AUTO
      minimum-value: 0
      maximum-value: 1000000
      maximum-number-length: 64
    mythic-changer:
      type: match_item
      enabled: false
      mode: STACK
      rules:
        named-item:
          operation: MULTIPLY
          value: 1.2
          match-item:
            has-name: true
        special-lore:
          operation: ADD
          value: 0.1
          match-item:
            contains-lore:
              - 'Special'
```

The durability modifier uses `1 - (lost durability ratio x deduction-coefficient)`. It never drops below `minimum-multiplier`; `minimum-price` can also set a floor for the current numeric price. Non-damageable items use `1`.

The Lore modifier searches each lore line with `pattern` and reads `value-group` from the first match. Formatting codes are removed when `strip-color` is true. `operation: SET` uses the captured number as the price of each sold trade unit, so stack splitting does not change the total, while `MULTIPLY` multiplies the current price by that number. Values outside the inclusive `minimum-value`/`maximum-value` range, values longer than `maximum-number-length`, and invalid or unmatched lore leave the price unchanged. Unsupported operations are reported as configuration errors and ignored instead of silently falling back to `MULTIPLY`.

The NBT modifier reads the scalar value at `key`; nested compounds use dot notation such as `custom.price`. `value-type` can be `AUTO`, `BYTE`, `SHORT`, `INT`, `LONG`, `FLOAT`, `DOUBLE`, or `STRING`. Numeric strings are accepted. It uses the same operations and numeric safety options as the Lore modifier and requires NBTAPI. Missing, non-finite, out-of-range, oversized, or invalid values leave the price unchanged.

The MythicChanger modifier uses MythicChanger `match-item` rules. `MULTIPLY` uses `value` as a multiplier, while `ADD` adds `value` to `1`. Unsupported rule operations are reported and the affected rule is ignored. Its mode supports `MAX`/`HIGHEST`, `MIN`/`LOWEST`, and `STACK`. If MythicChanger is unavailable, this modifier has no effect.

Enable item-aware pricing for a product in its shop configuration:

```yaml
price-modifier: true
```

Such a product exposes its dynamic sell price in the shop menu's added lore, calculated from its displayed item. Its sell price is hidden from normal sell APIs and all non-item-sell-menu flows, so it can only be sold through an item sell menu. When `item-sell-menu.enabled` is true, clicking the product in Java, Bedrock, or dialog shop menus opens the configured `item-sell-menu.menu` directly.


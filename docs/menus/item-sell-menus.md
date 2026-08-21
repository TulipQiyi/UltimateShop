# 💰Item Sell Menus

Item sell menus let players place item stacks into configured slots, preview their adjusted sell price, and confirm the sale with one button. Files are stored in `/item_sell_menus/` and can be opened with `/shop menu <menuName>`.

The default example is `item_sell_menus/item-sell.yml`.

## Menu structure

```yaml
menu-type: item-sell
title: 'Sell Items'
size: 54
max-sell-amount: -1

layout:
  - '000000000'
  - '0IIIIIII0'
  - '0IIIIIII0'
  - '0IIIIIII0'
  - '000000000'
  - '00B0C0X00'

input-item: 'I'
confirm-item: 'C'

confirm-button:
  display-item:
    material: EMERALD
    name: '&aConfirm Sale'
    lore:
      - '&7Price: &f{price}'
      - '&7Original price: &f{original-price}'
      - '&7Final multiplier: &fx{multiplier}'
      - '&7Sellable items: &f{item-amount}'
      - '&7Unsellable items: &f{unsellable-amount}'
      - '&aClick to sell.'
```

`input-item` is the layout marker for slots that accept player items. `confirm-item` is the marker for the confirmation button. `max-sell-amount` limits the total number of product trade units sold by one confirmation across all input slots; `-1` means unlimited. Unsold or unsupported items remain in the menu and are returned when it closes.

The confirmation display supports these placeholders:

* `{price}`: final price after the global sell multiplier and its item-aware modifiers
* `{original-price}`: product sell price before multipliers
* `{multiplier}`: the combined final sell multiplier
* `{item-amount}`: amount that can currently be sold
* `{unsellable-amount}`: amount that cannot currently be sold

The transaction still uses the normal product sell flow, including sell conditions, player/server limits, actions, events, logging, and the global sell multiplier. Item-aware price modifiers are configured globally under `sell.price-modifier` in `config.yml`; an item sell menu does not have its own modifier configuration.

Products with `price-modifier: true` use these rules. Their sell price is unavailable to other sell flows; enable `sell.price-modifier.item-sell-menu.enabled` and set `sell.price-modifier.item-sell-menu.menu` to redirect product clicks to an item sell menu.

## Price modifier API

Global item-aware price modifiers are created through `PriceModifierRegistry`. A custom implementation only needs to implement `PriceModifier` and register a factory:

```java
PriceModifierRegistry.register("my-modifier", MyPriceModifier::new);
```

Each modifier receives the player, the actual sold `ItemStack`, and the current numeric price. It returns a non-negative multiplier. The backward-compatible four-argument overload also receives the number of trade units when a modifier needs quantity-aware behavior such as `SET`. Modifiers declared under `sell.price-modifier` are applied in configuration order to every sell method.

## Durability modifier

```yaml
sell:
  price-modifier:
    durability:
      type: durability
      deduction-coefficient: 1
      minimum-multiplier: 0.1
      # minimum-price: 1
```

The formula is:

```text
1 - (lost durability ratio × deduction-coefficient)
```

The result never drops below `minimum-multiplier`. If `minimum-price` is configured, the modifier also prevents the current total numeric price from dropping below that value. Non-damageable items keep a multiplier of `1`.

## Lore value modifier

```yaml
sell:
  price-modifier:
    lore-value:
      type: lore
      enabled: true
      operation: SET
      pattern: 'Item Value[：:]\s*([+-]?(?:\d+(?:\.\d+)?|\.\d+))'
      value-group: 1
      strip-color: true
      case-sensitive: true
      minimum-value: 0
      maximum-value: 1000000
      maximum-number-length: 64
```

The first matching lore line supplies the number. `SET` makes that number the price of each sold trade unit, so selling a stack produces the same total as selling its contents separately. `MULTIPLY` multiplies the current price by the number. `value-group` selects the regular-expression capture group. Values outside the inclusive `minimum-value`/`maximum-value` range, values longer than `maximum-number-length`, and unmatched or invalid values leave the price unchanged. An unsupported operation is reported as a configuration error and the modifier is ignored instead of falling back to `MULTIPLY`.

## NBT value modifier

```yaml
sell:
  price-modifier:
    nbt-value:
      type: nbt
      enabled: true
      operation: SET
      key: item_value
      value-type: AUTO
      minimum-value: 0
      maximum-value: 1000000
      maximum-number-length: 64
```

The NBT modifier requires NBTAPI. `key` supports nested paths such as `custom.price`. `value-type` supports `AUTO`, `BYTE`, `SHORT`, `INT`, `LONG`, `FLOAT`, `DOUBLE`, and `STRING`; numeric strings are accepted. `SET`, `MULTIPLY`, and the numeric safety options behave the same as in the Lore modifier. Missing, non-finite, out-of-range, oversized, or invalid values leave the price unchanged.

## Match item modifier

This modifier uses MythicChanger `match-item` rules. If MythicChanger is unavailable, the modifier has no effect.

```yaml
sell:
  price-modifier:
    mythic-changer:
      type: match_item
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

Every matching rule produces a multiplier:

* `MULTIPLY`: uses `value` directly, so `1.2` means 120% of the current price.
* `ADD`: adds `value` to the multiplier, so `0.1` means 110%.

Unsupported rule operations are reported as configuration errors and the affected rule is ignored.

`mode` controls multiple matching rules:

* `MAX` or `HIGHEST`: use the highest resulting multiplier.
* `MIN` or `LOWEST`: use the lowest resulting multiplier.
* `STACK`: process matching rules in configuration order; `MULTIPLY` values multiply and `ADD` values add to the accumulated multiplier.

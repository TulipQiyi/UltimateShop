package cn.superiormc.ultimateshop.objects.items.pricemodifiers;

import cn.superiormc.ultimateshop.managers.ErrorManager;
import org.bukkit.configuration.ConfigurationSection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

abstract class NumericValuePriceModifier implements PriceModifier {

    private final Operation operation;

    private final BigDecimal minimumValue;

    private final BigDecimal maximumValue;

    private final int maximumNumberLength;

    protected NumericValuePriceModifier(ConfigurationSection section) {
        String configuredOperation = section.getString("operation",
                section.getString("mode", "MULTIPLY"));
        this.operation = Operation.parse(configuredOperation);
        if (operation == null) {
            reportInvalidOperation(section, configuredOperation, "SET or MULTIPLY");
        }
        this.minimumValue = parseConfiguredNumber(section.getString("minimum-value", "0"), BigDecimal.ZERO);
        BigDecimal configuredMaximum = parseConfiguredNumber(
                section.getString("maximum-value", "1000000"), new BigDecimal("1000000"));
        this.maximumValue = configuredMaximum.max(minimumValue);
        this.maximumNumberLength = Math.max(1, section.getInt("maximum-number-length", 64));
    }

    protected BigDecimal applyValue(BigDecimal value, BigDecimal currentPrice, int tradeAmount) {
        if (operation == null || !isAllowedValue(value)) {
            return BigDecimal.ONE;
        }
        if (operation == Operation.SET) {
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ONE;
            }
            BigDecimal targetPrice = value.multiply(BigDecimal.valueOf(Math.max(1, tradeAmount)));
            return targetPrice.divide(currentPrice, 12, RoundingMode.HALF_UP);
        }
        return value;
    }

    protected BigDecimal parseNumber(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replace(",", "");
        if (normalized.isEmpty() || normalized.length() > maximumNumberLength) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(normalized);
            return isAllowedValue(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isAllowedValue(BigDecimal value) {
        return value != null
                && value.compareTo(minimumValue) >= 0
                && value.compareTo(maximumValue) <= 0;
    }

    private static BigDecimal parseConfiguredNumber(String value, BigDecimal fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return new BigDecimal(value.trim().replace(",", ""));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static void reportInvalidOperation(ConfigurationSection section,
                                       String operation,
                                       String expected) {
        if (ErrorManager.errorManager == null) {
            return;
        }
        String path = section == null ? null : section.getCurrentPath();
        String location = path == null || path.isEmpty()
                ? (section == null ? "unknown price modifier" : section.getName())
                : path;
        ErrorManager.errorManager.sendErrorMessage("§cError: Invalid operation '" + operation
                + "' at " + location + ". Expected " + expected + ". This entry is ignored.");
    }

    private enum Operation {
        SET,
        MULTIPLY;

        private static Operation parse(String value) {
            if (value == null) {
                return null;
            }
            return switch (value.trim().toUpperCase(Locale.ROOT)) {
                case "SET", "DIRECT", "OVERRIDE" -> SET;
                case "MULTIPLY" -> MULTIPLY;
                default -> null;
            };
        }
    }
}

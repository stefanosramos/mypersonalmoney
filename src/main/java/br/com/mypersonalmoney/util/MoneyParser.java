package br.com.mypersonalmoney.util;

import java.math.BigDecimal;

public final class MoneyParser {

    private MoneyParser() {}

    /**
     * Parses Brazilian money formats like:
     * - "35,90"
     * - "1.234,56"
     * - "R$ 1.234,56"
     * Also accepts dot-decimal:
     * - "35.90"
     */
    public static BigDecimal parseBRL(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("amount is required");
        }

        String s = raw.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("amount is required");
        }

        // Remove currency symbol and spaces (including non-breaking spaces)
        s = s.replace("R$", "")
                .replace("\u00A0", "")
                .replace(" ", "");

        // If contains comma, treat as BR format: 1.234,56
        if (s.contains(",")) {
            s = s.replace(".", "");   // remove thousand separators
            s = s.replace(",", ".");  // comma becomes decimal separator
        }

        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid amount: " + raw);
        }
    }
}
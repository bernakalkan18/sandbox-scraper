package com.example.sandboxscraper.util;

import java.math.BigDecimal;

public class PriceUtils {
    // "$1,299.50" → 1299.50  ; "€49,90" → 49.90 (virgülü noktaya çevir)
    public static BigDecimal parsePrice(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("[^0-9,\\.]", "")
                .replace(",", ".");
        if (cleaned.isBlank()) return null;
        try {
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    public static String detectCurrency(String raw) {
        if (raw == null) return null;
        if (raw.contains("$")) return "USD";
        if (raw.contains("€")) return "EUR";
        if (raw.contains("£")) return "GBP";
        if (raw.contains("₺") || raw.toLowerCase().contains("tl")) return "TRY";
        return null;
    }
}

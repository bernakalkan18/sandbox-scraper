package com.example.sandboxscraper.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * String normalize yardımcı sınıfı.
 * - newline (\n, \r) ve NBSP'leri boşluğa çevirir
 * - fazla boşlukları teke indirir
 * - lower-case'e çevirir
 *
 * Böylece arama ve kategori eşleşmelerinde
 * "Action Adventure\nFantasy\nLinear" == "Action Adventure Fantasy Linear"
 * hale gelir.
 */
public final class Texts {

    private Texts() {}

    /** Genel normalize fonksiyonu */
    public static String norm(String s) {
        if (s == null) return "";
        String x = Normalizer.normalize(s, Normalizer.Form.NFKC)
                .replace('\u00A0', ' ')   // NBSP → boşluk
                .replace("\r", " ")
                .replace("\n", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        // Fazla boşlukları teke indir
        x = x.replaceAll("\\s+", " ");
        return x;
    }

    /** Kategori karşılaştırmaları için alias */
    public static String normCategory(String s) {
        return norm(s);
    }
}

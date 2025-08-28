package com.example.sandboxscraper.service;

import com.example.sandboxscraper.dto.ProductDTO;
import com.example.sandboxscraper.scraper.ProductScraper;
import com.example.sandboxscraper.scraper.SeleniumProductScraper;
import com.example.sandboxscraper.util.Texts;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tüm veriyi memory'de tutar + normalize ederek arama/filtre yapar.
 * DB yok, HashMap index var.
 */
@Service
public class ScraperService {

    // ---- In-memory cache ----
    // immutable snapshot tutuyoruz (volatile ile atomik swap)
    private volatile List<ProductDTO> all = List.of();

    // normalize edilmiş TAM kategori metni -> ürünler
    // örn key: "action adventure fantasy linear"
    private volatile Map<String, List<ProductDTO>> byFullCategoryNorm = Map.of();

    // arama için normalize edilmiş alan cache'leri (name/category)
    private volatile List<String> nameNormCache = List.of();
    private volatile List<String> categoryNormCache = List.of();

    // ---- Scraper seçimi ----
    private final ProductScraper scraper;   // şimdilik Selenium
    private final int maxPages = 5;         // istersen application.yml'a al

    // aynı anda birden fazla thread'in yükleme başlatmasını engellemek için
    private final Object refreshLock = new Object();

    public ScraperService(SeleniumProductScraper seleniumProductScraper) {
        this.scraper = Objects.requireNonNull(seleniumProductScraper);
    }

    // ---- Controller'ların çağırdığı API'ler ----
    public List<ProductDTO> getAll() throws Exception {
        ensureLoaded();
        return all; // immutable snapshot
    }

    public List<ProductDTO> getByCategory(String category) throws Exception {
        ensureLoaded();
        String key = Texts.normCategory(category);
        return byFullCategoryNorm.getOrDefault(key, List.of());
    }

    public List<ProductDTO> searchByName(String q) throws Exception {
        ensureLoaded();
        if (q == null || q.isBlank()) return List.of();
        String needle = Texts.norm(q);

        List<ProductDTO> out = new ArrayList<>();
        List<ProductDTO> snapshot = all; // volatile snapshot
        for (int i = 0; i < snapshot.size(); i++) {
            String n = nameNormCache.get(i);
            String c = categoryNormCache.get(i);
            if (n.contains(needle) || c.contains(needle)) {
                out.add(snapshot.get(i));
            }
        }
        return out;
    }

    public Map<String, List<ProductDTO>> getAllGrouped() throws Exception {
        ensureLoaded();
        // orijinal kategori metni ile gruplama (UI için okunaklı)
        return all.stream().collect(Collectors.groupingBy(
                ProductDTO::getCategory,
                TreeMap::new, // alfabetik key
                Collectors.toList()
        ));
    }

    public void refreshCache() throws Exception {
        List<ProductDTO> scraped = scraper.scrapeAll(maxPages);
        replaceAll(scraped);
        System.out.println("[ScraperService] cache refreshed: " + scraped.size());
    }

    public String currentEngine() {
        return scraper.getName();
    }

    // ---- İç yardımcılar ----
    private void ensureLoaded() throws Exception {
        if (!all.isEmpty()) return;
        synchronized (refreshLock) {
            if (all.isEmpty()) { // double-check
                System.out.println("[ScraperService] cache empty -> loading...");
                refreshCache(); // scrape + replaceAll
            }
        }
    }

    private void replaceAll(List<ProductDTO> fresh) {
        // immutable kopya + indexleri kur
        List<ProductDTO> copy = List.copyOf(fresh);

        Map<String, List<ProductDTO>> byCat = copy.stream()
                .collect(Collectors.groupingBy(
                        p -> Texts.normCategory(p.getCategory()),
                        Collectors.toUnmodifiableList()
                ));

        List<String> names = copy.stream().map(p -> Texts.norm(p.getName())).toList();
        List<String> cats  = copy.stream().map(p -> Texts.norm(p.getCategory())).toList();

        // atomik swap
        this.all = copy;
        this.byFullCategoryNorm = Map.copyOf(byCat);
        this.nameNormCache = names;
        this.categoryNormCache = cats;
    }
}

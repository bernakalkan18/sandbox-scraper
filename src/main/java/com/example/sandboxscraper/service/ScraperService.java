package com.example.sandboxscraper.service;

import com.example.sandboxscraper.dto.ProductDTO;
import com.example.sandboxscraper.scraper.ApiProductScraper;
import com.example.sandboxscraper.scraper.JsoupProductScraper;
import com.example.sandboxscraper.scraper.ProductScraper;
import com.example.sandboxscraper.scraper.SeleniumProductScraper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ScraperService {

    private final JsoupProductScraper jsoupScraper;
    private final SeleniumProductScraper seleniumScraper;
    private final ApiProductScraper apiScraper;

    private final Map<String, List<ProductDTO>> cache = new ConcurrentHashMap<>();
    private volatile Instant lastRefresh = Instant.EPOCH;

    @Value("${app.cache.ttlMinutes:10}")
    private long ttlMinutes;

    @Value("${app.maxPages:1}")
    private int maxPages;

    @Value("${app.scraper.engine:SELENIUM}")
    private String engine;

    public ScraperService(JsoupProductScraper jsoupScraper,
                          SeleniumProductScraper seleniumScraper,
                          ApiProductScraper apiScraper) {
        this.jsoupScraper = jsoupScraper;
        this.seleniumScraper = seleniumScraper;
        this.apiScraper = apiScraper;
    }

    private ProductScraper pick() {
        return switch (engine.toUpperCase(Locale.ROOT)) {
            case "JSOUP" -> jsoupScraper;
            case "API" -> apiScraper;
            default -> seleniumScraper; // SELENIUM
        };
    }

    public synchronized void refreshCache() throws Exception {
        ProductScraper scraper = pick();
        List<ProductDTO> all = scraper.scrapeAll(maxPages);
        Map<String, List<ProductDTO>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategory() == null || p.getCategory().isBlank() ? "Uncategorized" : p.getCategory(),
                        ConcurrentHashMap::new,
                        Collectors.toList()
                ));
        cache.clear();
        cache.putAll(grouped);
        lastRefresh = Instant.now();
    }

    private boolean isExpired() {
        if (ttlMinutes <= 0) return true;
        return lastRefresh.plusSeconds(ttlMinutes * 60).isBefore(Instant.now());
    }

    private void ensureCache() throws Exception {
        if (cache.isEmpty() || isExpired()) refreshCache();
    }

    public Map<String, List<ProductDTO>> getAllGrouped() throws Exception {
        ensureCache();
        return cache;
    }

    public List<ProductDTO> getByCategory(String category) throws Exception {
        ensureCache();
        return cache.getOrDefault(category, Collections.emptyList());
    }

    public List<ProductDTO> searchByName(String q) throws Exception {
        ensureCache();
        String needle = q.toLowerCase(Locale.ROOT);
        return cache.values().stream()
                .flatMap(List::stream)
                .filter(p -> p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(needle))
                .limit(200) // aşırı büyümesin
                .collect(Collectors.toList());
    }

    public String currentEngine() {
        return engine;
    }
}

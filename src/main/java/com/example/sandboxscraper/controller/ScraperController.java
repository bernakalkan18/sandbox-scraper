package com.example.sandboxscraper.controller;

import com.example.sandboxscraper.dto.ProductDTO;
import com.example.sandboxscraper.service.ScraperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController {

    private final ScraperService service;

    public ScraperController(ScraperService service) {
        this.service = service;
    }

    // Tüm ürünleri getir (düz liste)
    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> all() throws Exception {
        return ResponseEntity.ok(service.getAll());
    }

    // Gruplanmış ürünleri getir (kategori -> liste)
    @GetMapping("/products/grouped")
    public ResponseEntity<Map<String, List<ProductDTO>>> grouped() throws Exception {
        return ResponseEntity.ok(service.getAllGrouped());
    }

    // Belirli kategori
    @GetMapping("/products/{category}")
    public ResponseEntity<List<ProductDTO>> byCategory(@PathVariable String category) throws Exception {
        return ResponseEntity.ok(service.getByCategory(category));
    }

    // Arama
    @GetMapping("/products/search")
    public ResponseEntity<List<ProductDTO>> search(@RequestParam("q") String q) throws Exception {
        return ResponseEntity.ok(service.searchByName(q));
    }

    // Cache yenile
    @PostMapping("/products/refresh")
    public ResponseEntity<String> refresh() throws Exception {
        service.refreshCache();
        return ResponseEntity.ok("Cache refreshed using " + service.currentEngine());
    }

    // Aktif engine
    @GetMapping("/engine")
    public ResponseEntity<String> engine() {
        return ResponseEntity.ok(service.currentEngine());
    }
}

package com.example.sandboxscraper.controller;

import com.example.sandboxscraper.dto.ProductDTO;
import com.example.sandboxscraper.service.ScraperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ScraperController {

    private final ScraperService service;

    public ScraperController(ScraperService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<ProductDTO>>> all() throws Exception {
        return ResponseEntity.ok(service.getAllGrouped());
    }

    @GetMapping("/{category}")
    public ResponseEntity<List<ProductDTO>> byCategory(@PathVariable String category) throws Exception {
        return ResponseEntity.ok(service.getByCategory(category));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> search(@RequestParam("q") String q) throws Exception {
        return ResponseEntity.ok(service.searchByName(q));
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh() throws Exception {
        service.refreshCache();
        return ResponseEntity.ok("refreshed using engine=" + service.currentEngine());
    }
}

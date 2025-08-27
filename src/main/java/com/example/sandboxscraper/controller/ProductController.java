package com.example.sandboxscraper.controller;

import com.example.sandboxscraper.dto.ProductDTO;
import com.example.sandboxscraper.service.ScraperService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ScraperService scraperService;

    public ProductController(ScraperService scraperService) {
        this.scraperService = scraperService;
    }

    // Tüm ürünler (düz liste)
    @GetMapping
    public List<ProductDTO> getAll() throws Exception {
        return scraperService.getAll();
    }

    // Cache yenile
    @PostMapping("/refresh")
    public String refresh() throws Exception {
        scraperService.refreshCache();
        return "refreshed using " + scraperService.currentEngine();
    }

    // Arama
    @GetMapping("/search")
    public List<ProductDTO> search(@RequestParam("q") String q) throws Exception {
        return scraperService.searchByName(q);
    }

    // Kategoriye göre
    @GetMapping("/{category}")
    public List<ProductDTO> getByCategory(@PathVariable String category) throws Exception {
        return scraperService.getByCategory(category);
    }
}

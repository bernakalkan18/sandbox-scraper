package com.example.sandboxscraper.scraper;

import com.example.sandboxscraper.dto.ProductDTO;
import java.util.List;

public interface ProductScraper {
    List<ProductDTO> scrapeAll(int maxPages) throws Exception;
    String getName();
}

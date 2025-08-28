package com.example.sandboxscraper.scraper;

import com.example.sandboxscraper.dto.ProductDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class ApiProductScraper implements com.example.sandboxscraper.scraper.ProductScraper {

    @Value("${app.demoApiUrl}")
    private String apiUrl;

    @Value("${app.maxPages}")
    private int maxPages;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<ProductDTO> scrapeAll(int maxPagesParam) throws Exception {
        int pages = Math.min(maxPagesParam, maxPages);
        List<ProductDTO> out = new ArrayList<>();

        for (int page = 1; page <= pages; page++) {
            String url = apiUrl + "?page=" + page;

            ResponseEntity<String> resp = rest.getForEntity(url, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) continue;

            JsonNode root = mapper.readTree(resp.getBody());
            JsonNode items = root.path("items").isMissingNode() ? root.path("data") : root.path("items");

            if (items.isArray()) {
                for (JsonNode n : items) {
                    String name = text(n, "name", "title");
                    String priceRaw = text(n, "price", "price_text", "price_str");
                    String category = text(n, "category", "category_name");
                    String detailUrl = text(n, "url", "detail_url", "link");

                    double price = 0.0;
                    if (priceRaw != null && !priceRaw.isBlank()) {
                        priceRaw = priceRaw.replaceAll("[^0-9.,]", "").replace(",", ".");
                        try {
                            price = Double.parseDouble(priceRaw);
                        } catch (NumberFormatException ignored) {}
                    }

                    if (name != null) {
                        out.add(new ProductDTO(name, category, price, detailUrl));
                    }
                }
            }
        }
        return out;
    }

    private static String text(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.path(k);
            if (!v.isMissingNode() && !v.isNull() && !v.asText().isBlank()) return v.asText();
        }
        return null;
    }

    @Override
    public String getName() {
        return "API";
    }
}

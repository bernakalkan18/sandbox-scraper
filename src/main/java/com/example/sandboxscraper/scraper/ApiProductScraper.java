package com.example.sandboxscraper.scraper;

import com.example.sandboxscraper.dto.ProductDTO;
import com.example.sandboxscraper.util.PriceUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.internal.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ApiProductScraper implements ProductScraper {

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
            // API endpoint yapısı değişebilir; {page} gibi parametre varsa burada formatla
            String url = apiUrl + "?page=" + page;

            ResponseEntity<String> resp = rest.getForEntity(url, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) continue;

            JsonNode root = mapper.readTree(resp.getBody());

            // Demo API’nin field’ları değişebilir:
            // Aşağıda sık görülen isimlere göre esnek okuma yapıyoruz.
            JsonNode items = root.path("items").isMissingNode() ? root.path("data") : root.path("items");
            if (items.isArray()) {
                for (JsonNode n : items) {
                    String id = text(n, "id", "sku", "product_id");
                    String name = text(n, "name", "title");
                    String priceRaw = text(n, "price", "price_text", "price_str");
                    String category = text(n, "category", "category_name");
                    String detailUrl = text(n, "url", "detail_url", "link");
                    String imageUrl = text(n, "image", "image_url");
                    Double rating = number(n, "rating", "stars");

                    BigDecimal price = PriceUtils.parsePrice(priceRaw);
                    String currency = PriceUtils.detectCurrency(priceRaw);

                    if (name != null) {
                        out.add(new ProductDTO(id, name, price, currency, category, detailUrl, imageUrl, rating, getName()));
                    }
                }
            } else if (root.isArray()) {
                for (JsonNode n : root) {
                    String name = text(n, "name", "title");
                    String priceRaw = text(n, "price", "price_text");
                    String category = text(n, "category");
                    out.add(new ProductDTO(null, name, PriceUtils.parsePrice(priceRaw),
                            PriceUtils.detectCurrency(priceRaw), category, null, null, null, getName()));
                }
            }
        }
        return out;
    }

    private static String text(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.path(k);
            if (!v.isMissingNode() && !v.isNull() && !StringUtil.isBlank(v.asText())) return v.asText();
        }
        return null;
    }

    private static Double number(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.path(k);
            if (v.isNumber()) return v.asDouble();
        }
        return null;
    }

    @Override
    public String getName() {
        return "API";
    }
}

package com.example.sandboxscraper.scraper;

import com.example.sandboxscraper.dto.ProductDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JsoupProductScraper implements ProductScraper {

    @Value("${app.target.baseUrl}")
    private String baseUrl;

    @Value("${app.userAgent}")
    private String userAgent;

    @Value("${app.timeoutMs}")
    private int timeoutMs;

    @Value("${app.requestDelayMs}")
    private long delayMs;

    @Value("${app.selectors.product}")
    private String selProduct;

    @Value("${app.selectors.title}")
    private String selTitle;

    @Value("${app.selectors.price}")
    private String selPrice;

    @Value("${app.selectors.category}")
    private String selCategory;

    @Value("${app.selectors.link}")
    private String selLink;

    @Value("${app.selectors.next}")
    private String selNext;

    @Override
    public List<ProductDTO> scrapeAll(int maxPages) throws Exception {
        List<ProductDTO> out = new ArrayList<>();
        String url = baseUrl;
        int page = 1;

        while (page <= maxPages && url != null) {
            Document doc = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(timeoutMs)
                    .get();

            Elements cards = doc.select(selProduct);
            for (Element card : cards) {
                String title = card.selectFirst(selTitle) != null ? card.selectFirst(selTitle).text() : null;
                String priceRaw = card.selectFirst(selPrice) != null ? card.selectFirst(selPrice).text() : null;
                String category = card.selectFirst(selCategory) != null ? card.selectFirst(selCategory).text() : null;
                Element a = card.selectFirst(selLink);
                String link = a != null ? a.absUrl("href") : null;

                double price = 0.0;
                if (priceRaw != null && !priceRaw.isBlank()) {
                    priceRaw = priceRaw.replaceAll("[^0-9.,]", "").replace(",", ".");
                    try {
                        price = Double.parseDouble(priceRaw);
                    } catch (NumberFormatException ignored) {}
                }

                if (title != null) {
                    ProductDTO dto = new ProductDTO(title, category, price, link);
                    out.add(dto);
                }
            }

            // Sayfalama (next butonu varsa)
            Element next = doc.selectFirst(selNext);
            url = (next != null) ? next.absUrl("href") : null;

            page++;
            if (delayMs > 0) Thread.sleep(delayMs);
        }
        return out;
    }

    @Override
    public String getName() {
        return "JSOUP";
    }
}

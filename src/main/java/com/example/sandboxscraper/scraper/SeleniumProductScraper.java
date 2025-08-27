package com.example.sandboxscraper.scraper;

import com.example.sandboxscraper.dto.ProductDTO;
import com.example.sandboxscraper.util.PriceUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class SeleniumProductScraper implements ProductScraper {

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

    @Value("${app.selectors.image}")
    private String selImage;

    @Value("${app.selectors.next}")
    private String selNext;

    @Override
    public List<ProductDTO> scrapeAll(int maxPages) throws Exception {
        List<ProductDTO> out = new ArrayList<>();

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage");
        options.addArguments("--user-agent=" + userAgent);
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get(baseUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(timeoutMs));
            int page = 1;

            while (page <= maxPages) {
                // Sayfa kaynağını JS render sonrası alıp JSoup ile parse edelim
                String html = driver.getPageSource();
                Document doc = Jsoup.parse(html, baseUrl);

                Elements cards = doc.select(selProduct);
                for (Element card : cards) {
                    String title = card.selectFirst(selTitle) != null ? card.selectFirst(selTitle).text() : null;
                    String priceRaw = card.selectFirst(selPrice) != null ? card.selectFirst(selPrice).text() : null;
                    String category = card.selectFirst(selCategory) != null ? card.selectFirst(selCategory).text() : null;
                    Element a = card.selectFirst(selLink);
                    String link = a != null ? a.absUrl("href") : null;
                    Element img = card.selectFirst(selImage);
                    String imageUrl = img != null ? img.absUrl("src") : null;

                    BigDecimal price = PriceUtils.parsePrice(priceRaw);
                    String currency = PriceUtils.detectCurrency(priceRaw);

                    if (title != null) {
                        out.add(new ProductDTO(null, title, price, currency, category, link, imageUrl, null, getName()));
                    }
                }

                // Next butonuna tıkla (varsa)
                List<WebElement> nextCandidates = driver.findElements(By.cssSelector(selNext));
                if (nextCandidates.isEmpty()) break;

                try {
                    WebElement next = nextCandidates.get(0);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", next);
                } catch (Exception e) {
                    break;
                }

                page++;
                if (delayMs > 0) Thread.sleep(delayMs);
            }
        } finally {
            driver.quit();
        }
        return out;
    }

    @Override
    public String getName() {
        return "SELENIUM";
    }
}

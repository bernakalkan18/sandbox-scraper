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
import org.openqa.selenium.PageLoadStrategy;

import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class SeleniumProductScraper implements ProductScraper {

    @Override
    public List<ProductDTO> scrapeAll(int maxPages) throws Exception {
        WebDriverManager.chromedriver().setup();

        ChromeOptions opt = new ChromeOptions();
        opt.setPageLoadStrategy(PageLoadStrategy.EAGER);

        opt.addArguments("--headless=new");
        opt.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
        opt.addArguments("--window-size=1600,1000");
        opt.addArguments("--disable-blink-features=AutomationControlled");
        opt.addArguments("--remote-allow-origins=*");

        // Ağ/SSL toleransları
        opt.addArguments("--disable-quic");
        opt.addArguments("--ignore-certificate-errors");
        opt.addArguments("--allow-running-insecure-content");
        // Proxy ile ilgili hiç argüman göndermiyoruz (DİRECT hatası böyle kesildi)
        // İstersen: opt.addArguments("--no-proxy-server");

        opt.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit(537.36) Chrome/139.0.0.0 Safari/537.36");
        opt.setAcceptInsecureCerts(true);

        WebDriver driver = new ChromeDriver(opt);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        try {
            List<ProductDTO> products = new ArrayList<>();
            String base = "https://sandbox.oxylabs.io/products";
            int page = 1;

            while (page <= maxPages) {
                String url = base + (page > 1 ? "?page=" + page : "");
                System.out.println("[SCRAPER] navigating: " + url);

                // Basit retry
                int attempts = 0;
                while (true) {
                    try {
                        driver.get(url);
                        break;
                    } catch (WebDriverException wde) {
                        attempts++;
                        if (attempts >= 3) throw wde;
                        Thread.sleep(1000L * attempts);
                    }
                }

                // “hazır” sinyalleri
                try {
                    wait.until(ExpectedConditions.or(
                            ExpectedConditions.presenceOfElementLocated(By.xpath("//h1")),
                            ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//a"))
                    ));
                } catch (TimeoutException ignored) {}

                dismissConsentIfAny(driver);

                // 1) Önce Selenium ile kartları arayalım
                List<WebElement> cards = driver.findElements(By.xpath(
                        "//button[contains(.,'Add to Basket')]/ancestor::li[1] | " +
                                "//button[contains(.,'Add to Basket')]/ancestor::div[1]"
                ));
                if (cards.isEmpty()) {
                    cards = driver.findElements(By.xpath(
                            "//*[contains(text(),'€') or contains(text(),'$') or contains(text(),'₺') or contains(text(),'£')]/ancestor::li[1] | " +
                                    "//*[contains(text(),'€') or contains(text(),'$') or contains(text(),'₺') or contains(text(),'£')]/ancestor::div[1]"
                    ));
                }
                System.out.println("[SCRAPER] selenium cards found: " + cards.size());

                int before = products.size();
                for (WebElement card : cards) {
                    WebElement a = firstOrNull(card, By.xpath(".//a[1]"));
                    String title = safeText(a);
                    String link  = attrOrEmpty(a, "href");
                    String priceRaw = safeText(firstOrNull(card,
                            By.xpath(".//*[contains(text(),'€') or contains(text(),'$') or contains(text(),'₺') or contains(text(),'£')][1]")));
                    String category = safeText(firstOrNull(card, By.xpath(".//small | .//*[contains(@class,'category')][1]")));

                    if (title.isBlank()) continue;

                    BigDecimal priceVal = PriceUtils.parsePrice(priceRaw);
                    double price = (priceVal != null) ? priceVal.doubleValue() : 0.0;
                    products.add(new ProductDTO(title, category, price, link));
                }

                // 2) Eğer Selenium ile hiç ürün toplanmadıysa: JSoup fallback
                if (products.size() == before) {
                    String html = driver.getPageSource();
                    int added = parseWithJsoup(html, url, products);
                    System.out.println("[SCRAPER] jsoup added: " + added + " items (fallback)");
                } else {
                    System.out.println("[SCRAPER] selenium added: " + (products.size() - before) + " items");
                }

                page++;
                Thread.sleep(200);
            }

            System.out.println("[SCRAPER] total products: " + products.size());
            return products;
        } finally {
            try { driver.quit(); } catch (Exception ignore) {}
        }
    }

    /** JSoup ile sayfa kaynak kodundan ürünleri çıkaran fallback */
    private int parseWithJsoup(String html, String baseUrl, List<ProductDTO> products) {
        int before = products.size();
        Document doc = Jsoup.parse(html, baseUrl);

        // “/products/{id}” linki içeren blokları topla
        // li veya div altında bir <a href="/products/.."> varsa o bloğu ürün adayı say
        Elements blocks = doc.select("li:has(a[href*=/products/]), div:has(a[href*=/products/])");
        // Eğer aşırı geniş geldiyse filtreyi sıkılaştırabiliriz ama önce genel tarama iyi
        for (Element block : blocks) {
            Element a = block.selectFirst("a[href*=/products/]");
            if (a == null) continue;

            String title = safe(a.text());
            String link  = safe(a.absUrl("href").isEmpty() ? a.attr("href") : a.absUrl("href"));

            // Fiyata dair ilk görünen metni yakala
            Element priceEl = block.selectFirst(":matchesOwn(€|\\$|₺|£)");
            String priceRaw = priceEl != null ? priceEl.text() : "";

            // Kategori olarak <small> veya .category benzeri bir alanı dene
            Element catEl = block.selectFirst("small, .category, .product-category");
            String category = catEl != null ? safe(catEl.text()) : "";

            if (title.isBlank()) continue;

            BigDecimal priceVal = PriceUtils.parsePrice(priceRaw);
            double price = (priceVal != null) ? priceVal.doubleValue() : 0.0;

            products.add(new ProductDTO(title, category, price, link));
        }
        return products.size() - before;
    }

    private void dismissConsentIfAny(WebDriver driver) {
        try {
            String[] texts = {"Accept", "I agree", "Got it", "Kabul", "Tamam", "Close", "Allow", "Çerez", "Cookies"};
            for (String t : texts) {
                List<WebElement> btns = driver.findElements(By.xpath("//button[contains(.,'" + t + "')]"));
                if (!btns.isEmpty()) { btns.get(0).click(); break; }
            }
        } catch (Exception ignore) {}
    }

    private WebElement firstOrNull(WebElement root, By by) {
        try {
            List<WebElement> list = root.findElements(by);
            return list.isEmpty() ? null : list.get(0);
        } catch (StaleElementReferenceException e) {
            return null;
        }
    }

    private String safeText(WebElement el) {
        if (el == null) return "";
        String t = el.getText();
        if (t == null || t.isBlank()) t = el.getAttribute("textContent");
        return (t == null) ? "" : t.trim();
    }

    private String attrOrEmpty(WebElement el, String attr) {
        if (el == null) return "";
        String v = el.getAttribute(attr);
        return (v == null) ? "" : v.trim();
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    @Override
    public String getName() { return "SELENIUM"; }
}

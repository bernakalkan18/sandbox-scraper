package com.example.sandboxscraper.scraper;

import com.example.sandboxscraper.dto.ProductDTO;
import com.example.sandboxscraper.util.PriceUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class SeleniumProductScraper implements ProductScraper {

    @Override
    public List<ProductDTO> scrapeAll(int maxPages) throws Exception {
        // Chromedriver'ı otomatik kur/uyumla (proxy varsa ortam ayarı gerekebilir)
        WebDriverManager.chromedriver().setup();

        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("--headless=new");               // Swagger çağrısında pencere açmasın
        opt.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
        opt.addArguments("--window-size=1600,1000");
        opt.addArguments("--disable-blink-features=AutomationControlled");
        opt.addArguments("--remote-allow-origins=*");
        opt.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(opt);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        try {
            List<ProductDTO> products = new ArrayList<>();
            String base = "https://sandbox.oxylabs.io/products";
            int page = 1;

            while (page <= maxPages) {
                String url = base + (page > 1 ? "?page=" + page : "");
                driver.get(url);

                // Sayfa "hazır" sinyali: üst bantta "results - showing" veya bir başlık
                wait.until(ExpectedConditions.or(
                        ExpectedConditions.presenceOfElementLocated(
                                By.xpath("//*[contains(.,'results - showing')]")),
                        ExpectedConditions.presenceOfElementLocated(
                                By.xpath("//h1[contains(.,'Video Games') or contains(.,'Products')]"))
                ));

                // Olası çerez/overlay'leri kapat (best-effort)
                dismissConsentIfAny(driver);

                // Kart adaylarını birkaç yolla bul
                List<WebElement> cards = driver.findElements(By.xpath(
                        "//button[contains(.,'Add to Basket')]/ancestor::li[1] | " +
                                "//button[contains(.,'Add to Basket')]/ancestor::div[1]"
                ));

                if (cards.isEmpty()) {
                    // Para sembolü görünen bloklar
                    cards = driver.findElements(By.xpath(
                            "//*[contains(text(),'€') or contains(text(),'$') or contains(text(),'₺')]" +
                                    "/ancestor::li[1] | " +
                                    "//*[contains(text(),'€') or contains(text(),'$') or contains(text(),'₺')]" +
                                    "/ancestor::div[1]"
                    ));
                }

                if (cards.isEmpty()) {
                    // Linkten yukarı çık (genel fallback)
                    wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//a")));
                    cards = driver.findElements(By.xpath("//a/ancestor::li[1] | //a/ancestor::div[1]"));
                }

                int before = products.size();

                for (WebElement card : cards) {
                    WebElement a = firstOrNull(card, By.xpath(".//a[1]"));
                    String title = safeText(a);
                    String link  = attrOrEmpty(a, "href");

                    String priceRaw = safeText(firstOrNull(card,
                            By.xpath(".//*[contains(text(),'€') or contains(text(),'$') or contains(text(),'₺')][1]")));

                    String category = safeText(firstOrNull(card,
                            By.xpath(".//small | .//*[contains(@class,'category')][1]")));

                    if (title.isBlank()) continue;

                    BigDecimal priceVal = PriceUtils.parsePrice(priceRaw);
                    double price = (priceVal != null) ? priceVal.doubleValue() : 0.0;

                    products.add(new ProductDTO(title, category, price, link));
                }

                // Bu sayfadan hiç ürün eklenmediyse kısa bir ek bekleme dene
                if (products.size() == before) {
                    try {
                        new WebDriverWait(driver, Duration.ofSeconds(5)).until(
                                ExpectedConditions.presenceOfElementLocated(
                                        By.xpath("//button[contains(.,'Add to Basket')]")));
                    } catch (TimeoutException ignore) { /* geç */ }
                }

                page++;
                Thread.sleep(250);
            }

            return products;
        } finally {
            try { driver.quit(); } catch (Exception ignore) {}
        }
    }

    private void dismissConsentIfAny(WebDriver driver) {
        try {
            String[] texts = {"Accept", "I agree", "Got it", "Kabul", "Tamam", "Close", "Allow"};
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

    @Override
    public String getName() { return "SELENIUM"; }
}

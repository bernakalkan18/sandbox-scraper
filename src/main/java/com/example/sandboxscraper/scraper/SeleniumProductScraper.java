package com.example.sandboxscraper.scraper;

import com.example.sandboxscraper.dto.ProductDTO;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SeleniumProductScraper implements ProductScraper {

    private static final String BASE_URL = "https://sandbox.oxylabs.io/products";

    @Override
    public List<ProductDTO> scrapeAll(int maxPages) throws Exception {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        try {
            List<ProductDTO> products = new ArrayList<>();
            int pages = Math.max(1, maxPages);

            for (int page = 1; page <= pages; page++) {
                String url = (page == 1) ? BASE_URL : BASE_URL + "?page=" + page;
                driver.get(url);
                Thread.sleep(1500);

                List<WebElement> items = driver.findElements(By.cssSelector(".product-item"));
                if (items.isEmpty()) break;

                for (WebElement item : items) {
                    String name = item.findElement(By.cssSelector(".product-name")).getText();
                    String category = item.findElement(By.cssSelector(".product-category")).getText();
                    double price = Double.parseDouble(
                            item.findElement(By.cssSelector(".product-price"))
                                    .getText()
                                    .replace("$","")
                    );
                    String link = item.findElement(By.tagName("a")).getAttribute("href");

                    products.add(new ProductDTO(name, category, price, link));
                }
            }
            return products;
        } finally {
            driver.quit();
        }
    }

    @Override
    public String getName() {
        return "SELENIUM";
    }
}

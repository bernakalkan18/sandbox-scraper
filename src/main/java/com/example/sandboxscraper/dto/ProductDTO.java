package com.example.sandboxscraper.dto;

import java.math.BigDecimal;

public class ProductDTO {
    private String id;          // SKU veya sayfadaki ID
    private String name;
    private BigDecimal price;
    private String currency;    // "$" → "USD" dönüştürülebilir
    private String category;
    private String detailUrl;
    private String imageUrl;
    private Double rating;      // varsa
    private String source;      // "JSOUP" | "SELENIUM" | "API"

    public ProductDTO() {}

    public ProductDTO(String id, String name, BigDecimal price, String currency,
                      String category, String detailUrl, String imageUrl, Double rating, String source) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.currency = currency;
        this.category = category;
        this.detailUrl = detailUrl;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.source = source;
    }

    // Getters & Setters
    // (IntelliJ: Code → Generate → Getter/Setter)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDetailUrl() { return detailUrl; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}

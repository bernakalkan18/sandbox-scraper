package com.example.sandboxscraper.dto;

public class ProductDTO {

    private String name;
    private String category;
    private double price;
    private String url;

    // Default constructor (Spring/JSON için gerekli)
    public ProductDTO() {}

    // Parametreli constructor
    public ProductDTO(String name, String category, double price, String url) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.url = url;
    }

    // Getter ve Setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}

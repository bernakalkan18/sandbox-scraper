# Sandbox Scraper

Spring Boot tabanlı web scraping projesi.  
HashMap ve DTO kullanılarak in-memory cache yapısı oluşturulmuştur. Selenium ile scraping yapılmakta ve sonuçlar Swagger UI üzerinden REST API olarak sunulmaktadır.  

Bu proje öğrenme amaçlıdır. Medium’da yazdığım HashMap ve DTO hakkındaki yazıyı desteklemek için geliştirilmiştir.

---

## Özellikler
- Scraper Motoru: Selenium (headless Chrome)
- In-memory cache: HashMap + DTO ile hızlı arama ve filtreleme
- Swagger UI: API uçlarını deneyerek test etme
- Lazy load cache: Cache boşsa ilk istekte otomatik olarak dolmaktadır
- Maven ile derlenmektedir
- Yakında Docker desteği eklenecektir

---

## Çalıştırma
```bash
# 1) Derleme
./mvnw clean package

# 2) Uygulamayı başlatma
./mvnw spring-boot:run

# 3) Swagger UI
http://localhost:8080/swagger-ui/index.html



API Uçları

GET    /api/scraper/products/refresh          
GET    /api/scraper/products  
GET    /api/scraper/products/{category}     
GET    /api/scraper/products/search    
POST   /api/scraper/products/grouped   
GET    /api/scraper/engine             

GET    /api/products/refresh                  
POST   /api/products         
GET    /api/products/{category}    
GET    /api/products/search

Kullanılan Teknolojiler:
Java 17,
Spring Boot 3.3.x,
Selenium WebDriver,
Swagger / OpenAPI,
Maven





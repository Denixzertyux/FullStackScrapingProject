package com.example.scraping.service;

import com.example.scraping.entity.Product;
import com.example.scraping.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingService {

    private final ProductRepository productRepository;
    private final ExchangeRateService exchangeRateService;

    @Scheduled(cron = "0 0 12-18 * * *")
    public void performScrapingTask() {
        log.info("Începem procesul de web scraping cu suport pentru paginare...");

        // 1. Obținem cursul valutar o singură dată per execuție
        Double currentExchangeRate = exchangeRateService.getCurrentUsdToRonRate();

        try {
            // 2. Efectuăm logarea pentru a obține cookie-urile de sesiune
            Connection.Response loginForm = Jsoup.connect("https://www.web-scraping.dev/login")
                    .method(Connection.Method.GET)
                    .execute();

            Connection.Response loginAction = Jsoup.connect("https://www.web-scraping.dev/api/login")
                    .data("username", "user123")
                    .data("password", "password")
                    .cookies(loginForm.cookies())
                    .method(Connection.Method.POST)
                    .execute();

            Map<String, String> cookies = loginAction.cookies();

            // 3. Setăm variabilele pentru navigarea prin pagini
            int currentPage = 1;
            boolean hasMorePages = true;
            int totalNewProducts = 0;

            // 4. Bucla care trece prin toate paginile disponibile
            while (hasMorePages) {
                String url = "https://www.web-scraping.dev/products?category=consumables&page=" + currentPage;
                log.info("Accesăm pagina {}: {}", currentPage, url);

                Document doc = Jsoup.connect(url)
                        .cookies(cookies)
                        .get();

                Elements productElements = doc.select(".product");

                // Dacă nu mai găsim elemente cu clasa .product, înseamnă că am ajuns la capăt
                if (productElements.isEmpty()) {
                    log.info("Nu s-au mai găsit produse pe pagina {}. Oprim navigarea.", currentPage);
                    hasMorePages = false;
                    break;
                }

                log.info("S-au găsit {} produse pe pagina {}.", productElements.size(), currentPage);

                // 5. Procesăm produsele găsite pe pagina curentă
                for (Element element : productElements) {

                    // Utilizăm selectorii corecți extrași din structura site-ului
                    String name = element.select("h3 a").text();
                    String imageUrl = element.select(".thumbnail img").attr("src");
                    String rawPrice = element.select(".price").text();
                    String description = element.select(".short-description").text();

                    String cleanPrice = rawPrice.replaceAll("[^0-9.]", "");
                    Double finalPrice = 0.0;

                    if (!cleanPrice.isEmpty()) {
                        finalPrice = Double.parseDouble(cleanPrice);
                    }

                    // 6. Verificăm în baza de date ca să evităm duplicatele
                    if (productRepository.findByName(name).isEmpty()) {
                        Product newProduct = new Product();
                        newProduct.setName(name);
                        newProduct.setImageUrl(imageUrl);
                        newProduct.setOriginalPrice(String.valueOf(finalPrice));
                        newProduct.setDescription(description);

                        // Calculăm prețul în RON
                        Double priceInRon = finalPrice * currentExchangeRate;
                        newProduct.setPriceRon(Math.round(priceInRon * 100.0) / 100.0);
                        newProduct.setExchangeRate(currentExchangeRate);

                        productRepository.save(newProduct);
                        totalNewProducts++;
                        log.info("A fost salvat un produs nou: {}", name);
                    }
                }

                // Trecem la următoarea pagină
                currentPage++;
            }

            log.info("Procesul de web scraping a fost finalizat! Total produse noi salvate: {}", totalNewProducts);

        } catch (IOException e) {
            log.error("Eroare la accesarea paginilor pentru web scraping: ", e);
        }
    }
}
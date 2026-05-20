package com.example.scraping.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class ExchangeRateService {

    // Folosim endpoint-ul public; baza este USD
    private static final String API_URL = "https://open.er-api.com/v6/latest/USD";

    public Double getCurrentUsdToRonRate() {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Mapăm răspunsul JSON direct într-un Map de Java
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(API_URL, Map.class);

            if (response != null && response.containsKey("rates")) {
                @SuppressWarnings("unchecked")
                Map<String, Number> rates = (Map<String, Number>) response.get("rates");

                if (rates.containsKey("RON")) {
                    Double ronRate = rates.get("RON").doubleValue();
                    log.info("Curs valutar obținut cu succes: 1 USD = {} RON", ronRate);
                    return ronRate;
                }
            }
        } catch (Exception e) {
            log.error("Eroare la preluarea cursului valutar. Se va folosi un curs de fallback.", e);
        }

        // Valoare de rezervă (fallback) în cazul în care API-ul pică, pentru a nu bloca aplicația
        return 4.60;
    }
}
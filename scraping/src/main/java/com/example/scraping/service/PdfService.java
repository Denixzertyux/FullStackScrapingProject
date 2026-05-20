package com.example.scraping.service;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PdfService {

    public record ExtractedProduct(String code, String name, String unitPrice, String currency, String quantity) {}

    public List<ExtractedProduct> extractProductsFromPdf(MultipartFile file) {
        List<ExtractedProduct> products = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String pdfText = stripper.getText(document);

            // 1. Extragem codul și linia asociată din secțiunea "Identificator vanzator articol pentru linia X : Y"
            Pattern codePattern = Pattern.compile("Identificator vanzator articol pentru linia (\\d+)\\s*:(.+)");
            Matcher codeMatcher = codePattern.matcher(pdfText);

            // Folosim un Map pentru a lega numărul liniei de codul produsului
            java.util.Map<Integer, String> codeMap = new java.util.LinkedHashMap<>();
            while (codeMatcher.find()) {
                Integer lineNum = Integer.parseInt(codeMatcher.group(1).trim());
                String code = codeMatcher.group(2).trim();
                codeMap.put(lineNum, code);
            }

            // 2. Parcurgem fiecare pereche Linie-Cod și extragem datele
            for (java.util.Map.Entry<Integer, String> entry : codeMap.entrySet()) {
                int lineNumber = entry.getKey();
                String code = entry.getValue();

                // Regex explicat pe baza logului tău brut:
                // ^([-\\d.,]+)        -> Extrage Prețul Unitar (Ex: 251.96)
                // \\s+(RON|EUR)       -> Extrage Moneda (Ex: RON)
                // \\s+([-\\d.,]+)     -> Extrage Cantitatea (Ex: -1)
                // \\s+[-\\d.,]+\\s+\\w+\\s+[\\d.,]+\\s+[-\\d.,]+\\s* -> Trece peste celelalte numere și ajunge la valoarea netă
                // Pattern.quote(code) -> Trece exact peste codul lipit (Ex: 172812F)
                // \\s+(.+?)           -> Extrage Numele curat (Ex: COMUTATOR PORNIRE FEBI)
                // \\s* + lineNumber + \\s*$ -> Consumă numărul liniei lipit la final (Ex: 1)

                String regex = "(?m)^([-\\d.,]+)\\s+(RON|EUR)\\s+([-\\d.,]+)\\s+[-\\d.,]+\\s+\\w+\\s+[\\d.,]+\\s+[-\\d.,]+\\s*"
                        + Pattern.quote(code) + "\\s+(.+?)\\s*" + lineNumber + "\\s*$";

                Pattern linePattern = Pattern.compile(regex);
                Matcher lineMatcher = linePattern.matcher(pdfText);

                if (lineMatcher.find()) {
                    String unitPrice = lineMatcher.group(1).trim();
                    String currency = lineMatcher.group(2).trim();
                    String quantity = lineMatcher.group(3).trim();
                    String cleanName = lineMatcher.group(4).trim();

                    products.add(new ExtractedProduct(code, cleanName, unitPrice, currency, quantity));
                    log.info("Adăugat: {} - {} - {} {} - Cantitate: {}", code, cleanName, unitPrice, currency, quantity);
                } else {
                    log.warn("Regex-ul nu a găsit potrivire pentru produsul: {} de pe linia {}", code, lineNumber);
                }
            }

            log.info("Au fost extrase {} produse din fisierul PDF.", products.size());

        } catch (IOException e) {
            log.error("Eroare la procesarea fisierului PDF", e);
            throw new RuntimeException("Nu s-a putut citi fisierul PDF", e);
        }

        return products;
    }

    public byte[] generateCsvFromProducts(List<ExtractedProduct> products) {
        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {

            // 1. Definim și scriem capul de tabel (Header)
            String[] header = {"Cod produs", "Denumire produs", "Pret unitar", "Moneda", "Cantitate"};
            csvWriter.writeNext(header);

            // 2. Iterăm prin lista de produse și scriem fiecare rând
            for (ExtractedProduct product : products) {
                String[] data = {
                        product.code(),
                        product.name(),
                        product.unitPrice(),
                        product.currency(),
                        product.quantity()
                };
                csvWriter.writeNext(data);
            }

            return stringWriter.toString().getBytes(StandardCharsets.UTF_8);

        } catch (IOException e) {
            log.error("Eroare la generarea fisierului CSV", e);
            throw new RuntimeException("Eroare la generarea CSV-ului", e);
        }
    }
}
package com.example.scraping.controller;

import com.example.scraping.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;


    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> uploadPdfAndGetCsv(@RequestParam("file") MultipartFile file) {
        log.info("S-a primit cererea de procesare pentru fișierul: {}", file.getOriginalFilename());

        try {
            // 1. Extragem datele din PDF folosind serviciul nostru
            List<PdfService.ExtractedProduct> extractedProducts = pdfService.extractProductsFromPdf(file);

            // 2. Generăm CSV-ul sub formă de array de bytes
            byte[] csvData = pdfService.generateCsvFromProducts(extractedProducts);

            // 3. Configurăm headerele HTTP pentru a declanșa descărcarea fișierului în browser
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", "produse_extrase.csv");
            headers.setContentType(MediaType.parseMediaType("text/csv"));

            log.info("Fișierul CSV a fost generat cu succes.");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvData);

        } catch (Exception e) {
            log.error("A apărut o eroare la procesarea fișierului: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

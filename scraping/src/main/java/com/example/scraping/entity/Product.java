package com.example.scraping.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "name",unique = true ,nullable = false)
    private String name;

    @Column(name = "original_price")
    private String originalPrice;

    @Column(name = "price_ron")
    private Double priceRon;

    @Column(name = "exchange_rate")
    private Double exchangeRate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

}

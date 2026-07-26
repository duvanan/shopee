package com.shopee.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Khong dung @ManyToOne/@JoinColumn cho category - chi luu categoryId
 * (plain FK), tranh phu thuoc vao object graph/lazy loading cua JPA khi
 * he thong lon dan. Muon lay ten category thi tu goi CategoryRepository
 * rieng (xem ProductService), khong de Hibernate tu "mapping" ho. Cot audit
 * ke thua tu Auditable, Spring Data JPA Auditing tu gan gia tri.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "category_id")
    private UUID categoryId;
}

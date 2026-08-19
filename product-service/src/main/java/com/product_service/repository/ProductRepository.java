package com.product_service.repository;

import com.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsByNameIgnoreCase(String name);
    boolean existsBySkuIgnoreCase(String sku);

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN p.brands b 
    WHERE LOWER(p.name) LIKE LOWER(CONCAT('%' , :keyword, '%'))
    OR LOWER(p.subCategory.name) LIKE LOWER(CONCAT('%' , :keyword, '%'))
    OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) 
""")

    List<Product>searchProducts(@Param("keyword") String keyword);

    @Query(value = """
            SELECT DISTINCT p FROM Product p
            LEFT JOIN p.subCategory sc
            LEFT JOIN sc.category c
            LEFT JOIN p.brands b
            WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(sc.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:subCategoryId IS NULL OR sc.id = :subCategoryId)
              AND (:brandId IS NULL OR b.id = :brandId)
              AND (:minPrice IS NULL OR b.price >= :minPrice)
              AND (:maxPrice IS NULL OR b.price <= :maxPrice)
              AND p.active = true
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p) FROM Product p
            LEFT JOIN p.subCategory sc
            LEFT JOIN sc.category c
            LEFT JOIN p.brands b
            WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(sc.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:subCategoryId IS NULL OR sc.id = :subCategoryId)
              AND (:brandId IS NULL OR b.id = :brandId)
              AND (:minPrice IS NULL OR b.price >= :minPrice)
              AND (:maxPrice IS NULL OR b.price <= :maxPrice)
              AND p.active = true
            """)
    Page<Product> searchCatalog(@Param("keyword") String keyword,
                                @Param("categoryId") Integer categoryId,
                                @Param("subCategoryId") Integer subCategoryId,
                                @Param("brandId") Integer brandId,
                                @Param("minPrice") java.math.BigDecimal minPrice,
                                @Param("maxPrice") java.math.BigDecimal maxPrice,
                                Pageable pageable);

}

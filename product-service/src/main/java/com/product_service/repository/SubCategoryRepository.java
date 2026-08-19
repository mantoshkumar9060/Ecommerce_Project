package com.product_service.repository;

import com.product_service.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Integer> {
    Optional<SubCategory> findByNameIgnoreCase(String name);
}

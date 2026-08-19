package com.product_service.dto;

import java.util.LinkedHashSet;
import java.util.Set;

public class ProductDto {
    private Integer id;
    private String name;
    private String sku;
    private String description;
    private boolean active;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public SubCategoryDto getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(SubCategoryDto subCategory) {
        this.subCategory = subCategory;
    }

    public Set<BrandDto> getBrands() {
        return brands;
    }

    public void setBrands(Set<BrandDto> brands) {
        this.brands = brands;
    }

    private SubCategoryDto subCategory;
    private Set<BrandDto> brands = new LinkedHashSet<>();


}

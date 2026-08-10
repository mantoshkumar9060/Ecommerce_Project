package com.product_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.product_service.entity.Category;
import com.product_service.entity.Product;
import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

public class SubCategoryDto {

    public CategoryDto getCategory() {
        return category;
    }

    public void setCategory(CategoryDto category) {
        this.category = category;
    }

    private CategoryDto category;

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

    private Integer id;
    private String name;

    public Set<ProductDto> getProducts() {
        return products;
    }

    public void setProducts(Set<ProductDto> products) {
        this.products = products;
    }

    @JsonIgnore
    private Set<ProductDto> products = new LinkedHashSet<>();




}

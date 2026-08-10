package com.product_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.product_service.entity.Image;
import com.product_service.entity.Product;
import com.product_service.entity.Size;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

public class BrandDto {


    public ProductDto getProduct() {
        return product;
    }

    public void setProduct(ProductDto productDto) {
        this.product = productDto;
    }

    @JsonIgnore
    private ProductDto product;

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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    private Integer id;
    private String name;
    private BigDecimal price;

    private Set<SizeDto> sizes = new LinkedHashSet<>();

    public Set<SizeDto> getSizes() {
        return sizes;
    }

    public void setSizes(Set<SizeDto> sizes) {
        this.sizes = sizes;
    }

    public Set<ImageDto> getImages() {
        return images;
    }

    public void setImages(Set<ImageDto> images) {
        this.images = images;
    }

    private Set<ImageDto> images = new LinkedHashSet<>();





}

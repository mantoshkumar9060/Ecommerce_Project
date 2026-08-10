package com.product_service.dto;

import com.product_service.entity.Brand;

public class ImageDto {


    private Integer id;

    private String url;




    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

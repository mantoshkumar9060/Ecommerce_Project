package com.ecommerce.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "product-service", path = "/api/v1/products")
public interface ProductClient {
    @GetMapping CatalogueResponse catalogue(@RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("sort") String sort);
    record CatalogueResponse(List<Product> data, Integer page, Integer size, Long totalElements, Integer totalPages) { }
    record Product(Integer id, String name, String description, boolean active, SubCategory subCategory, List<Brand> brands) { }
    record Brand(Integer id, String name, BigDecimal price) { }
    record SubCategory(Integer id, String name, Category category) { }
    record Category(long id, String name) { }
}

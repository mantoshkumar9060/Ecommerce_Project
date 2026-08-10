package com.product_service.service;

import com.product_service.dto.ProductDto;
import jakarta.transaction.Transactional;

import java.util.List;

public interface ProductService {

    List<ProductDto> searchProducts(String keyword);
}

package com.product_service.service;

import com.product_service.dto.ImageDto;
import com.product_service.dto.ProductDto;
import com.product_service.dto.ProductBrandSnapshot;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import com.product_service.dto.ApiResponse;
import java.math.BigDecimal;
import com.product_service.dto.ProductManagementDtos;

public interface ProductService {

    List<ProductDto> searchProducts(String keyword);

    ApiResponse<List<ProductDto>> searchCatalog(String keyword, Integer categoryId, Integer subCategoryId,
                                                 Integer brandId, BigDecimal minPrice, BigDecimal maxPrice,
                                                 int page, int size, String sort);
    ProductDto createProduct(ProductManagementDtos.Create request);
    ProductDto updateProduct(Integer productId, ProductManagementDtos.Update request);
    void deactivateProduct(Integer productId);
    ProductDto getProduct(Integer productId);

    ProductBrandSnapshot getProductBrandSnapshot(Integer productId, Integer brandId);

    List<ImageDto> uploadProductImages(
            Integer productId,
            Integer brandId,
            MultipartFile[] files
    ) throws IOException;

    List<ImageDto> getProductImages(Integer productId);

    DownloadedFile getProductImage(
            Integer productId,
            Integer imageId
    );

    void deleteProductImage(
            Integer productId,
            Integer imageId
    );
}

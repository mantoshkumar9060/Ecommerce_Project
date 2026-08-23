package com.product_service.service;

import com.product_service.dto.ProductManagementDtos;
import com.product_service.entity.Product;
import com.product_service.repository.ImageRepository;
import com.product_service.repository.ProductRepository;
import com.product_service.repository.SubCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceImplTest {

    private ProductRepository productRepository;
    private ImageRepository imageRepository;
    private S3Service s3Service;
    private SubCategoryRepository subCategoryRepository;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        imageRepository = mock(ImageRepository.class);
        s3Service = mock(S3Service.class);
        subCategoryRepository = mock(SubCategoryRepository.class);

        productService = new ProductServiceImpl(
                productRepository,
                imageRepository,
                s3Service,
                subCategoryRepository
        );
    }

    @Test
    void shouldDeactivateProduct() {

        Product product = new Product();
        product.setActive(true);

        when(productRepository.findById(1))
                .thenReturn(Optional.of(product));

        productService.deactivateProduct(1);

        assertFalse(product.isActive());
    }

    @Test
    void shouldThrowExceptionWhenDeactivatingMissingProduct() {

        when(productRepository.findById(999))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.deactivateProduct(999)
        );
    }

    @Test
    void shouldThrowExceptionWhenGettingInactiveProduct() {

        Product product = new Product();
        product.setActive(false);

        when(productRepository.findById(1))
                .thenReturn(Optional.of(product));

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getProduct(1)
        );
    }

    @Test
    void shouldThrowExceptionForDuplicateSkuDuringCreate() {

        ProductManagementDtos.Create request =
                new ProductManagementDtos.Create(
                        "Test Product",
                        "sku-123",
                        "Description",
                        1
                );

        when(productRepository.existsBySkuIgnoreCase("sku-123"))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> productService.createProduct(request)
        );

        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenCatalogHasNegativeMinPrice() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.searchCatalog(
                        null,
                        null,
                        null,
                        null,
                        java.math.BigDecimal.valueOf(-100),
                        null,
                        0,
                        10,
                        "relevance"
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenMinPriceIsGreaterThanMaxPrice() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.searchCatalog(
                        null,
                        null,
                        null,
                        null,
                        java.math.BigDecimal.valueOf(5000),
                        java.math.BigDecimal.valueOf(1000),
                        0,
                        10,
                        "relevance"
                )
        );
    }

    @Test
    void shouldThrowExceptionForUnsupportedSort() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.searchCatalog(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        10,
                        "invalid-sort"
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenGettingMissingProduct() {

        when(productRepository.findById(999))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.getProduct(999)
        );
    }

    @Test
    void shouldNotCallSaveWhenProductIsAlreadyInactive() {

        Product product = new Product();
        product.setActive(false);

        when(productRepository.findById(1))
                .thenReturn(Optional.of(product));

        productService.deactivateProduct(1);

        verify(productRepository, never())
                .save(any(Product.class));
    }

}
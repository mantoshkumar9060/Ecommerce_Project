package com.product_service.controller;

import com.product_service.dto.ApiResponse;
import com.product_service.dto.CategoryDto;
import com.product_service.dto.ImageDto;
import com.product_service.dto.ProductDto;
import com.product_service.dto.ProductManagementDtos;
import com.product_service.service.CategoryService;
import com.product_service.service.DownloadedFile;
import com.product_service.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private CategoryService categoryService;
    private ProductService productService;

    public ProductController(CategoryService categoryService,
                             ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping("/{productId}")
    public ProductDto getProduct(@PathVariable Integer productId) { return productService.getProduct(productId); }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@jakarta.validation.Valid @RequestBody ProductManagementDtos.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{productId}")
    public ProductDto updateProduct(@PathVariable Integer productId, @jakarta.validation.Valid @RequestBody ProductManagementDtos.Update request) {
        return productService.updateProduct(productId, request);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateProduct(@PathVariable Integer productId) { productService.deactivateProduct(productId); }

    @GetMapping("/list/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories() {

        List<CategoryDto> categoriesDto = categoryService.findAll();

        ApiResponse<List<CategoryDto>> response = new ApiResponse<>();

        if (!categoriesDto.isEmpty()) {
            response.setMessage("All categories data fetched");
            response.setStatus(200);
            response.setData(categoriesDto);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.setMessage("No categories data found");
        response.setStatus(200);
        response.setData(categoriesDto);

        return new ResponseEntity<>(
                response,
                HttpStatus.OK
        );
    }

    @GetMapping("/list/search")
    public ResponseEntity<ApiResponse<List<ProductDto>>> searchProducts(
            @RequestParam String keyword) {

        List<ProductDto> productDtos =
                productService.searchProducts(keyword);

        ApiResponse<List<ProductDto>> response = new ApiResponse<>();

        if (!productDtos.isEmpty()) {
            response.setMessage("All data fetched");
            response.setStatus(200);
            response.setData(productDtos);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.setMessage("No products found");
        response.setStatus(200);
        response.setData(productDtos);

        return new ResponseEntity<>(
                response,
                HttpStatus.OK
        );
    }

    @PostMapping("/{productId}/upload/images")
    public ResponseEntity<ApiResponse<List<ImageDto>>> uploadImages(
            @PathVariable Integer productId,
            @RequestParam Integer brandId,
            @RequestParam("files") MultipartFile[] files)
            throws IOException {

        List<ImageDto> imageDtos =
                productService.uploadProductImages(
                        productId,
                        brandId,
                        files
                );

        ApiResponse<List<ImageDto>> response =
                new ApiResponse<>();

        response.setMessage(
                "Images uploaded successfully"
        );

        response.setStatus(200);
        response.setData(imageDtos);

        return new ResponseEntity<>(
                response,
                HttpStatus.OK
        );
    }

    @GetMapping("/{productId}/images/{imageId}")
    public ResponseEntity<byte[]> getProductImage(
            @PathVariable Integer productId,
            @PathVariable Integer imageId) {

        DownloadedFile image =
                productService.getProductImage(
                        productId,
                        imageId
                );

        return ResponseEntity.ok()
                .contentType(image.contentType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.parseMediaType(image.contentType()))
                .body(image.content());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDto>>> catalogue(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer subCategoryId,
            @RequestParam(required = false) Integer brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sort) {
        return ResponseEntity.ok(productService.searchCatalog(keyword, categoryId, subCategoryId, brandId,
                minPrice, maxPrice, page, size, sort));
    }

    @GetMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<List<ImageDto>>> getProductImages(
            @PathVariable Integer productId) {
        ApiResponse<List<ImageDto>> response = new ApiResponse<>();
        response.setMessage("Product images fetched successfully");
        response.setStatus(200);
        response.setData(productService.getProductImages(productId));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<String>> deleteImage(
            @PathVariable Integer productId,
            @PathVariable Integer imageId) {

        productService.deleteProductImage(
                productId,
                imageId
        );

        ApiResponse<String> response =
                new ApiResponse<>();

        response.setMessage(
                "Image deleted successfully"
        );

        response.setStatus(200);
        response.setData(null);

        return new ResponseEntity<>(
                response,
                HttpStatus.OK
        );
    }
}

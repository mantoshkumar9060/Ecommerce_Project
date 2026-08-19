package com.product_service.service;
import com.product_service.dto.ImageDto;
import com.product_service.dto.ProductDto;
import com.product_service.dto.ProductBrandSnapshot;
import com.product_service.entity.Brand;
import com.product_service.entity.Image;
import com.product_service.entity.Product;
import com.product_service.mapper.ProductMapper;
import com.product_service.repository.ImageRepository;
import com.product_service.repository.ProductRepository;
import com.product_service.repository.SubCategoryRepository;
import com.product_service.dto.ProductManagementDtos;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ImageRepository imageRepository;
    private final S3Service s3Service;
    private final SubCategoryRepository subCategories;

    public ProductServiceImpl(
            ProductRepository productRepository,
            ImageRepository imageRepository,
            S3Service s3Service, SubCategoryRepository subCategories) {

        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
        this.s3Service = s3Service;
        this.subCategories = subCategories;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProductDto> searchProducts(String keyword) {

        List<Product> products =
                productRepository.searchProducts(keyword);

        List<ProductDto> productDtos =
                new ArrayList<>();

        for (Product product : products) {
            productDtos.add(
                    ProductMapper.convertProductToDto(product)
            );
        }

        return productDtos;
    }

    @Transactional(readOnly = true)
    @Override
    public com.product_service.dto.ApiResponse<List<ProductDto>> searchCatalog(
            String keyword, Integer categoryId, Integer subCategoryId, Integer brandId,
            java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, int page, int size, String sort) {
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0) {
            throw new IllegalArgumentException("Price filters must not be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must not exceed maxPrice");
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort order = switch (sort == null ? "relevance" : sort.toLowerCase()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "brands.price", "id");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "brands.price", "id");
            case "newest" -> Sort.by(Sort.Direction.DESC, "id");
            case "name" -> Sort.by(Sort.Direction.ASC, "name", "id");
            case "relevance" -> Sort.by(Sort.Direction.ASC, "id");
            default -> throw new IllegalArgumentException("Unsupported sort. Use relevance, price_asc, price_desc, newest, or name");
        };
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        var result = productRepository.searchCatalog(normalizedKeyword, categoryId, subCategoryId, brandId,
                minPrice, maxPrice, PageRequest.of(safePage, safeSize, order));
        var response = new com.product_service.dto.ApiResponse<List<ProductDto>>();
        response.setStatus(200);
        response.setMessage("Catalogue fetched successfully");
        response.setData(result.getContent().stream().map(ProductMapper::convertProductToDto).toList());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        return response;
    }

    @Transactional
    @Override
    public ProductDto createProduct(ProductManagementDtos.Create request) {
        if (productRepository.existsBySkuIgnoreCase(request.sku().trim())) throw new IllegalStateException("SKU is already in use");
        Product product = new Product();
        apply(product, request.name(), request.sku(), request.description(), request.subCategoryId());
        return ProductMapper.convertProductToDto(productRepository.save(product));
    }

    @Transactional
    @Override
    public ProductDto updateProduct(Integer productId, ProductManagementDtos.Update request) {
        Product product = product(productId);
        if (!product.getSku().equalsIgnoreCase(request.sku().trim()) && productRepository.existsBySkuIgnoreCase(request.sku().trim())) throw new IllegalStateException("SKU is already in use");
        apply(product, request.name(), request.sku(), request.description(), request.subCategoryId());
        product.setActive(request.active());
        return ProductMapper.convertProductToDto(product);
    }

    @Transactional
    @Override
    public void deactivateProduct(Integer productId) { product(productId).setActive(false); }

    @Transactional(readOnly = true)
    @Override
    public ProductDto getProduct(Integer productId) {
        Product product = product(productId);
        if (!product.isActive()) throw new IllegalArgumentException("Product not found");
        return ProductMapper.convertProductToDto(product);
    }

    private Product product(Integer productId) { return productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found")); }
    private void apply(Product product, String name, String sku, String description, Integer subCategoryId) {
        product.setName(name.trim()); product.setSku(sku.trim().toUpperCase()); product.setDescription(description == null || description.isBlank() ? null : description.trim());
        product.setSubCategory(subCategories.findById(subCategoryId).orElseThrow(() -> new IllegalArgumentException("Subcategory not found")));
    }

    private int quantity(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { return 0; }
    }

    @Transactional(readOnly = true)
    @Override
    public ProductBrandSnapshot getProductBrandSnapshot(Integer productId, Integer brandId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        Brand brand = product.getBrands().stream()
                .filter(candidate -> candidate.getId().equals(brandId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Brand does not belong to product"));
        int availableQuantity = brand.getSizes().stream().map(com.product_service.entity.Size::getQuantity)
                .mapToInt(this::quantity).filter(value -> value > 0).sum();
        return new ProductBrandSnapshot(productId, brandId, product.getName(), brand.getPrice(),
                product.isActive() && availableQuantity > 0, availableQuantity);
    }

    @Transactional
    @Override
    public List<ImageDto> uploadProductImages(
            Integer productId,
            Integer brandId,
            MultipartFile[] files)
            throws IOException {

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found with id: "
                                                + productId
                                ));

        Brand brand =
                product.getBrands()
                        .stream()
                        .filter(b ->
                                b.getId().equals(brandId))
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Brand " + brandId +
                                                " does not belong to product "
                                                + productId
                                ));

        List<ImageDto> imageDtos =
                new ArrayList<>();

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one image file is required");
        }

        if (files.length > 4) {
            throw new IllegalArgumentException("A maximum of 4 images can be uploaded at once");
        }

        List<String> uploadedKeys = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                String s3Key = s3Service.uploadFile(file);
                uploadedKeys.add(s3Key);

                Image image = new Image();
                image.setUrl(s3Key);
                image.setBrand(brand);

                Image savedImage = imageRepository.saveAndFlush(image);

                ImageDto imageDto = new ImageDto();

                imageDto.setId(savedImage.getId());

                imageDto.setS3Key(savedImage.getUrl());

                imageDto.setUrl(
                        "/api/v1/products/" + productId + "/images/" + savedImage.getId()
                );

                imageDtos.add(imageDto);
            }
        } catch (IOException | RuntimeException exception) {
            for (String key : uploadedKeys) {
                try {
                    s3Service.deleteFile(key);
                } catch (RuntimeException cleanupFailure) {
                    exception.addSuppressed(cleanupFailure);
                }
            }
            throw exception;
        }

        return imageDtos;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ImageDto> getProductImages(
            Integer productId) {

        Product product =
                productRepository.findById(productId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found with id: "
                                                + productId
                                ));

        List<ImageDto> imageDtos =
                new ArrayList<>();

        for (Brand brand : product.getBrands()) {

            for (Image image : brand.getImages()) {

                ImageDto imageDto =
                        new ImageDto();

                imageDto.setId(
                        image.getId()
                );

                imageDto.setS3Key(
                        image.getUrl()
                );

                imageDto.setUrl(
                        "/api/v1/products/"
                                + productId
                                + "/images/"
                                + image.getId()
                );

                imageDtos.add(imageDto);
            }
        }

        return imageDtos;
    }

    @Transactional(readOnly = true)
    @Override
    public DownloadedFile getProductImage(
            Integer productId,
            Integer imageId) {

        Image image =
                imageRepository.findById(imageId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Image not found with id: "
                                                + imageId
                                ));

        Brand brand = image.getBrand();

        if (brand == null ||
                brand.getProduct() == null ||
                !brand.getProduct()
                        .getId()
                        .equals(productId)) {

            throw new RuntimeException(
                    "Image does not belong to product "
                            + productId
            );
        }

        return s3Service.downloadFile(image.getUrl());
    }

    @Transactional
    @Override
    public void deleteProductImage(
            Integer productId,
            Integer imageId) {

        Image image =
                imageRepository.findById(imageId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Image not found with id: "
                                                + imageId
                                ));

        Brand brand = image.getBrand();

        if (brand == null ||
                brand.getProduct() == null ||
                !brand.getProduct()
                        .getId()
                        .equals(productId)) {

            throw new RuntimeException(
                    "Image does not belong to product "
                            + productId
            );
        }

        imageRepository.delete(image);
        imageRepository.flush();
        s3Service.deleteFile(image.getUrl());
    }
}

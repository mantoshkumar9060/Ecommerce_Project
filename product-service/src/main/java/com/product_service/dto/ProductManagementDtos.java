package com.product_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProductManagementDtos {
    private ProductManagementDtos() { }
    public record Create(@NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 80) String sku,
                         @Size(max = 4000) String description, @NotNull Integer subCategoryId) { }
    public record Update(@NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 80) String sku,
                         @Size(max = 4000) String description, @NotNull Integer subCategoryId, boolean active) { }
}

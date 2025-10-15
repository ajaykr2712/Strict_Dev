package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import com.ecommerce.entity.Product;

public record ProductStatusUpdateRequest(
        @NotNull Product.ProductStatus newStatus
) {}

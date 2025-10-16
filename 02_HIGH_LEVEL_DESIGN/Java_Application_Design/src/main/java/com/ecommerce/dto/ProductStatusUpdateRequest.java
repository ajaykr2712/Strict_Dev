package com.ecommerce.dto;

import com.ecommerce.entity.Product;

import jakarta.validation.constraints.NotNull;

public record ProductStatusUpdateRequest(
        @NotNull Product.ProductStatus newStatus
) {}

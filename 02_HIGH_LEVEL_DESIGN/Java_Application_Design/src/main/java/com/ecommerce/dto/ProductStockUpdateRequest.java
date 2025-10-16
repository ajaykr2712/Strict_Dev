package com.ecommerce.dto;

import jakarta.validation.constraints.Min;

public record ProductStockUpdateRequest(
        @Min(0) int newStock
) {}

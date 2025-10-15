package com.ecommerce.dto;

import jakarta.validation.constraints.*;

public record ProductStockUpdateRequest(
        @Min(0) int newStock
) {}

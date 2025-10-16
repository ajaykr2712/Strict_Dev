package com.ecommerce.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ProductPriceUpdateRequest(
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal newPrice
) {}

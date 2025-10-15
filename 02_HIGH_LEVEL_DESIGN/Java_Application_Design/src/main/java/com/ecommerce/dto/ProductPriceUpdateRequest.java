package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductPriceUpdateRequest(
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal newPrice
) {}

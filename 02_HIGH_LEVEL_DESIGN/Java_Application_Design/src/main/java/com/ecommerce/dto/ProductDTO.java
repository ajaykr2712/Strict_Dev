package com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductDTO(
        String id,
        String name,
        String description,
        BigDecimal price,
        String category,
        String status,
        Integer stockQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

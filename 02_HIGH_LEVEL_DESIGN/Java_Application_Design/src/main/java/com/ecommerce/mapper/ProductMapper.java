package com.ecommerce.mapper;

import com.ecommerce.dto.ProductDTO;
import com.ecommerce.entity.Product;

public final class ProductMapper {
    private ProductMapper() {}

    public static ProductDTO toDto(Product p) {
        if (p == null) return null;
        return new ProductDTO(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getCategory().name(),
                p.getStatus().name(),
                p.getStockQuantity(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}

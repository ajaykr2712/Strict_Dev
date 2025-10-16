package com.ecommerce.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.entity.Product;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.repository.ProductRepository;

/**
 * Product Service - encapsulates business logic separate from controllers.
 * Uses JPA entity as aggregate root. DTO mapping handled in controller layer.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) { this.repository = repository; }

    @Transactional
    @CacheEvict(value = {"productById","allProducts"}, allEntries = true)
    public Product create(String name, String description, BigDecimal price) {
        Product product = new Product(java.util.UUID.randomUUID().toString(), name, description, price,
                Product.ProductCategory.OTHER, Product.ProductStatus.ACTIVE, 0);
        return repository.save(product);
    }

    @Cacheable(value = "productById", key = "#id")
    public Product get(String id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException("Product %s not found".formatted(id)));
    }

    @Cacheable(value = "allProducts")
    public List<Product> list() { return repository.findAllByOrderByCreatedAtDesc(); }

    @Transactional
    @CacheEvict(value = {"productById","allProducts"}, allEntries = true)
    public Product updatePrice(String id, BigDecimal newPrice) {
        Product current = get(id);
        current.updatePrice(newPrice);
        return repository.save(current);
    }

    @Transactional
    @CacheEvict(value = {"productById","allProducts"}, allEntries = true)
    public Product updateStock(String id, int newStock) {
        Product current = get(id);
        current.updateStock(newStock);
        return repository.save(current);
    }

    @Transactional
    @CacheEvict(value = {"productById","allProducts"}, allEntries = true)
    public Product updateStatus(String id, Product.ProductStatus status) {
        Product current = get(id);
        current.updateStatus(status);
        return repository.save(current);
    }
}

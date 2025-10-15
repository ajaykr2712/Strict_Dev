package com.ecommerce.api;

import com.ecommerce.dto.ProductDTO;
import com.ecommerce.entity.Product;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static com.ecommerce.mapper.ProductMapper.toDto;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) { this.productService = productService; }

    @PostMapping
    public ResponseEntity<ProductDTO> create(@RequestBody ProductDTO request) {
        Product product = productService.create(request.name(), request.description(), request.price());
        return ResponseEntity.created(URI.create("/api/products/" + product.getId())).body(toDto(product));
    }

    @GetMapping("/{id}")
    public ProductDTO get(@PathVariable String id) { return toDto(productService.get(id)); }

    @GetMapping
    public List<ProductDTO> list() {
        return productService.list().stream().map(ProductMapper::toDto).collect(Collectors.toList());
    }

    @PatchMapping("/{id}/price")
    public ProductDTO updatePrice(@PathVariable String id, @RequestParam BigDecimal price) {
        return toDto(productService.updatePrice(id, price));
    }
}

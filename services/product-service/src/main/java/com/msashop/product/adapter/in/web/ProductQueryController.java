package com.msashop.product.adapter.in.web;

import com.msashop.product.adapter.in.web.dto.ProductResponse;
import com.msashop.product.adapter.in.web.mapper.ProductWebQueryMapper;
import com.msashop.product.application.port.in.GetProductUseCase;
import com.msashop.product.application.port.in.GetProductsUseCase;
import com.msashop.product.application.port.in.model.ProductResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductQueryController {

    private final GetProductUseCase getProductUseCase;
    private final GetProductsUseCase getProductsUseCase;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts() {
        List<ProductResponse> responses = getProductsUseCase.getProducts().stream()
                .map(ProductWebQueryMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        ProductResult result = getProductUseCase.getProduct(productId);
        return ResponseEntity.ok(ProductWebQueryMapper.toResponse(result));
    }
}

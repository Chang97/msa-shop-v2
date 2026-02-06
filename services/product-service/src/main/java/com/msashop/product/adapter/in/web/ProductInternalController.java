package com.msashop.product.adapter.in.web;

import com.msashop.product.adapter.in.web.dto.ProductResponse;
import com.msashop.product.adapter.in.web.mapper.ProductWebQueryMapper;
import com.msashop.product.application.port.in.GetProductUseCase;
import com.msashop.product.application.port.in.model.ProductResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class ProductInternalController {

    private final GetProductUseCase getProductUseCase;

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        ProductResult result = getProductUseCase.getProduct(productId);
        return ResponseEntity.ok(ProductWebQueryMapper.toResponse(result));
    }
}


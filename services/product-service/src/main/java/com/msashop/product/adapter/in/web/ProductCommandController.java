package com.msashop.product.adapter.in.web;

import com.msashop.product.adapter.in.web.dto.CreateProductRequest;
import com.msashop.product.adapter.in.web.mapper.ProductWebCommandMapper;
import com.msashop.product.application.port.in.CreateProductUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductCommandController {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    private final CreateProductUseCase createProductUseCase;
    // 상품 등록
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<Long> create(@Valid @RequestBody CreateProductRequest request) {
        Long productId = createProductUseCase.createProduct(ProductWebCommandMapper.toCommand(request));

        return ResponseEntity.ok(productId);
    }
}


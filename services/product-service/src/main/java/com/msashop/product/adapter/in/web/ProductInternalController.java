package com.msashop.product.adapter.in.web;

import com.msashop.product.adapter.in.web.dto.ProductResponse;
import com.msashop.product.adapter.in.web.mapper.ProductWebQueryMapper;
import com.msashop.product.application.port.in.DecreaseStockUseCase;
import com.msashop.product.application.port.in.GetProductUseCase;
import com.msashop.product.application.port.in.model.ProductResult;
import com.msashop.product.application.port.in.model.DecreaseStockCommand;
import com.msashop.product.adapter.in.web.dto.DecreaseStockRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class ProductInternalController {

    private final GetProductUseCase getProductUseCase;
    private final DecreaseStockUseCase decreaseStockUseCase;

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        ProductResult result = getProductUseCase.getProduct(productId);
        return ResponseEntity.ok(ProductWebQueryMapper.toResponse(result));
    }

    @PostMapping("/decrease-stock")
    public ResponseEntity<Void> decreaseStock(@RequestBody DecreaseStockRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        var commands = request.items().stream()
                .map(i -> new DecreaseStockCommand(i.productId(), i.quantity()))
                .toList();
        decreaseStockUseCase.decreaseStocks(commands);
        return ResponseEntity.noContent().build();
    }
}

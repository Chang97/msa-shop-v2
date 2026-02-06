package com.msashop.order.adapter.out.client;

import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.NotFoundException;
import com.msashop.order.application.port.out.DecreaseProductStockPort;
import com.msashop.order.application.port.out.LoadProductPort;
import com.msashop.order.application.port.out.model.ProductRow;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductClientAdapter implements LoadProductPort, DecreaseProductStockPort {

    private final WebClient webClient;

    @Value("${clients.product.base-url}")
    private String productBaseUrl;

    @Value("${security.internal.header-name:X-Internal-Secret}")
    private String internalHeaderName;

    @Value("${security.internal.service-secret:local-internal-secret}")
    private String internalServiceSecret;

    @Override
    public List<ProductRow> loadProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        return productIds.stream()
                .distinct()
                .map(this::fetchProduct)
                .toList();
    }

    private ProductRow fetchProduct(Long productId) {
        String url = productBaseUrl.endsWith("/")
                ? productBaseUrl + productId
                : productBaseUrl + "/" + productId;
        try {
            ProductResponse res = webClient.get()
                    .uri(url)
                    .header(internalHeaderName, internalServiceSecret)
                    .retrieve()
                    .bodyToMono(ProductResponse.class)
                    .block();
            if (res == null) {
                throw new NotFoundException(CommonErrorCode.COMMON_NOT_FOUND, "product not found. productId: " + productId);
            }
            return new ProductRow(res.productId(), res.productName(), res.price(), res.stock(), res.status(), res.useYn());
        } catch (WebClientResponseException.NotFound e) {
            throw new NotFoundException(CommonErrorCode.COMMON_NOT_FOUND, "product not found. productId: " + productId);
        }
    }

    private record ProductResponse(Long productId,
                                   String productName,
                                   java.math.BigDecimal price,
                                   Integer stock,
                                   String status,
                                   Boolean useYn) {}

    @Override
    public void decreaseStocks(Map<Long, Integer> quantitiesByProductId) {
        if (quantitiesByProductId == null || quantitiesByProductId.isEmpty()) {
            return;
        }

        List<StockItem> items = quantitiesByProductId.entrySet().stream()
                .map(e -> new StockItem(e.getKey(), e.getValue()))
                .toList();

        String url = productBaseUrl.endsWith("/") ? productBaseUrl + "decrease-stock" : productBaseUrl + "/decrease-stock";

        webClient.post()
                .uri(url)
                .header(internalHeaderName, internalServiceSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new StockDecreaseRequest(items))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private record StockDecreaseRequest(List<StockItem> items) {}

    private record StockItem(Long productId, Integer quantity) {}
}

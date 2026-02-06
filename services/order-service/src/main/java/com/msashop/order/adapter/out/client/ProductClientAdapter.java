package com.msashop.order.adapter.out.client;

import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.NotFoundException;
import com.msashop.order.application.port.out.LoadProductPort;
import com.msashop.order.application.port.out.model.ProductRow;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductClientAdapter implements LoadProductPort {

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
}

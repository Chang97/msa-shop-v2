package com.msashop.product.adapter.in.web.dto;

import java.util.List;

public record DecreaseStockRequest(List<Item> items) {
    public record Item(Long productId, Integer quantity) {}
}


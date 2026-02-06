package com.msashop.product.application.port.in.model;

public record DecreaseStockCommand(Long productId, int quantity) {}


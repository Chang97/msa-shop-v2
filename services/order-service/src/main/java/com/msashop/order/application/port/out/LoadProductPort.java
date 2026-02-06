package com.msashop.order.application.port.out;

import com.msashop.order.application.port.out.model.ProductRow;

import java.util.List;

public interface LoadProductPort {
    List<ProductRow> loadProducts(List<Long> productIds);
}

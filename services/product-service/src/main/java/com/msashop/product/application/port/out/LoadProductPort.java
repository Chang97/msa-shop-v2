package com.msashop.product.application.port.out;

import com.msashop.product.application.port.out.model.ProductRow;
import com.msashop.product.domain.model.Product;

import java.util.List;

public interface LoadProductPort {
    ProductRow findById(Long productId);
    List<ProductRow> findAll();
}

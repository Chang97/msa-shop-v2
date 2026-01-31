package com.msashop.product.application.port.out;

import com.msashop.product.domain.model.Product;

public interface SaveProductPort {
    Long save(Product product);
}

package com.msashop.order.application.port.out;

import java.util.Map;

public interface DecreaseProductStockPort {
    void decreaseStocks(Map<Long, Integer> quantitiesByProductId);
}


package com.msashop.product.application.port.in;

import com.msashop.product.application.port.in.model.DecreaseStockCommand;

import java.util.List;

public interface DecreaseStockUseCase {
    void decreaseStocks(List<DecreaseStockCommand> commands);
}


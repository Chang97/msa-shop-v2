package com.msashop.order.application.service;

import com.msashop.order.application.mapper.OrderCommandMapper;
import com.msashop.order.application.port.in.CreateOrderUseCase;
import com.msashop.order.application.port.in.model.CreateOrderCommand;
import com.msashop.order.application.port.out.LoadProductPort;
import com.msashop.order.application.port.out.OrderNumberPort;
import com.msashop.order.application.port.out.SaveOrderPort;
import com.msashop.order.application.port.out.SaveOrderStatusHistoryPort;
import com.msashop.order.application.port.out.model.ProductRow;
import com.msashop.common.web.exception.CommonErrorCode;
import com.msashop.common.web.exception.ConflictException;
import com.msashop.common.web.exception.NotFoundException;
import com.msashop.order.domain.model.Order;
import com.msashop.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateOrderService implements CreateOrderUseCase {

    private static final String STATUS_ON_SALE = "ON_SALE";

    private final OrderNumberPort orderNumberPort;
    private final SaveOrderPort saveOrderPort;
    private final SaveOrderStatusHistoryPort saveOrderStatusHistoryPort;
    private final LoadProductPort loadProductPort;

    @Override
    public Long createOrder(CreateOrderCommand command) {
        var productsById = loadProducts(command);
        validateProducts(command, productsById);

        String orderNumber = orderNumberPort.nextOrderNumber();
        Order order = OrderCommandMapper.toDomain(orderNumber, command, productsById);
        Long orderId = saveOrderPort.save(order);
        saveOrderStatusHistoryPort.saveHistory(orderId, null, OrderStatus.CREATED, "ORDER_CREATED", command.userId());
        return orderId;
    }

    private Map<Long, ProductRow> loadProducts(CreateOrderCommand command) {
        List<Long> ids = command.items().stream()
                .map(CreateOrderCommand.CreateOrderItem::productId)
                .distinct()
                .toList();
        List<ProductRow> products = loadProductPort.loadProducts(ids);
        Map<Long, ProductRow> map = products.stream()
                .collect(Collectors.toMap(ProductRow::productId, p -> p));
        if (map.size() != ids.size()) {
            throw new NotFoundException(CommonErrorCode.COMMON_NOT_FOUND, "one or more products not found");
        }
        return map;
    }

    private void validateProducts(CreateOrderCommand command, Map<Long, ProductRow> productsById) {
        Map<Long, Integer> requestedQty = buildRequestedQuantities(command);

        requestedQty.forEach((productId, qty) -> {
            ProductRow product = productsById.get(productId);
            if (product == null) {
                throw new NotFoundException(CommonErrorCode.COMMON_NOT_FOUND, "product not found. productId: " + productId);
            }
            if (product.useYn() == null || !product.useYn()) {
                throw new ConflictException(CommonErrorCode.COMMON_CONFLICT, "product disabled. productId: " + product.productId());
            }
            if (!STATUS_ON_SALE.equals(product.status())) {
                throw new ConflictException(CommonErrorCode.COMMON_CONFLICT, "product not on sale. productId: " + product.productId());
            }
            if (product.stock() == null || product.stock() < qty) {
                throw new ConflictException(CommonErrorCode.COMMON_CONFLICT, "insufficient stock. productId: " + product.productId());
            }
        });
    }

    private Map<Long, Integer> buildRequestedQuantities(CreateOrderCommand command) {
        Map<Long, Integer> quantities = new java.util.HashMap<>();
        command.items().forEach(item -> quantities.merge(item.productId(), item.quantity(), Integer::sum));
        return quantities;
    }
}

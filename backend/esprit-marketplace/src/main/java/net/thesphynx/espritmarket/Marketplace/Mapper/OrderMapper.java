package net.thesphynx.espritmarket.Marketplace.Mapper;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Order;

@Component
public class OrderMapper {

    private final OrderLineMapper orderLineMapper;

    public OrderMapper(OrderLineMapper orderLineMapper) {
        this.orderLineMapper = orderLineMapper;
    }

    public Order toEntity(OrderRequest request) {
        if (request == null) return null;
        Order order = new Order();
        order.setDate(request.getDate());
        order.setStatus(request.getStatus());
        order.setTotalAmount(request.getTotalAmount());

        // ✅ shippingAddress supprimé

        if (request.getUserId() != null) {
            User user = new User();
            user.setId(request.getUserId());
            order.setUser(user);
        }

        if (request.getOrderLines() != null) {
            var lines = request.getOrderLines().stream()
                .map(orderLineMapper::toEntity)
                .collect(Collectors.toList());
            lines.forEach(line -> line.setOrder(order));
            order.setOrderLines(lines);
        }

        return order;
    }

    public OrderResponse toResponse(Order order) {
        if (order == null) return null;
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setDate(order.getDate());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());

        if (order.getUser() != null) {
            response.setUserId(order.getUser().getId());
        }

        if (order.getOrderLines() != null) {
            response.setOrderLines(order.getOrderLines().stream()
                .map(orderLineMapper::toResponse)
                .collect(Collectors.toList()));
        }

        return response;
    }
}
package net.thesphynx.espritmarket.Marketplace.Mapper;

import org.springframework.stereotype.Component;

import net.thesphynx.espritmarket.Marketplace.Dto.OrderLineRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderLineResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.OrderLine;
import net.thesphynx.espritmarket.Marketplace.Entity.Product;

@Component
public class OrderLineMapper {

    public OrderLine toEntity(OrderLineRequest request) {
        if (request == null) return null;
        OrderLine orderLine = new OrderLine();
        orderLine.setQuantity(request.getQuantity());
        orderLine.setPrice(request.getPrice());
        orderLine.setDimensionsLabel(request.getDimensionsLabel());
        orderLine.setWeight(request.getWeight());

        // ✅ order sera setté dans OrderMapper
        if (request.getProductId() != null) {
            Product product = new Product();
            product.setId(request.getProductId());
            orderLine.setProduct(product);
        }
        return orderLine;
    }

    public OrderLineResponse toResponse(OrderLine orderLine) {
        if (orderLine == null) return null;
        OrderLineResponse response = new OrderLineResponse();
        response.setId(orderLine.getId());
        response.setQuantity(orderLine.getQuantity());
        response.setPrice(orderLine.getPrice());
        response.setSubtotal(orderLine.getSubtotal());
        response.setDimensionsLabel(orderLine.getDimensionsLabel());
        response.setWeight(orderLine.getWeight());

        if (orderLine.getOrder() != null) {
            response.setOrderId(orderLine.getOrder().getId());
        }
        if (orderLine.getProduct() != null) {
            response.setProductId(orderLine.getProduct().getId());
            response.setProductName(orderLine.getProduct().getName());
            response.setProductImage(orderLine.getProduct().getImageUrl());
        }
        return response;
    }
}
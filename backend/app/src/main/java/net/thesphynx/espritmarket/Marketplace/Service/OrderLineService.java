package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Marketplace.Dto.OrderLineRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderLineResponse;
import net.thesphynx.espritmarket.Marketplace.Mapper.OrderLineMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.IOrderLineRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.IOrderRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.IProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderLineService {
    private final IOrderLineRepository orderLineRepository;
    private final OrderLineMapper orderLineMapper;
    private final IOrderRepository orderRepository;
    private final IProductRepository productRepository;

    public OrderLineService(IOrderLineRepository orderLineRepository, 
                          OrderLineMapper orderLineMapper,
                          IOrderRepository orderRepository,
                          IProductRepository productRepository) {
        this.orderLineRepository = orderLineRepository;
        this.orderLineMapper = orderLineMapper;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public List<OrderLineResponse> getAll() {
        return orderLineRepository.findAll()
                .stream()
                .map(orderLineMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<OrderLineResponse> getById(Long id) {
        return orderLineRepository.findById(id)
                .map(orderLineMapper::toResponse);
    }

    public OrderLineResponse create(OrderLineRequest request) {
        var entity = orderLineMapper.toEntity(request);

        if (request.getOrderId() == null) {
            throw new RuntimeException("Order ID is required for order line creation");
        }
        var order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + request.getOrderId()));
        entity.setOrder(order);
        
        // Source metadata from Request if provided, otherwise Snapshot from Product DB
        String dims = request.getDimensionsLabel();
        Double wgt = request.getWeight();

        if (request.getProductId() != null) {
            productRepository.findById(request.getProductId()).ifPresent(product -> {
                // If request didn't provide dimensions, take from product (fallback to N/A)
                if (dims == null || dims.isBlank() || "N/A".equalsIgnoreCase(dims)) {
                    entity.setDimensionsLabel(product.getDimensionsLabel() != null ? product.getDimensionsLabel() : "N/A");
                } else {
                    entity.setDimensionsLabel(dims);
                }

                // If request didn't provide weight (is null or <= 0), take from product (fallback to 0.0)
                if (wgt == null || wgt <= 0) {
                    Double productWeight = product.getWeight();
                    entity.setWeight(productWeight != null ? productWeight : 0.0);
                } else {
                    entity.setWeight(wgt);
                }
            });
        } else {
            // No product ID provided? Still use request values or N/A
            entity.setDimensionsLabel(dims != null ? dims : "N/A");
            entity.setWeight(wgt != null ? wgt : 0.0);
        }

        // Compute subtotal automatically
        entity.setSubtotal(entity.getPrice() * entity.getQuantity());
        return orderLineMapper.toResponse(orderLineRepository.save(entity));
    }

    public OrderLineResponse update(Long id, OrderLineRequest request) {
        var entity = orderLineMapper.toEntity(request);
        entity.setId(id);

        if (request.getOrderId() != null) {
            var order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + request.getOrderId()));
            entity.setOrder(order);
        }

        entity.setSubtotal(entity.getPrice() * entity.getQuantity());
        return orderLineMapper.toResponse(orderLineRepository.save(entity));
    }

    public void delete(Long id) {
        orderLineRepository.deleteById(id);
    }
}

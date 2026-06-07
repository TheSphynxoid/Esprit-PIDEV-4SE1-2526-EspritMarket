package net.thesphynx.espritmarket.Marketplace.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;

import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.Common.Event.NotificationEvent;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderDeliveryResponse;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Store;
import net.thesphynx.espritmarket.Marketplace.Mapper.OrderMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.IOrderRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.IProductRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.IStoreRepository;

@Service
@Slf4j
public class OrderService {

    private final IOrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final IProductRepository productRepository;
    private final IStoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(IOrderRepository orderRepository, OrderMapper orderMapper,
                        IProductRepository productRepository,
                        IStoreRepository storeRepository,
                        UserRepository userRepository,
                        ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<OrderResponse> getAll() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<OrderResponse> getById(Long id) {
        return orderRepository.findById(id)
                .map(orderMapper::toResponse);
    }

    public Optional<OrderDeliveryResponse> getDeliveryInfo(Long id) {
        return orderRepository.findById(id).map(order -> {
            OrderDeliveryResponse response = new OrderDeliveryResponse();
            response.setOrderId(order.getId());


            response.setStatus(order.getStatus());

            if (order.getOrderLines() != null) {
                List<OrderDeliveryResponse.OrderDeliveryLineDto> lines = order.getOrderLines()
                    .stream()
                    .map(line -> {
                        OrderDeliveryResponse.OrderDeliveryLineDto dto =
                            new OrderDeliveryResponse.OrderDeliveryLineDto();
                        if (line.getProduct() != null) {
                            dto.setProductId(line.getProduct().getId());
                            dto.setProductName(line.getProduct().getName());
                        }
                        dto.setQuantity(line.getQuantity());
                        dto.setPrice(line.getPrice());
                        dto.setSubtotal(line.getSubtotal());
                        dto.setDimensionsLabel(line.getDimensionsLabel());
                        dto.setWeight(line.getWeight());
                        return dto;
                    })
                    .collect(Collectors.toList());
                response.setProducts(lines);
            }

            return response;
        });
    }

    public Long resolveUserIdFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        log.info("Resolving userId for user: {}", username);
        return userRepository.findByEmail(username)
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Cannot resolve userId for user: " + username));
    }

    public OrderResponse create(OrderRequest request) {
        log.info("Creating order - UserId: {}", request.getUserId());

        var order = orderMapper.toEntity(request);

        if (order.getOrderLines() != null) {
            order.getOrderLines().forEach(line -> {
                line.setOrder(order);

                if (line.getProduct() != null && line.getProduct().getId() != null) {
                    productRepository.findById(line.getProduct().getId()).ifPresent(product -> {
                        if (line.getDimensionsLabel() == null
                                || line.getDimensionsLabel().isBlank()
                                || "N/A".equalsIgnoreCase(line.getDimensionsLabel())) {
                            line.setDimensionsLabel(product.getDimensionsLabel() != null
                                    ? product.getDimensionsLabel() : "N/A");
                        }
                        if (line.getWeight() == null || line.getWeight() <= 0) {
                            line.setWeight(Objects.requireNonNullElse(product.getWeight(), 0.0d));
                        }
                    });
                }

                if (line.getPrice() != null) {
                    line.setSubtotal(line.getPrice() * line.getQuantity());
                }
            });
        }

        var savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {} and {} lines",
                savedOrder.getId(),
                savedOrder.getOrderLines() != null ? savedOrder.getOrderLines().size() : 0);

        if (savedOrder.getOrderLines() != null) {
            savedOrder.getOrderLines().forEach(line -> {
                if (line.getProduct() != null && line.getProduct().getId() != null) {
                    productRepository.findById(line.getProduct().getId()).ifPresent(product -> {
                        Store store = product.getStore();
                        if (store != null) {
                            double currentBalance = Objects.requireNonNullElse(store.getBalance(), 0.0d);
                            double lineSubtotal = Objects.requireNonNullElse(line.getSubtotal(), 0.0d);
                            store.setBalance(currentBalance + lineSubtotal);
                            storeRepository.save(store);
                            log.info("Updated store '{}' balance: +{} DT (New balance: {} DT)",
                                    store.getName(), lineSubtotal, store.getBalance());
                        }
                    });
                }
            });
        }

        publishOrderNotifications(savedOrder);

        return orderMapper.toResponse(savedOrder);
    }

    public OrderResponse update(Long id, OrderRequest request) {
        var entity = orderMapper.toEntity(request);
        entity.setId(id);
        return orderMapper.toResponse(orderRepository.save(entity));
    }

    public void delete(Long id) {
        orderRepository.deleteById(id);
    }


    private void publishOrderNotifications(net.thesphynx.espritmarket.Marketplace.Entity.Order order) {
        if (order.getOrderLines() == null || order.getOrderLines().isEmpty()) {
            return;
        }

        Map<Long, OwnerOrderNotification> notificationsByOwnerId = new LinkedHashMap<>();

        order.getOrderLines().forEach(line -> {
            if (line.getProduct() == null || line.getProduct().getId() == null) {
                return;
            }
//bd
            productRepository.findById(line.getProduct().getId()).ifPresent(product -> {
                Store store = product.getStore();
                if (store == null || store.getOwner() == null || store.getOwner().getId() == null) {
                    return;
                }

                Long ownerId = store.getOwner().getId();
                OwnerOrderNotification notification = notificationsByOwnerId.computeIfAbsent(
                        ownerId,
                        id -> new OwnerOrderNotification(
                                id,
                                store.getOwner().getName(),
                                store.getName(),
                                order.getId()
                        )
                );

                double subtotal = Objects.requireNonNullElse(
                    line.getSubtotal(),
                    Objects.requireNonNullElse(line.getPrice(), 0.0d) * line.getQuantity()
                );

                notification.addLine(
                        product.getName(),
                        line.getQuantity(),
                        subtotal
                );
            });
        });

        notificationsByOwnerId.values().forEach(notification -> {
            if (notification.getItemCount() == 0) {
                return;
            }

            String title = "Nouvelle commande reçue";
            String message = notification.buildMessage();
            eventPublisher.publishEvent(new NotificationEvent(
                    notification.ownerId,
                    "ORDER_CREATED",
                    title,
                    message,
                    notification.orderId,
                    "Order"
            ));

            log.info("Published order notification for ownerId={} orderId={} items={}",
                    notification.ownerId, notification.orderId, notification.getItemCount());
        });
    }

    private static final class OwnerOrderNotification {
        private final Long ownerId;
        private final String ownerName;
        private final String storeName;
        private final Long orderId;
        private final List<String> lines = new ArrayList<>();
        private double totalAmount = 0.0;

        private OwnerOrderNotification(Long ownerId, String ownerName, String storeName, Long orderId) {
            this.ownerId = ownerId;
            this.ownerName = ownerName;
            this.storeName = storeName;
            this.orderId = orderId;
        }

        private void addLine(String productName, int quantity, double subtotal) {
            lines.add(String.format("%s x%d - %.2f DT", productName, quantity, subtotal));
            totalAmount += subtotal;
        }

        private int getItemCount() {
            return lines.size();
        }

        private String buildMessage() {
            String lineList = String.join(", ", lines);
            String recipient = ownerName != null && !ownerName.isBlank() ? ownerName : "votre boutique";
            return String.format(
                    "Bonjour %s, une nouvelle commande a été passée dans la boutique %s. Articles: %s. Total: %.2f DT. Commande #%d.",
                    recipient,
                    storeName != null && !storeName.isBlank() ? storeName : "votre boutique",
                    lineList,
                    totalAmount,
                    orderId
            );
        }
    }
}
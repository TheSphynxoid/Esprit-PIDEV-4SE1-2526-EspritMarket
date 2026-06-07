package net.thesphynx.espritmarket.Marketplace.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderDeliveryResponse;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.OrderResponse;
import net.thesphynx.espritmarket.Marketplace.Service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace/orders")
@Tag(name = "Marketplace - Orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List orders")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Orders retrieved")})
    public List<OrderResponse> getAll() {
        return orderService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return orderService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─── Endpoint pour l'équipe Delivery ─────────────────────────────────────
    @GetMapping("/{id}/delivery-info")
    @Operation(summary = "Get order info for delivery team")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delivery info retrieved"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderDeliveryResponse> getDeliveryInfo(@PathVariable Long id) {
        return orderService.getDeliveryInfo(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public OrderResponse create(@Valid @RequestBody OrderRequest request,
                                Authentication authentication) {
        if (request.getUserId() == null && authentication != null) {
            try {
                Long userId = orderService.resolveUserIdFromAuthentication(authentication);
                request.setUserId(userId);
            } catch (RuntimeException ex) {
                log.warn("Could not resolve userId from authentication '{}': {}",
                        authentication.getName(), ex.getMessage());
            }
        }
        return orderService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody OrderRequest request) {
        if (orderService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Order deleted"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (orderService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
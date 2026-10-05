package net.thesphynx.espritmarket.Delivery.Controller;

import net.thesphynx.espritmarket.Delivery.Dto.DeliveryDto;
import net.thesphynx.espritmarket.Delivery.Dto.DeliveryAddressViewDto;
import net.thesphynx.espritmarket.Delivery.Dto.MapTrackingDto;
import net.thesphynx.espritmarket.Delivery.Entity.Delivery;
import net.thesphynx.espritmarket.Delivery.Entity.DeliveryAddressDetails;
import net.thesphynx.espritmarket.Delivery.Entity.MapTracking;
import net.thesphynx.espritmarket.Delivery.Entity.Vehicule;
import net.thesphynx.espritmarket.Delivery.Service.DeliveryAddressDetailsService;
import net.thesphynx.espritmarket.Delivery.Service.DeliveryService;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Marketplace.Entity.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/delivery/deliveries")
@Tag(name = "Delivery - Deliveries")
@PreAuthorize("isAuthenticated()")
public class DeliveryController {
    private final DeliveryService deliveryService;
    private final DeliveryAddressDetailsService addressDetailsService;
    private final UserRepository userRepository;

    public DeliveryController(DeliveryService deliveryService,
                              DeliveryAddressDetailsService addressDetailsService,
                              UserRepository userRepository) {
        this.deliveryService = deliveryService;
        this.addressDetailsService = addressDetailsService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "List deliveries")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Deliveries retrieved")})
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_DELIVERY', 'COURIER', 'USER')")
    public List<Delivery> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        boolean canViewAllDeliveries = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority)
                        || "ROLE_ADMIN_DELIVERY".equals(authority)
                        || "ROLE_COURIER".equals(authority));

        if (canViewAllDeliveries) {
            return deliveryService.getAll();
        }

        return deliveryService.getAllForUser(resolveConnectedUserId(userDetails));
    }

    @GetMapping("/me")
    @Operation(summary = "List deliveries for connected user")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Deliveries retrieved")})
    public List<Delivery> getMine(@AuthenticationPrincipal UserDetails userDetails) {
        return deliveryService.getAllForUser(resolveConnectedUserId(userDetails));
    }

    @GetMapping("/my-addresses")
    @Operation(summary = "List connected user delivery addresses")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Addresses retrieved")})
    public List<DeliveryAddressViewDto> getMyAddresses(@AuthenticationPrincipal UserDetails userDetails) {
        return deliveryService.getAddressViewsByConnectedUserId(resolveConnectedUserId(userDetails));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get delivery by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delivery found"),
        @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<Delivery> getById(@PathVariable Long id) {
        return deliveryService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id:\\d+}/tracking")
    @Operation(summary = "Get delivery tracking details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tracking found"),
        @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<MapTracking> getTracking(@PathVariable Long id) {
        return deliveryService.getTrackingForDelivery(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id:\\d+}/tracking")
    @Operation(summary = "Update delivery tracking details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tracking updated"),
        @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<MapTracking> updateTracking(@PathVariable Long id,
                                                      @Valid @RequestBody MapTrackingDto request) {
        return deliveryService.updateTrackingForDelivery(id, toTrackingEntity(request))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id:\\d+}/qr-code")
    @Operation(summary = "Generate QR code for a delivery")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "QR code generated"),
        @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<byte[]> getQrCode(@PathVariable Long id) {
        return deliveryService.getById(id)
                .map(delivery -> {
                    Delivery deliveryWithToken = deliveryService.ensureQrToken(delivery);
                        String scanUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/delivery/deliveries/scan/")
                            .path(deliveryWithToken.getQrToken())
                            .toUriString();

                    byte[] qrCode = deliveryService.generateQrCodePng(scanUrl);
                    return ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_PNG)
                            .header(HttpHeaders.CACHE_CONTROL, "no-store")
                            .body(qrCode);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/invoice")
    @Operation(summary = "Download invoice PDF for delivery")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long id) {
        return deliveryService.getById(id)
                .map(delivery -> {
                    byte[] pdf = deliveryService.generateInvoicePdf(delivery);
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice_" + id + ".pdf")
                            .body(pdf);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/scan/{token}")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Scan delivery QR code and mark delivery as Delivered")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delivery marked as Delivered"),
        @ApiResponse(responseCode = "404", description = "Delivery not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Delivery> scanQrCode(@PathVariable String token) {
        return deliveryService.markDeliveredByQrToken(token)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create delivery")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Delivery created")})
    public Delivery create(@Valid @RequestBody DeliveryDto request,
                           @AuthenticationPrincipal UserDetails userDetails) {
        return deliveryService.create(toEntity(request, resolveConnectedUserId(userDetails)));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Update delivery")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delivery updated"),
        @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<Delivery> update(@PathVariable Long id,
                                           @RequestBody DeliveryDto request,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        return deliveryService.getById(id)
                .map(existing -> {
                    Long connectedUserId = resolveConnectedUserId(userDetails);
                    applyPartialUpdate(existing, request, connectedUserId);
                    return ResponseEntity.ok(deliveryService.update(id, existing));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id:\\d+}/vehicule/{vehiculeId:\\d+}")
    @Operation(summary = "Assign vehicule to delivery")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehicule assigned"),
        @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<Delivery> assignVehicule(@PathVariable Long id,
                                                   @PathVariable Long vehiculeId) {
        return deliveryService.assignVehicule(id, vehiculeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Delete delivery")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Delivery deleted"),
        @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_DELIVERY')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (deliveryService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        deliveryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Delivery toEntity(DeliveryDto request, Long connectedUserId) {
        Delivery delivery = new Delivery();
        delivery.setDeliverytype(request.getDeliverytype());
        delivery.setStatus(request.getStatus());
        delivery.setCancellationReason(request.getCancellationReason());
        delivery.setDeliverydate(request.getDeliverydate());
        delivery.setDeliveryMode(request.getDeliveryMode());
        delivery.setPaymentMode(request.getPaymentMode());
        delivery.setDistanceKm(request.getDistanceKm());
        delivery.setConnectedUserId(connectedUserId);
        applyAddressSelection(delivery, request, connectedUserId);

        if (request.getOrderId() != null) {
            Order order = new Order();
            order.setId(request.getOrderId());
            delivery.setOrder(order);
        }

        if (request.getVehiculeId() != null) {
            Vehicule vehicule = new Vehicule();
            vehicule.setId(request.getVehiculeId());
            delivery.setVehicule(vehicule);
        }

        if (request.getTracking() != null) {
            delivery.setTracking(toTrackingEntity(request.getTracking()));
        }

        // package weight/dimensions
        delivery.setRealWeight(request.getRealWeight());
        delivery.setLength(request.getLength());
        delivery.setWidth(request.getWidth());
        delivery.setHeight(request.getHeight());

        return delivery;
    }

    private Long resolveConnectedUserId(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        return userRepository.findByEmail(userDetails.getUsername())
                .map(user -> user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private MapTracking toTrackingEntity(MapTrackingDto request) {
        MapTracking tracking = new MapTracking();
        tracking.setCurrentLocation(request.getCurrentLocation());
        tracking.setLastUpdate(request.getLastUpdate());
        tracking.setEstimatedArrival(request.getEstimatedArrival());
        return tracking;
    }

    private DeliveryAddressDetails toAddressDetailsEntity(DeliveryDto request, Long connectedUserId) {
        DeliveryAddressDetails details = new DeliveryAddressDetails();
        details.setDeliveryAddress(request.getDeliveryAddress());
        details.setCity(request.getCity());
        details.setPostalCode(request.getPostalCode());
        details.setPhoneNumber(request.getPhoneNumber());
        details.setConnectedUserId(connectedUserId);
        return details;
    }

    private void applyAddressSelection(Delivery delivery, DeliveryDto request, Long connectedUserId) {
        if (request.getAddressDetailsId() != null) {
            DeliveryAddressDetails selectedAddress = addressDetailsService
                    .getByIdForUser(request.getAddressDetailsId(), connectedUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found for connected user"));

            delivery.setDeliveryAddressDetails(selectedAddress);
            delivery.setAddress(selectedAddress.getDeliveryAddress());
            delivery.setDeliveryAddress(selectedAddress.getDeliveryAddress());
            delivery.setCity(selectedAddress.getCity());
            delivery.setPostalCode(selectedAddress.getPostalCode());
            delivery.setPhoneNumber(selectedAddress.getPhoneNumber());
            return;
        }

        delivery.setAddress(request.getAddress() != null && !request.getAddress().isBlank()
                ? request.getAddress()
                : request.getDeliveryAddress());
        delivery.setDeliveryAddress(request.getDeliveryAddress());
        delivery.setCity(request.getCity());
        delivery.setPostalCode(request.getPostalCode());
        delivery.setPhoneNumber(request.getPhoneNumber());
        delivery.setDeliveryAddressDetails(toAddressDetailsEntity(request, connectedUserId));
    }

    private void applyPartialUpdate(Delivery existing, DeliveryDto request, Long connectedUserId) {
        // Keep original delivery owner; admin assignment must not transfer ownership.
        Long ownerUserId = existing.getConnectedUserId() != null
                ? existing.getConnectedUserId()
                : connectedUserId;

        if (hasText(request.getDeliverytype())) {
            existing.setDeliverytype(request.getDeliverytype());
        }
        if (hasText(request.getStatus())) {
            existing.setStatus(request.getStatus());
        }
        if (hasText(request.getCancellationReason())) {
            existing.setCancellationReason(request.getCancellationReason());
        }
        if (request.getDeliverydate() != null) {
            existing.setDeliverydate(request.getDeliverydate());
        }
        if (request.getDeliveryMode() != null) {
            existing.setDeliveryMode(request.getDeliveryMode());
        }
        if (request.getPaymentMode() != null) {
            existing.setPaymentMode(request.getPaymentMode());
        }
        if (request.getDistanceKm() != null) {
            existing.setDistanceKm(request.getDistanceKm());
        }

        if (request.getOrderId() != null) {
            Order order = new Order();
            order.setId(request.getOrderId());
            existing.setOrder(order);
        }

        if (request.getVehiculeId() != null) {
            Vehicule vehicule = new Vehicule();
            vehicule.setId(request.getVehiculeId());
            existing.setVehicule(vehicule);
        }

        if (request.getTracking() != null) {
            existing.setTracking(toTrackingEntity(request.getTracking()));
        }

        if (request.getRealWeight() != null) {
            existing.setRealWeight(request.getRealWeight());
        }
        if (request.getLength() != null) {
            existing.setLength(request.getLength());
        }
        if (request.getWidth() != null) {
            existing.setWidth(request.getWidth());
        }
        if (request.getHeight() != null) {
            existing.setHeight(request.getHeight());
        }

        boolean shouldApplyAddressSelection = request.getAddressDetailsId() != null
                || hasText(request.getAddress())
                || hasText(request.getDeliveryAddress())
                || hasText(request.getCity())
                || hasText(request.getPostalCode())
                || hasText(request.getPhoneNumber());

        if (!shouldApplyAddressSelection) {
            return;
        }

        if (request.getAddressDetailsId() != null) {
            applyAddressSelection(existing, request, connectedUserId);
            return;
        }

        String resolvedDeliveryAddress = hasText(request.getDeliveryAddress())
                ? request.getDeliveryAddress()
                : existing.getDeliveryAddress();
        String resolvedCity = hasText(request.getCity()) ? request.getCity() : existing.getCity();
        String resolvedPostalCode = hasText(request.getPostalCode()) ? request.getPostalCode() : existing.getPostalCode();
        String resolvedPhoneNumber = hasText(request.getPhoneNumber()) ? request.getPhoneNumber() : existing.getPhoneNumber();
        String resolvedAddress = hasText(request.getAddress())
                ? request.getAddress()
                : (resolvedDeliveryAddress != null ? resolvedDeliveryAddress : existing.getAddress());

        existing.setAddress(resolvedAddress);
        existing.setDeliveryAddress(resolvedDeliveryAddress);
        existing.setCity(resolvedCity);
        existing.setPostalCode(resolvedPostalCode);
        existing.setPhoneNumber(resolvedPhoneNumber);

        DeliveryAddressDetails details = existing.getDeliveryAddressDetails();
        if (details == null) {
            details = new DeliveryAddressDetails();
            details.setConnectedUserId(ownerUserId);
            existing.setDeliveryAddressDetails(details);
        }

        details.setDeliveryAddress(resolvedDeliveryAddress);
        details.setCity(resolvedCity);
        details.setPostalCode(resolvedPostalCode);
        details.setPhoneNumber(resolvedPhoneNumber);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

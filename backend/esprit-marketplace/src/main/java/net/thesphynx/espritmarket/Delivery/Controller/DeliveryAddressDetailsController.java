package net.thesphynx.espritmarket.Delivery.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Delivery.Dto.DeliveryAddressDetailsDto;
import net.thesphynx.espritmarket.Delivery.Entity.DeliveryAddressDetails;
import net.thesphynx.espritmarket.Delivery.Service.DeliveryAddressDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/delivery/address-details")
@Tag(name = "Delivery - Address Details")
public class DeliveryAddressDetailsController {

    private final DeliveryAddressDetailsService addressDetailsService;
    private final UserRepository userRepository;

    public DeliveryAddressDetailsController(DeliveryAddressDetailsService addressDetailsService,
                                            UserRepository userRepository) {
        this.addressDetailsService = addressDetailsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    @Operation(summary = "List connected user address book")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Address book retrieved")})
    public List<DeliveryAddressDetails> getMine(@AuthenticationPrincipal UserDetails userDetails) {
        return addressDetailsService.getAllForUser(resolveConnectedUserId(userDetails));
    }

    @PostMapping("/me")
    @Operation(summary = "Add address to connected user address book")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Address created")})
    public DeliveryAddressDetails createMine(@Valid @RequestBody DeliveryAddressDetailsDto request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        return addressDetailsService.createForUser(toEntity(request), resolveConnectedUserId(userDetails));
    }

    @PutMapping("/me/{id}")
    @Operation(summary = "Update connected user address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address updated"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<DeliveryAddressDetails> updateMine(@PathVariable Long id,
                                                             @Valid @RequestBody DeliveryAddressDetailsDto request,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        return addressDetailsService.updateForUser(id, toEntity(request), resolveConnectedUserId(userDetails))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/me/{id}")
    @Operation(summary = "Delete connected user address")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Address deleted"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<Void> deleteMine(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        if (!addressDetailsService.deleteForUser(id, resolveConnectedUserId(userDetails))) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List address details")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Address details retrieved")})
    public List<DeliveryAddressDetails> getAll() {
        return addressDetailsService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address details by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address details found"),
            @ApiResponse(responseCode = "404", description = "Address details not found")
    })
    public ResponseEntity<DeliveryAddressDetails> getById(@PathVariable Long id) {
        return addressDetailsService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create address details")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Address details created")})
    public DeliveryAddressDetails create(@Valid @RequestBody DeliveryAddressDetailsDto request) {
        return addressDetailsService.create(toEntity(request), request.getDeliveryId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update address details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address details updated"),
            @ApiResponse(responseCode = "404", description = "Address details not found")
    })
    public ResponseEntity<DeliveryAddressDetails> update(@PathVariable Long id,
                                                         @Valid @RequestBody DeliveryAddressDetailsDto request) {
        if (addressDetailsService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(addressDetailsService.update(id, toEntity(request), request.getDeliveryId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address details")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Address details deleted"),
            @ApiResponse(responseCode = "404", description = "Address details not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (addressDetailsService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        addressDetailsService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private DeliveryAddressDetails toEntity(DeliveryAddressDetailsDto request) {
        DeliveryAddressDetails details = new DeliveryAddressDetails();
        details.setDeliveryAddress(request.getDeliveryAddress());
        details.setCity(request.getCity());
        details.setPostalCode(request.getPostalCode());
        details.setPhoneNumber(request.getPhoneNumber());
        return details;
    }

    private Long resolveConnectedUserId(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        return userRepository.findByEmail(userDetails.getUsername())
                .map(user -> user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }
}

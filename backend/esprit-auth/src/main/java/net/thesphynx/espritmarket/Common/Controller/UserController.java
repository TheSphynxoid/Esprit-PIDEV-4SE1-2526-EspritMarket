package net.thesphynx.espritmarket.Common.Controller;

import net.thesphynx.espritmarket.Common.DTO.UserRequest;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/common/users")
@Tag(name = "Common - Users")
@PreAuthorize("isAuthenticated()")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_DELIVERY', 'RECRUITER')")
    public List<User> getAll() {
        return userService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
        })
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return userService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User created")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_DELIVERY', 'RECRUITER')")
    public User create(@Valid @RequestBody UserRequest request) {
        return userService.create(toEntity(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_DELIVERY', 'RECRUITER')")
    public ResponseEntity<User> update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        if (userService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userService.update(id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_DELIVERY', 'RECRUITER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (userService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private User toEntity(UserRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        return user;
    }
}

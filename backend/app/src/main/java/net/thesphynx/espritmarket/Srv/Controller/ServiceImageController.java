package net.thesphynx.espritmarket.Srv.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Srv.Entity.Service;
import net.thesphynx.espritmarket.Srv.Repository.IServiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/srv/services")
@Tag(name = "Srv - Service Images")
public class ServiceImageController {

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> VALID_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private final String uploadDir;
    private final IServiceRepository serviceRepository;

    public ServiceImageController(@Value("${srv.upload-dir:/tmp/uploads/srv-images}") String uploadDir,
                                  IServiceRepository serviceRepository) {
        this.uploadDir = uploadDir;
        this.serviceRepository = serviceRepository;
    }

    @PostMapping("/{id}/image")
    @Operation(summary = "Upload service image")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Image uploaded"),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_PROVIDER')")
    public ResponseEntity<String> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service", id));

        String imageUrl = store(id, file);
        service.setImageUrl(imageUrl);
        serviceRepository.save(service);

        return ResponseEntity.ok(imageUrl);
    }

    @GetMapping("/images/{filename}")
    @Operation(summary = "Get service image")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Image returned"),
        @ApiResponse(responseCode = "404", description = "Image not found")
    })
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String detectedType = Files.probeContentType(filePath);
            MediaType contentType = detectedType != null
                    ? MediaType.parseMediaType(detectedType)
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String store(Long serviceId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size must not exceed 5MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new BadRequestException("Invalid filename");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        if (extension.isEmpty() || !VALID_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("Unsupported image extension: " + extension);
        }

        String filename = "service-" + serviceId + "-" + UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadPath, e);
        }

        Path targetLocation = uploadPath.resolve(filename);
        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }

        return "/api/srv/services/images/" + filename;
    }
}

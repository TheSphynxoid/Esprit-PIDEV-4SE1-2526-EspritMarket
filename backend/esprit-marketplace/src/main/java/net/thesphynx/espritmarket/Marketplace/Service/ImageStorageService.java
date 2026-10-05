package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageStorageService {
    private static final Logger logger = LoggerFactory.getLogger(ImageStorageService.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );
    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;

    private final Path productsDir;

    public ImageStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.productsDir = initializeUploadDirectory(uploadDir);
    }

    private Path initializeUploadDirectory(String uploadDir) {
        Path uploadPath = Path.of(uploadDir).toAbsolutePath().normalize();
        Path targetDir = uploadPath.resolve("products");
        
        try {
            Files.createDirectories(targetDir);
            logger.info("Using upload directory: {}", targetDir);
            return targetDir;
        } catch (IOException e) {
            throw new RuntimeException("Could not create product images directory at " + uploadPath, e);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Invalid image type. Allowed: JPEG, PNG, GIF, WebP, SVG");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Image file too large. Maximum size: 5 MB");
        }

        String extension = resolveExtension(contentType);
        String filename = UUID.randomUUID() + extension;
        Path target = productsDir.resolve(filename);

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Product image saved: {}", target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image file", e);
        }

        return "/uploads/products/" + filename;
    }

    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> ".bin";
        };
    }
}

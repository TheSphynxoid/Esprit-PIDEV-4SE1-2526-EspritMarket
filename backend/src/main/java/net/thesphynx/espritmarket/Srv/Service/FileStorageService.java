package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 25L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".csv", ".zip", ".rar", ".7z"
    );

    private final String uploadDir;
    private final String bookingUploadDir;

    public FileStorageService(
            @Value("${srv.deliverable.upload-dir:/tmp/uploads/deliverables}") String uploadDir,
            @Value("${srv.booking.upload-dir:/tmp/uploads/bookings}") String bookingUploadDir) {
        this.uploadDir = uploadDir;
        this.bookingUploadDir = bookingUploadDir;
    }

    public String store(MultipartFile file, Long deliverableId) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        int dotIndex = originalFilename.lastIndexOf('.');
        String extension = dotIndex >= 0 ? originalFilename.substring(dotIndex).toLowerCase() : "";

        String filename = "deliverable-" + deliverableId + "-" + UUID.randomUUID() + extension;

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

        return "/api/srv/deliverables/files/" + filename;
    }

    public void delete(String fileUrl) {
        if (fileUrl == null) return;

        String dir = null;
        if (fileUrl.startsWith("/api/srv/deliverables/files/")) {
            dir = uploadDir;
        } else if (fileUrl.startsWith("/api/srv/bookings/files/")) {
            dir = bookingUploadDir;
        }
        if (dir == null) return;

        String filename = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        Path filePath = Paths.get(dir).toAbsolutePath().normalize().resolve(filename);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    public String storeBookingFile(MultipartFile file, Long bookingId) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        int dotIndex = originalFilename.lastIndexOf('.');
        String extension = dotIndex >= 0 ? originalFilename.substring(dotIndex).toLowerCase() : "";

        String filename = "booking-" + bookingId + "-" + UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(bookingUploadDir).toAbsolutePath().normalize();
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

        return "/api/srv/bookings/files/" + filename;
    }

    public Path resolveBookingFilePath(String filename) {
        return Paths.get(bookingUploadDir).toAbsolutePath().normalize().resolve(filename);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size must not exceed 25MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new BadRequestException("Invalid filename");
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        String extension = dotIndex >= 0 ? originalFilename.substring(dotIndex).toLowerCase() : "";
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported file type: " + extension);
        }
    }

    public Path resolveFilePath(String filename) {
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
    }

    public String getContentType(Path filePath) {
        try {
            String detected = Files.probeContentType(filePath);
            return detected != null ? detected : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }
}

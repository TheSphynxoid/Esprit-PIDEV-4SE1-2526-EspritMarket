package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingAttachmentResponse {
    private Long id;
    private Long bookingId;
    private Long uploadedBy;
    private String uploadedByName;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadedAt;
}

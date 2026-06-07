package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliverableAttachmentResponse {
    private Long id;
    private Long deliverableId;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadedAt;
}

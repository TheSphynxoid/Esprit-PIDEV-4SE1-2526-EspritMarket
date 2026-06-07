package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliverableVersionAttachmentResponse {
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadedAt;
}

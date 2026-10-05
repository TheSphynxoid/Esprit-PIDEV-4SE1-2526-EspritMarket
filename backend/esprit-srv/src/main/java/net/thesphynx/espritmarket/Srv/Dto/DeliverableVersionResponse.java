package net.thesphynx.espritmarket.Srv.Dto;

import lombok.Data;
import net.thesphynx.espritmarket.Srv.Entity.DeliverableStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeliverableVersionResponse {
    private Long id;
    private Long deliverableId;
    private int versionNumber;
    private DeliverableStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private List<DeliverableVersionAttachmentResponse> attachments;
}

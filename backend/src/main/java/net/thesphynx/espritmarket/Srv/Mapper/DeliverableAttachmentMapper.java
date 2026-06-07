package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Srv.Dto.DeliverableAttachmentResponse;
import net.thesphynx.espritmarket.Srv.Entity.DeliverableAttachment;
import org.springframework.stereotype.Component;

@Component
public class DeliverableAttachmentMapper {
    public DeliverableAttachmentResponse toResponse(DeliverableAttachment attachment) {
        if (attachment == null) return null;
        DeliverableAttachmentResponse response = new DeliverableAttachmentResponse();
        response.setId(attachment.getId());
        if (attachment.getDeliverable() != null) {
            response.setDeliverableId(attachment.getDeliverable().getId());
        }
        response.setFileUrl(attachment.getFileUrl());
        response.setFileName(attachment.getFileName());
        response.setFileSize(attachment.getFileSize());
        response.setFileType(attachment.getFileType());
        response.setUploadedAt(attachment.getUploadedAt());
        return response;
    }
}

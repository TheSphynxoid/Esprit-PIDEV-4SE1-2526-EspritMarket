package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Srv.Dto.BookingAttachmentResponse;
import net.thesphynx.espritmarket.Srv.Entity.BookingAttachment;
import org.springframework.stereotype.Component;

@Component
public class BookingAttachmentMapper {
    public BookingAttachmentResponse toResponse(BookingAttachment attachment) {
        if (attachment == null) return null;
        BookingAttachmentResponse response = new BookingAttachmentResponse();
        response.setId(attachment.getId());
        if (attachment.getBooking() != null) {
            response.setBookingId(attachment.getBooking().getId());
        }
        if (attachment.getUploadedBy() != null) {
            response.setUploadedBy(attachment.getUploadedBy().getId());
            response.setUploadedByName(attachment.getUploadedBy().getEmail());
        }

        response.setFileUrl(attachment.getFileUrl());
        response.setFileName(attachment.getFileName());
        response.setFileSize(attachment.getFileSize());
        response.setFileType(attachment.getFileType());
        response.setUploadedAt(attachment.getUploadedAt());
        return response;
    }
}
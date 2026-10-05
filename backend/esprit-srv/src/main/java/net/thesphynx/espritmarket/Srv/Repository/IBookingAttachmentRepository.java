package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.BookingAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IBookingAttachmentRepository extends JpaRepository<BookingAttachment, Long> {
    List<BookingAttachment> findByBookingIdOrderByUploadedAtAsc(Long bookingId);
    Optional<BookingAttachment> findByFileUrl(String fileUrl);
}

package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Dto.BookingAttachmentResponse;
import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.BookingAttachment;
import net.thesphynx.espritmarket.Srv.Mapper.BookingAttachmentMapper;
import net.thesphynx.espritmarket.Srv.Repository.IBookingAttachmentRepository;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingAttachmentService {

    private final IBookingAttachmentRepository attachmentRepository;
    private final IBookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final BookingAttachmentMapper attachmentMapper;

    public BookingAttachmentService(IBookingAttachmentRepository attachmentRepository,
                                    IBookingRepository bookingRepository,
                                    UserRepository userRepository,
                                    FileStorageService fileStorageService,
                                    BookingAttachmentMapper attachmentMapper) {
        this.attachmentRepository = attachmentRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.attachmentMapper = attachmentMapper;
    }

    @Transactional
    public BookingAttachmentResponse upload(Long bookingId, Long userId, MultipartFile file) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!isParticipant(booking, userId)) {
            throw new BadRequestException("You are not authorized to upload files for this booking");
        }

        String fileUrl = fileStorageService.storeBookingFile(file, bookingId);

        BookingAttachment attachment = new BookingAttachment();
        attachment.setBooking(booking);
        attachment.setUploadedBy(user);
        attachment.setFileUrl(fileUrl);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());

        BookingAttachment saved = attachmentRepository.save(attachment);
        return attachmentMapper.toResponse(saved);
    }

    @Transactional
    public List<BookingAttachmentResponse> getByBookingId(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!isParticipant(booking, userId)) {
            throw new BadRequestException("You are not authorized to view files for this booking");
        }

        cleanupOrphanAttachmentsForBookingInternal(bookingId);

        return attachmentRepository.findByBookingIdOrderByUploadedAtAsc(bookingId).stream()
                .map(attachmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public int cleanupOrphanAttachmentsForBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!isParticipant(booking, userId)) {
            throw new BadRequestException("You are not authorized to clean files for this booking");
        }

        return cleanupOrphanAttachmentsForBookingInternal(bookingId);
    }

    @Transactional
    public boolean deleteIfPhysicalFileMissing(BookingAttachment attachment) {
        if (attachment == null) return false;
        if (isBookingFilePresent(attachment.getFileUrl())) return false;
        attachmentRepository.deleteById(attachment.getId());
        return true;
    }

    public Optional<BookingAttachment> getByFileUrl(String fileUrl) {
        return attachmentRepository.findByFileUrl(fileUrl);
    }

    public boolean canAccessFile(BookingAttachment attachment, Long userId) {
        Booking booking = attachment.getBooking();
        return isParticipant(booking, userId);
    }

    private int cleanupOrphanAttachmentsForBookingInternal(Long bookingId) {
        List<BookingAttachment> attachments = attachmentRepository.findByBookingIdOrderByUploadedAtAsc(bookingId);
        int removed = 0;
        for (BookingAttachment attachment : attachments) {
            if (!isBookingFilePresent(attachment.getFileUrl())) {
                attachmentRepository.delete(attachment);
                removed++;
            }
        }
        return removed;
    }

    private boolean isBookingFilePresent(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/api/srv/bookings/files/")) {
            return false;
        }

        String filename = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        Path path = fileStorageService.resolveBookingFilePath(filename);
        return Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path);
    }

    @Transactional
    public void delete(Long bookingId, Long attachmentId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        BookingAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("BookingAttachment", attachmentId));

        if (!attachment.getBooking().getId().equals(bookingId)) {
            throw new BadRequestException("Attachment does not belong to this booking");
        }

        if (!attachment.getUploadedBy().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own files");
        }

        fileStorageService.delete(attachment.getFileUrl());
        attachmentRepository.delete(attachment);
    }

    private boolean isParticipant(Booking booking, Long userId) {
        if (booking.getUser() != null && booking.getUser().getId().equals(userId)) return true;
        return isProvider(booking, userId);
    }

    private boolean isProvider(Booking booking, Long userId) {
        return booking.getProvider() != null && booking.getProvider().getId().equals(userId);
    }
}

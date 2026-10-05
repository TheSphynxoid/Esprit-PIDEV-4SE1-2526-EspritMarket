package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Srv.Dto.BookingRequest;
import net.thesphynx.espritmarket.Srv.Dto.BookingResponse;
import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.BookingStatus;
import net.thesphynx.espritmarket.Srv.Entity.Partner;
import net.thesphynx.espritmarket.Srv.Entity.Project;
import net.thesphynx.espritmarket.Srv.Entity.Service;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BookingMapper {
    public Booking toEntity(BookingRequest request) {
        if (request == null) return null;
        Booking booking = new Booking();
        booking.setDate(request.getDate());
        booking.setDuration(request.getDuration());
        booking.setNotes(request.getNotes());
        booking.setStatus(BookingStatus.PENDING);

        Service service = new Service();
        service.setId(request.getServiceId());
        booking.setService(service);

        if (request.getPartnerId() != null) {
            Partner partner = new Partner();
            partner.setId(request.getPartnerId());
            booking.setPartner(partner);
        }

        if (request.getProjectId() != null) {
            Project project = new Project();
            project.setId(request.getProjectId());
            booking.setProject(project);
        }

        return booking;
    }

    public BookingResponse toResponse(Booking booking) {
        if (booking == null) return null;
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setDate(booking.getDate());
        response.setDuration(booking.getDuration());
        response.setStatus(booking.getStatus());
        response.setNotes(booking.getNotes());
        response.setTotalPrice(booking.getTotalPrice());
        response.setPriorityMarkup(booking.getPriorityMarkup());
        response.setHighPriority(booking.getPriorityMarkup() != null && booking.getPriorityMarkup().compareTo(BigDecimal.ZERO) > 0);
        if (booking.getUser() != null) {
            response.setUserId(booking.getUser().getId());
            response.setUserName(booking.getUser().getName());
        }
        if (booking.getService() != null) {
            response.setServiceId(booking.getService().getId());
            response.setServiceName(booking.getService().getName());
        }
        if (booking.getProvider() != null) {
            response.setProviderId(booking.getProvider().getId());
            response.setProviderName(booking.getProvider().getName());
        }
        if (booking.getPartner() != null) {
            response.setPartnerId(booking.getPartner().getId());
        }
        if (booking.getProject() != null) {
            response.setProjectId(booking.getProject().getId());
            response.setProjectTitle(booking.getProject().getTitle());
        }
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        return response;
    }
}

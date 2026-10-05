package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Srv.Dto.WeeklyTemplateRequest;
import net.thesphynx.espritmarket.Srv.Dto.WeeklyTemplateResponse;
import net.thesphynx.espritmarket.Srv.Entity.ProviderWeeklyTemplate;
import net.thesphynx.espritmarket.Srv.Entity.Service;
import org.springframework.stereotype.Component;

@Component
public class WeeklyTemplateMapper {
    public ProviderWeeklyTemplate toEntity(WeeklyTemplateRequest request) {
        if (request == null) return null;
        ProviderWeeklyTemplate template = new ProviderWeeklyTemplate();
        template.setDayOfWeek(request.getDayOfWeek());
        template.setStartHour(request.getStartHour());
        template.setEndHour(request.getEndHour());
        template.setSlotDurationMinutes(request.getSlotDurationMinutes());
        template.setMaxConcurrent(request.getMaxConcurrent());

        User provider = new User();
        provider.setId(request.getProviderId());
        template.setProvider(provider);

        if (request.getServiceId() != null) {
            Service service = new Service();
            service.setId(request.getServiceId());
            template.setService(service);
        }

        return template;
    }

    public WeeklyTemplateResponse toResponse(ProviderWeeklyTemplate template) {
        if (template == null) return null;
        WeeklyTemplateResponse response = new WeeklyTemplateResponse();
        response.setId(template.getId());
        response.setDayOfWeek(template.getDayOfWeek());
        response.setStartHour(template.getStartHour());
        response.setEndHour(template.getEndHour());
        response.setSlotDurationMinutes(template.getSlotDurationMinutes());
        response.setMaxConcurrent(template.getMaxConcurrent());
        if (template.getProvider() != null) {
            response.setProviderId(template.getProvider().getId());
            response.setProviderName(template.getProvider().getName());
        }
        if (template.getService() != null) {
            response.setServiceId(template.getService().getId());
            response.setServiceName(template.getService().getName());
        }
        return response;
    }
}

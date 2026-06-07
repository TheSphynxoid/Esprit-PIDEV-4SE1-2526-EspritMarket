package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Srv.Dto.WeeklyTemplateBatchRequest;
import net.thesphynx.espritmarket.Srv.Dto.WeeklyTemplateRequest;
import net.thesphynx.espritmarket.Srv.Dto.WeeklyTemplateResponse;
import net.thesphynx.espritmarket.Srv.Entity.ProviderWeeklyTemplate;
import net.thesphynx.espritmarket.Srv.Mapper.WeeklyTemplateMapper;
import net.thesphynx.espritmarket.Srv.Repository.IProviderWeeklyTemplateRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
public class WeeklyTemplateService {
    private final IProviderWeeklyTemplateRepository templateRepository;
    private final WeeklyTemplateMapper templateMapper;

    public WeeklyTemplateService(IProviderWeeklyTemplateRepository templateRepository,
                                 WeeklyTemplateMapper templateMapper) {
        this.templateRepository = templateRepository;
        this.templateMapper = templateMapper;
    }

    public List<WeeklyTemplateResponse> getByProvider(Long providerId) {
        return templateRepository.findByProviderId(providerId).stream()
                .map(templateMapper::toResponse)
                .toList();
    }

    public List<WeeklyTemplateResponse> getGlobalByProvider(Long providerId) {
        return templateRepository.findByProviderIdAndServiceIdIsNull(providerId).stream()
                .map(templateMapper::toResponse)
                .toList();
    }

    public List<WeeklyTemplateResponse> getByProviderAndService(Long providerId, Long serviceId) {
        return templateRepository.findByProviderIdAndServiceId(providerId, serviceId).stream()
                .map(templateMapper::toResponse)
                .toList();
    }

    public WeeklyTemplateResponse create(WeeklyTemplateRequest request) {
        ProviderWeeklyTemplate entity = templateMapper.toEntity(request);
        return templateMapper.toResponse(templateRepository.save(entity));
    }

    @Transactional
    public List<WeeklyTemplateResponse> batchReplace(WeeklyTemplateBatchRequest request) {
        if (request.getServiceId() != null) {
            templateRepository.deleteByProviderIdAndServiceId(request.getProviderId(), request.getServiceId());
        } else {
            templateRepository.deleteByProviderIdAndServiceIdIsNull(request.getProviderId());
        }
        templateRepository.flush();

        User provider = new User();
        provider.setId(request.getProviderId());

        net.thesphynx.espritmarket.Srv.Entity.Service service = null;
        if (request.getServiceId() != null) {
            service = new net.thesphynx.espritmarket.Srv.Entity.Service();
            service.setId(request.getServiceId());
        }

        List<ProviderWeeklyTemplate> saved = new ArrayList<>();
        for (WeeklyTemplateBatchRequest.DayEntry entry : request.getEntries()) {
            ProviderWeeklyTemplate template = new ProviderWeeklyTemplate();
            template.setProvider(provider);
            template.setService(service);
            template.setDayOfWeek(entry.getDayOfWeek());
            template.setStartHour(entry.getStartHour());
            template.setEndHour(entry.getEndHour());
            template.setSlotDurationMinutes(request.getSlotDurationMinutes());
            template.setMaxConcurrent(request.getMaxConcurrent());
            saved.add(templateRepository.save(template));
        }

        return saved.stream()
                .map(templateMapper::toResponse)
                .toList();
    }

    public Optional<WeeklyTemplateResponse> update(Long id, WeeklyTemplateRequest request) {
        return templateRepository.findById(id)
                .map(existing -> {
                    ProviderWeeklyTemplate entity = templateMapper.toEntity(request);
                    entity.setId(id);
                    return templateMapper.toResponse(templateRepository.save(entity));
                });
    }

    public boolean delete(Long id) {
        if (!templateRepository.existsById(id)) return false;
        templateRepository.deleteById(id);
        return true;
    }
}

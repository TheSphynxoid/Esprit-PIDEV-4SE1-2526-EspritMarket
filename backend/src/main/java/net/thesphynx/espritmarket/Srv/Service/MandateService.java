package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Srv.Dto.ProviderMandateRequest;
import net.thesphynx.espritmarket.Srv.Dto.ProviderMandateResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceMandateRequest;
import net.thesphynx.espritmarket.Srv.Dto.ServiceMandateResponse;
import net.thesphynx.espritmarket.Srv.Entity.ProviderMandate;
import net.thesphynx.espritmarket.Srv.Entity.ServiceMandate;
import net.thesphynx.espritmarket.Srv.Mapper.MandateMapper;
import net.thesphynx.espritmarket.Srv.Repository.IProviderMandateRepository;
import net.thesphynx.espritmarket.Srv.Repository.IServiceMandateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MandateService {
    private final IServiceMandateRepository serviceMandateRepository;
    private final IProviderMandateRepository providerMandateRepository;
    private final AvailabilityService availabilityService;
    private final MandateMapper mandateMapper;

    public MandateService(IServiceMandateRepository serviceMandateRepository,
                          IProviderMandateRepository providerMandateRepository,
                          AvailabilityService availabilityService,
                          MandateMapper mandateMapper) {
        this.serviceMandateRepository = serviceMandateRepository;
        this.providerMandateRepository = providerMandateRepository;
        this.availabilityService = availabilityService;
        this.mandateMapper = mandateMapper;
    }

    public List<ServiceMandateResponse> getServiceMandates(Long providerId) {
        return serviceMandateRepository.findByProviderId(providerId).stream()
                .map(m -> mandateMapper.toServiceMandateResponse(m,
                        availabilityService.countActiveBookingsForService(providerId, m.getService().getId()),
                        availabilityService.isServiceOverbooked(providerId, m.getService().getId())))
                .toList();
    }

    public ServiceMandateResponse createServiceMandate(ServiceMandateRequest request) {
        ServiceMandate mandate = serviceMandateRepository
                .findByProviderIdAndServiceId(request.getProviderId(), request.getServiceId())
                .orElseGet(ServiceMandate::new);

        User provider = new User();
        provider.setId(request.getProviderId());
        mandate.setProvider(provider);

        net.thesphynx.espritmarket.Srv.Entity.Service service = new net.thesphynx.espritmarket.Srv.Entity.Service();
        service.setId(request.getServiceId());
        mandate.setService(service);

        mandate.setMaxBookings(request.getMaxBookings());

        ServiceMandate saved = serviceMandateRepository.save(mandate);
        return mandateMapper.toServiceMandateResponse(saved,
                availabilityService.countActiveBookingsForService(request.getProviderId(), request.getServiceId()),
                availabilityService.isServiceOverbooked(request.getProviderId(), request.getServiceId()));
    }

    public boolean deleteServiceMandate(Long id) {
        if (!serviceMandateRepository.existsById(id)) return false;
        serviceMandateRepository.deleteById(id);
        return true;
    }

    public Optional<ProviderMandateResponse> getProviderMandate(Long providerId) {
        return providerMandateRepository.findByProviderId(providerId)
                .map(m -> mandateMapper.toProviderMandateResponse(m,
                        availabilityService.countActiveBookingsForProvider(providerId),
                        availabilityService.isProviderOverbooked(providerId)));
    }

    public ProviderMandateResponse createProviderMandate(ProviderMandateRequest request) {
        ProviderMandate mandate = providerMandateRepository
                .findByProviderId(request.getProviderId())
                .orElseGet(ProviderMandate::new);

        User provider = new User();
        provider.setId(request.getProviderId());
        mandate.setProvider(provider);

        mandate.setMaxBookings(request.getMaxBookings());

        ProviderMandate saved = providerMandateRepository.save(mandate);
        return mandateMapper.toProviderMandateResponse(saved,
                availabilityService.countActiveBookingsForProvider(request.getProviderId()),
                availabilityService.isProviderOverbooked(request.getProviderId()));
    }

    public boolean deleteProviderMandate(Long id) {
        if (!providerMandateRepository.existsById(id)) return false;
        providerMandateRepository.deleteById(id);
        return true;
    }
}

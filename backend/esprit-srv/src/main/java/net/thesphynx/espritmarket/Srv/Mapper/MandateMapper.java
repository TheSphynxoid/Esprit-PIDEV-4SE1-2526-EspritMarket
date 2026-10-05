package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Srv.Dto.ProviderMandateResponse;
import net.thesphynx.espritmarket.Srv.Dto.ServiceMandateResponse;
import net.thesphynx.espritmarket.Srv.Entity.ProviderMandate;
import net.thesphynx.espritmarket.Srv.Entity.ServiceMandate;
import org.springframework.stereotype.Component;

@Component
public class MandateMapper {

    public ServiceMandateResponse toServiceMandateResponse(ServiceMandate mandate, int currentBookings, boolean overbooked) {
        if (mandate == null) return null;
        ServiceMandateResponse response = new ServiceMandateResponse();
        response.setId(mandate.getId());
        response.setProviderId(mandate.getProvider() != null ? mandate.getProvider().getId() : null);
        response.setProviderName(mandate.getProvider() != null ? mandate.getProvider().getName() : null);
        response.setServiceId(mandate.getService() != null ? mandate.getService().getId() : null);
        response.setServiceName(mandate.getService() != null ? mandate.getService().getName() : null);
        response.setMaxBookings(mandate.getMaxBookings());
        response.setCurrentBookings(currentBookings);
        response.setOverbooked(overbooked);
        return response;
    }

    public ProviderMandateResponse toProviderMandateResponse(ProviderMandate mandate, int currentBookings, boolean overbooked) {
        if (mandate == null) return null;
        ProviderMandateResponse response = new ProviderMandateResponse();
        response.setId(mandate.getId());
        response.setProviderId(mandate.getProvider() != null ? mandate.getProvider().getId() : null);
        response.setProviderName(mandate.getProvider() != null ? mandate.getProvider().getName() : null);
        response.setMaxBookings(mandate.getMaxBookings());
        response.setCurrentBookings(currentBookings);
        response.setOverbooked(overbooked);
        return response;
    }
}

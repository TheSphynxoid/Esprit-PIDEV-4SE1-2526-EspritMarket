package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Srv.Dto.ProviderExceptionRequest;
import net.thesphynx.espritmarket.Srv.Dto.ProviderExceptionResponse;
import net.thesphynx.espritmarket.Srv.Entity.ProviderException;
import org.springframework.stereotype.Component;

@Component
public class ProviderExceptionMapper {
    public ProviderException toEntity(ProviderExceptionRequest request) {
        if (request == null) return null;
        ProviderException entity = new ProviderException();
        entity.setDate(request.getDate());
        entity.setType(request.getType());
        entity.setStartHour(request.getStartHour());
        entity.setEndHour(request.getEndHour());
        entity.setReason(request.getReason());

        User provider = new User();
        provider.setId(request.getProviderId());
        entity.setProvider(provider);

        return entity;
    }

    public ProviderExceptionResponse toResponse(ProviderException entity) {
        if (entity == null) return null;
        ProviderExceptionResponse response = new ProviderExceptionResponse();
        response.setId(entity.getId());
        response.setDate(entity.getDate());
        response.setType(entity.getType());
        response.setStartHour(entity.getStartHour());
        response.setEndHour(entity.getEndHour());
        response.setReason(entity.getReason());
        if (entity.getProvider() != null) {
            response.setProviderId(entity.getProvider().getId());
            response.setProviderName(entity.getProvider().getName());
        }
        return response;
    }
}

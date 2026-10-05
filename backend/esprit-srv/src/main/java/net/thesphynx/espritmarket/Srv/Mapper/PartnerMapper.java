package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Srv.Dto.PartnerRequest;
import net.thesphynx.espritmarket.Srv.Dto.PartnerResponse;
import net.thesphynx.espritmarket.Srv.Entity.Partner;
import org.springframework.stereotype.Component;

@Component
public class PartnerMapper {
    public Partner toEntity(PartnerRequest request) {
        if (request == null) return null;
        Partner partner = new Partner();
        partner.setName(request.getName());
        partner.setContactInfo(request.getContactInfo());
        partner.setRole(request.getRole());
        return partner;
    }

    public PartnerResponse toResponse(Partner partner) {
        if (partner == null) return null;
        PartnerResponse response = new PartnerResponse();
        response.setId(partner.getId());
        response.setName(partner.getName());
        response.setContactInfo(partner.getContactInfo());
        response.setRole(partner.getRole());
        return response;
    }
}

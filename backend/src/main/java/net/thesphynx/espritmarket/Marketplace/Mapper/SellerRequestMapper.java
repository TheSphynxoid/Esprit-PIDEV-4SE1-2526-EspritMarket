package net.thesphynx.espritmarket.Marketplace.Mapper;

import net.thesphynx.espritmarket.Marketplace.Dto.SellerRequestRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.SellerRequestResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.SellerRequest;
import org.springframework.stereotype.Component;

@Component
public class SellerRequestMapper {

    public SellerRequest toEntity(SellerRequestRequest request) {
        if (request == null)
            return null;
        SellerRequest entity = new SellerRequest();
        entity.setNumeroEtudiant(request.getNumeroEtudiant());
        entity.setNom(request.getNom());
        entity.setPrenom(request.getPrenom());
        entity.setEmail(request.getEmail());
        entity.setCarteEtudiantUrl(request.getCarteEtudiantUrl());
        return entity;
    }

    public SellerRequestResponse toResponse(SellerRequest entity) {
        if (entity == null)
            return null;
        SellerRequestResponse response = new SellerRequestResponse();
        response.setId(entity.getId());
        response.setNumeroEtudiant(entity.getNumeroEtudiant());
        response.setNom(entity.getNom());
        response.setPrenom(entity.getPrenom());
        response.setEmail(entity.getEmail());
        response.setCarteEtudiantUrl(entity.getCarteEtudiantUrl());
        response.setStatut(entity.getStatut());
        response.setDateDemande(entity.getDateDemande());
        response.setDateValidation(entity.getDateValidation());

        if (entity.getUser() != null) {
            response.setUserId(entity.getUser().getId());
            response.setUserName(entity.getUser().getName());
        }

        if (entity.getValidatedBy() != null) {
            response.setValidatedById(entity.getValidatedBy().getId());
            response.setValidatedByName(entity.getValidatedBy().getName());
        }

        if (entity.getStatut() == net.thesphynx.espritmarket.Marketplace.Entity.RequestStatus.EN_ATTENTE) {
            response.setMessage("Votre demande a été envoyée et attend la vérification par l'admin.");
        }

        return response;
    }
}

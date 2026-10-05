package net.thesphynx.espritmarket.Marketplace.Dto;

import lombok.Data;
import net.thesphynx.espritmarket.Marketplace.Entity.RequestStatus;

import java.util.Date;

@Data
public class SellerRequestResponse {
    private Long id;
    private String numeroEtudiant;
    private String nom;
    private String prenom;
    private String email;
    private String carteEtudiantUrl;
    private RequestStatus statut;
    private Date dateDemande;
    private Date dateValidation;
    private Long userId;
    private String userName;
    private Long validatedById;
    private String validatedByName;
    private String message;
}

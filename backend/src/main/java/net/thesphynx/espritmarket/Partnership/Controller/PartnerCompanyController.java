package net.thesphynx.espritmarket.Partnership.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Partnership.Dto.PartnerCompanyRequest;
import net.thesphynx.espritmarket.Partnership.Entity.PartnerCompany;
import net.thesphynx.espritmarket.Partnership.Service.PartnerCompanyService;

import java.util.List;

@RestController
@RequestMapping("/api/partnership/companies")
@RequiredArgsConstructor
@CrossOrigin("*")
@PreAuthorize("isAuthenticated()")
public class PartnerCompanyController {

    private final PartnerCompanyService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'RECRUITER')")
    public PartnerCompany create(@Valid @RequestBody PartnerCompanyRequest request) {
        return service.create(toEntity(request));
    }

    @GetMapping
    public List<PartnerCompany> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public PartnerCompany getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'RECRUITER')")
    public PartnerCompany update(@PathVariable Long id,
                                 @Valid @RequestBody PartnerCompanyRequest request) {
        return service.update(id, toEntity(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARTNER', 'RECRUITER')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    private PartnerCompany toEntity(PartnerCompanyRequest request) {
        PartnerCompany company = new PartnerCompany();
        company.setName(request.getName());
        company.setSector(request.getSector());
        company.setContactEmail(request.getContactEmail());
        company.setPartnershipStatus(request.getPartnershipStatus());
        return company;
    }
}

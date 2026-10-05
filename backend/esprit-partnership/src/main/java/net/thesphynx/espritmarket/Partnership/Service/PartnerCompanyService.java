package net.thesphynx.espritmarket.Partnership.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import net.thesphynx.espritmarket.Partnership.Entity.PartnerCompany;
import net.thesphynx.espritmarket.Partnership.Repository.PartnerCompanyRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartnerCompanyService {

    private final PartnerCompanyRepository repository;

    public PartnerCompany create(PartnerCompany company) {
        return repository.save(company);
    }

    public List<PartnerCompany> getAll() {
        return repository.findAll();
    }

    public PartnerCompany getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));
    }

    public PartnerCompany update(Long id, PartnerCompany updated) {
        PartnerCompany existing = getById(id);

        existing.setName(updated.getName());
        existing.setSector(updated.getSector());
        existing.setContactEmail(updated.getContactEmail());
        existing.setPartnershipStatus(updated.getPartnershipStatus());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
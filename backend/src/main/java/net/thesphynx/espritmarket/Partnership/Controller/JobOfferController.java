package net.thesphynx.espritmarket.Partnership.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Partnership.Dto.JobOfferRequest;
import net.thesphynx.espritmarket.Partnership.Dto.JobOfferPerformanceDTO;
import net.thesphynx.espritmarket.Partnership.Entity.JobOffer;
import net.thesphynx.espritmarket.Partnership.Entity.ExperienceLevel;
import net.thesphynx.espritmarket.Partnership.Entity.PartnerCompany;
import net.thesphynx.espritmarket.Partnership.Service.JobOfferService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;

@RestController
@RequestMapping("/api/partnership/joboffers")
@RequiredArgsConstructor
@CrossOrigin("*")
public class JobOfferController {

    private final JobOfferService service;

    @PostMapping
    public JobOffer create(@Valid @RequestBody JobOfferRequest request) {
        return service.create(toEntity(request));
    }

    @GetMapping
    public List<JobOffer> getAll() {
        return service.getAll();
    }

    @GetMapping("/search")
    public Page<JobOffer> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {
        
        // Handle sorting
        Sort sortOrder = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        
        return service.search(keyword, type, location, experienceLevel, pageable);
    }

    @GetMapping("/performance-report")
    public List<JobOfferPerformanceDTO> getPerformanceReport() {
        return service.getPerformanceReport();
    }

    @GetMapping("/{id}")
    public JobOffer getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public JobOffer update(@PathVariable Long id,
                           @Valid @RequestBody JobOfferRequest request) {
        return service.update(id, toEntity(request));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    private JobOffer toEntity(JobOfferRequest request) {
        JobOffer offer = new JobOffer();
        offer.setTitle(request.getTitle());
        offer.setDescription(request.getDescription());
        offer.setType(request.getType());
        offer.setStatus(request.getStatus());
        offer.setLocation(request.getLocation());
        offer.setExperienceLevel(ExperienceLevel.valueOf(request.getExperienceLevel()));
        offer.setRequiredSkills(request.getRequiredSkills());

        PartnerCompany company = new PartnerCompany();
        company.setId(request.getCompanyId());
        offer.setCompany(company);

        return offer;
    }
}
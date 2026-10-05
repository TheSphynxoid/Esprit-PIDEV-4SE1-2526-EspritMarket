package net.thesphynx.espritmarket.Partnership.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import net.thesphynx.espritmarket.Partnership.Entity.JobOffer;
import net.thesphynx.espritmarket.Partnership.Entity.ExperienceLevel;
import net.thesphynx.espritmarket.Partnership.Dto.JobOfferPerformanceDTO;
import net.thesphynx.espritmarket.Partnership.Repository.JobOfferRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobOfferService {

    private final JobOfferRepository repository;

    public JobOffer create(JobOffer offer) {
        return repository.save(offer);
    }

    public List<JobOffer> getAll() {
        return repository.findAll();
    }

    public Page<JobOffer> search(String keyword, String type, String location, ExperienceLevel experienceLevel, Pageable pageable) {
        Specification<JobOffer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isEmpty()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("requiredSkills")), pattern)
                ));
            }

            if (type != null && !type.isEmpty()) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (location != null && !location.isEmpty()) {
                predicates.add(cb.equal(root.get("location"), location));
            }

            if (experienceLevel != null) {
                predicates.add(cb.equal(root.get("experienceLevel"), experienceLevel));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(spec, pageable);
    }

    public JobOffer getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("JobOffer not found"));
    }

    public JobOffer update(Long id, JobOffer updated) {
        JobOffer existing = getById(id);

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setType(updated.getType());
        existing.setStatus(updated.getStatus());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<JobOfferPerformanceDTO> getPerformanceReport() {
        return repository.getJobOfferPerformanceReport();
    }
}
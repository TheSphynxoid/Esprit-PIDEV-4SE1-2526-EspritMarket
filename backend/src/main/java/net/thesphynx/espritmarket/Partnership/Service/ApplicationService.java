package net.thesphynx.espritmarket.Partnership.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import net.thesphynx.espritmarket.Partnership.Entity.Application;
import net.thesphynx.espritmarket.Partnership.Entity.ApplicationActivityStatus;
import net.thesphynx.espritmarket.Partnership.Repository.ApplicationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService {

    private final ApplicationRepository repository;

    public Application create(Application application) {
        return repository.save(application);
    }

    @Transactional(readOnly = true)
    public List<Application> getAll() {
        return repository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Application> getByApplicantId(Long applicantId) {
        return repository.findByApplicantIdWithDetails(applicantId);
    }

    @Transactional(readOnly = true)
    public Application getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
    }

    public Application update(Long id, Application updated) {
        Application existing = getById(id);

        existing.setStatus(updated.getStatus());
        existing.setMatchingScore(updated.getMatchingScore());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    // ── Inactivity Detection Methods ─────────────────────

    /**
     * Refresh the candidate's last action timestamp.
     * Call this whenever the candidate performs an action (apply, update, etc.)
     */
    public void refreshCandidateActivity(Long applicationId) {
        Application app = getById(applicationId);
        app.setLastCandidateActionAt(LocalDateTime.now());
        app.setActivityStatus(ApplicationActivityStatus.ACTIVE);
        app.setFlagged(false);
        repository.save(app);
    }

    /**
     * Get all flagged (INACTIVE or AT_RISK) applications,
     * sorted by urgency (AT_RISK first).
     */
    @Transactional(readOnly = true)
    public List<Application> getFlaggedApplications() {
        return repository.findFlaggedApplications();
    }
}

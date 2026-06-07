package net.thesphynx.espritmarket.Partnership.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import net.thesphynx.espritmarket.Partnership.Dto.InterviewRequest;
import net.thesphynx.espritmarket.Partnership.Dto.InterviewUpdateRequest;
import net.thesphynx.espritmarket.Partnership.Entity.Application;
import net.thesphynx.espritmarket.Partnership.Entity.Interview;
import net.thesphynx.espritmarket.Partnership.Entity.InterviewResult;
import net.thesphynx.espritmarket.Partnership.Service.InterviewService;

import java.util.List;

@RestController
@RequestMapping("/api/partnership/interviews")
@RequiredArgsConstructor
@CrossOrigin("*")
public class InterviewController {

    private final InterviewService service;

    @PostMapping
    public Interview create(@Valid @RequestBody InterviewRequest request) {
        return service.create(toEntity(request));
    }

    @GetMapping
    public List<Interview> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Interview getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Interview update(@PathVariable Long id,
                            @Valid @RequestBody InterviewUpdateRequest updateRequest) {
        return service.update(id, updateRequest);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/suggest-slots")
    public List<java.time.LocalDateTime> suggestSlots(@RequestParam Long applicationId) {
        return service.suggestOptimalSlots(applicationId);
    }

    private Interview toEntity(InterviewRequest request) {
        Interview interview = new Interview();
        interview.setInterviewDate(request.getInterviewDate());
        interview.setType(request.getType());
        interview.setLocation(request.getLocation());
        
        // Handle result field - convert string to enum if not null/blank
        if (request.getResult() != null && !request.getResult().trim().isEmpty()) {
            try {
                interview.setResult(InterviewResult.valueOf(request.getResult().trim()));
            } catch (IllegalArgumentException e) {
                // If invalid enum value, leave as null
                interview.setResult(null);
            }
        } else {
            interview.setResult(null);
        }
        
        interview.setResultNotes(request.getResultNotes());

        Application application = new Application();
        application.setId(request.getApplicationId());
        interview.setApplication(application);

        return interview;
    }
}
package net.thesphynx.espritmarket.EventPlanning.Service;

import net.thesphynx.espritmarket.EventPlanning.Entity.Collaboration;
import net.thesphynx.espritmarket.EventPlanning.Repository.ICollaborationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CollaborationService {
    private final ICollaborationRepository collaborationRepository;

    public CollaborationService(ICollaborationRepository collaborationRepository) {
        this.collaborationRepository = collaborationRepository;
    }

    public List<Collaboration> getAll() {
        return collaborationRepository.findAll();
    }

    public Optional<Collaboration> getById(Long id) {
        return collaborationRepository.findById(id);
    }

    public Collaboration create(Collaboration collaboration) {
        return collaborationRepository.save(collaboration);
    }

    public Collaboration update(Long id, Collaboration collaboration) {
        collaboration.setId(id);
        return collaborationRepository.save(collaboration);
    }

    public void delete(Long id) {
        collaborationRepository.deleteById(id);
    }
}

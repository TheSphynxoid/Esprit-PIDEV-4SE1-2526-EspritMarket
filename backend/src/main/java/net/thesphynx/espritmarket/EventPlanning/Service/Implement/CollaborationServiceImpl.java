package net.thesphynx.espritmarket.EventPlanning.Service.Implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Entity.Collaboration;
import net.thesphynx.espritmarket.EventPlanning.Repository.ICollaborationRepository;
import net.thesphynx.espritmarket.EventPlanning.Service.Interface.ICollaborationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationServiceImpl implements ICollaborationService {

    private final ICollaborationRepository collaborationRepository;

    @Override
    public Collaboration addCollaboration(Collaboration collaboration) {
        log.info("Adding Collaboration: {}", collaboration);
        return collaborationRepository.save(collaboration);
    }

    @Override
    public Collaboration updateCollaboration(Collaboration collaboration) {
        log.info("Updating Collaboration: {}", collaboration);
        return collaborationRepository.save(collaboration);
    }

    @Override
    public void deleteCollaboration(Long id) {
        log.info("Deleting Collaboration with id: {}", id);
        collaborationRepository.deleteById(id);
    }

    @Override
    public Optional<Collaboration> getCollaborationById(Long id) {
        log.info("Getting Collaboration with id: {}", id);
        return collaborationRepository.findById(id);
    }

    @Override
    public List<Collaboration> getAllCollaborations() {
        log.info("Getting all Collaborations");
        return collaborationRepository.findAll();
    }
}
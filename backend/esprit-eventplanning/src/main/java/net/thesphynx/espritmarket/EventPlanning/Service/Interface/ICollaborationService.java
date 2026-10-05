package net.thesphynx.espritmarket.EventPlanning.Service.Interface;

import net.thesphynx.espritmarket.EventPlanning.Entity.Collaboration;
import java.util.List;
import java.util.Optional;

public interface ICollaborationService {
    Collaboration addCollaboration(Collaboration collaboration);
    Collaboration updateCollaboration(Collaboration collaboration);
    void deleteCollaboration(Long id);
    Optional<Collaboration> getCollaborationById(Long id);
    List<Collaboration> getAllCollaborations();
}
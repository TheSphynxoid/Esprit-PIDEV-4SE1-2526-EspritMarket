package net.thesphynx.espritmarket.EventPlanning.Service;

import net.thesphynx.espritmarket.EventPlanning.Entity.Stall;
import net.thesphynx.espritmarket.EventPlanning.Repository.IStallRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StallService {
    private final IStallRepository stallRepository;

    public StallService(IStallRepository stallRepository) {
        this.stallRepository = stallRepository;
    }

    public List<Stall> getAll() {
        return stallRepository.findAll();
    }

    public Optional<Stall> getById(Long id) {
        return stallRepository.findById(id);
    }

    public Stall create(Stall stall) {
        return stallRepository.save(stall);
    }

    public Stall update(Long id, Stall stall) {
        stall.setId(id);
        return stallRepository.save(stall);
    }

    public void delete(Long id) {
        stallRepository.deleteById(id);
    }
}

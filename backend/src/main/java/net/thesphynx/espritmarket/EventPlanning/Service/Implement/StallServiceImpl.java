package net.thesphynx.espritmarket.EventPlanning.Service.Implement;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Entity.Stall;
import net.thesphynx.espritmarket.EventPlanning.Repository.IStallRepository;
import net.thesphynx.espritmarket.EventPlanning.Service.Interface.IStallService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StallServiceImpl implements IStallService {

    private final IStallRepository stallRepository;

    @Override
    public Stall addStall(Stall stall) {
        log.info("Adding Stall: {}", stall);
        return stallRepository.save(stall);
    }

    @Override
    public Stall updateStall(Stall stall) {
        log.info("Updating Stall: {}", stall);
        return stallRepository.save(stall);
    }

    @Override
    public void deleteStall(Long id) {
        log.info("Deleting Stall with id: {}", id);
        stallRepository.deleteById(id);
    }

    @Override
    public Optional<Stall> getStallById(Long id) {
        log.info("Getting Stall with id: {}", id);
        return stallRepository.findById(id);
    }

    @Override
    public List<Stall> getAllStalls() {
        log.info("Getting all Stalls");
        return stallRepository.findAll();
    }
}
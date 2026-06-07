package net.thesphynx.espritmarket.EventPlanning.Service.Interface;

import net.thesphynx.espritmarket.EventPlanning.Entity.Stall;
import java.util.List;
import java.util.Optional;

public interface IStallService {
    Stall addStall(Stall stall);
    Stall updateStall(Stall stall);
    void deleteStall(Long id);
    Optional<Stall> getStallById(Long id);
    List<Stall> getAllStalls();
}
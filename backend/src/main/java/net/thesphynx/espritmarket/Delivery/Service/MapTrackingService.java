package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Delivery.Entity.MapTracking;
import net.thesphynx.espritmarket.Delivery.Repository.IMapTrackingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MapTrackingService {
    private final IMapTrackingRepository mapTrackingRepository;

    public MapTrackingService(IMapTrackingRepository mapTrackingRepository) {
        this.mapTrackingRepository = mapTrackingRepository;
    }

    public List<MapTracking> getAll() {
        return mapTrackingRepository.findAll();
    }

    public Optional<MapTracking> getById(Long id) {
        return mapTrackingRepository.findById(id);
    }

    public MapTracking create(MapTracking mapTracking) {
        return mapTrackingRepository.save(mapTracking);
    }

    public MapTracking update(Long id, MapTracking mapTracking) {
        mapTracking.setId(id);
        return mapTrackingRepository.save(mapTracking);
    }

    public void delete(Long id) {
        mapTrackingRepository.deleteById(id);
    }
}

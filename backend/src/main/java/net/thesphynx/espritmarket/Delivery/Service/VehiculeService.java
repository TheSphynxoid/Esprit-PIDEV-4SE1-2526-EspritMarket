package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Delivery.Entity.Vehicule;
import net.thesphynx.espritmarket.Delivery.Repository.IVehiculeRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class VehiculeService {
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final IVehiculeRepository vehiculeRepository;
    private final UserRepository userRepository; // ✅ ajouter

    public VehiculeService(IVehiculeRepository vehiculeRepository,
                           UserRepository userRepository) { // ✅ injecter
        this.vehiculeRepository = vehiculeRepository;
        this.userRepository = userRepository;
    }

    public List<Vehicule> getVoituresByUserEmail(String email) {
        return vehiculeRepository.findByUserEmail(email);
    }

    public List<Vehicule> getAllVehicules() {
        return vehiculeRepository.findAll();
    }

    public Optional<Vehicule> getById(Long id) {
        return vehiculeRepository.findById(id);
    }

    public Optional<Vehicule> getByIdForUser(Long id, String email) {
        return vehiculeRepository.findByIdAndUserEmail(id, email);
    }

    public Vehicule create(Vehicule vehicule, String userEmail) {
        String normalizedSerie = normalizeSerie(vehicule.getRegistrationnumbers());
        if (vehiculeRepository.existsByRegistrationnumbers(normalizedSerie)) {
            throw new IllegalStateException("Un vehicule avec cette serie existe deja");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        vehicule.setRegistrationnumbers(normalizedSerie);
        vehicule.setUser(user);

        return vehiculeRepository.save(vehicule);
    }

    public Optional<Vehicule> updateForUser(Long id, Vehicule updates, String userEmail) {
        Optional<Vehicule> existingOpt = vehiculeRepository.findByIdAndUserEmail(id, userEmail);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        Vehicule existing = existingOpt.get();
        String normalizedSerie = normalizeSerie(updates.getRegistrationnumbers());
        if (vehiculeRepository.existsByRegistrationnumbersAndIdNot(normalizedSerie, existing.getId())) {
            throw new IllegalStateException("Un vehicule avec cette serie existe deja");
        }

        existing.setType(updates.getType());
        existing.setRegistrationnumbers(normalizedSerie);
        existing.setCapacity(updates.getCapacity());
        existing.setStatus(updates.getStatus());

        return Optional.of(vehiculeRepository.save(existing));
    }

    public Optional<Vehicule> uploadVehiclePhotoForUser(Long id, String userEmail, MultipartFile file) throws IOException {
        return uploadImageForUser(id, userEmail, file, ImageType.VEHICLE_PHOTO);
    }

    public Optional<Vehicule> uploadRegistrationCardFrontForUser(Long id, String userEmail, MultipartFile file) throws IOException {
        return uploadImageForUser(id, userEmail, file, ImageType.REGISTRATION_FRONT);
    }

    public Optional<Vehicule> uploadRegistrationCardBackForUser(Long id, String userEmail, MultipartFile file) throws IOException {
        return uploadImageForUser(id, userEmail, file, ImageType.REGISTRATION_BACK);
    }

    private Optional<Vehicule> uploadImageForUser(Long id,
                                                  String userEmail,
                                                  MultipartFile file,
                                                  ImageType imageType) throws IOException {
        Optional<Vehicule> existingOpt = vehiculeRepository.findByIdAndUserEmail(id, userEmail);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        validateImage(file, imageType.label);

        Vehicule vehicule = existingOpt.get();
        byte[] content = file.getBytes();
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        switch (imageType) {
            case VEHICLE_PHOTO -> {
                vehicule.setVehiclePhoto(content);
                vehicule.setVehiclePhotoFileName(fileName);
                vehicule.setVehiclePhotoContentType(contentType);
            }
            case REGISTRATION_FRONT -> {
                vehicule.setRegistrationCardFront(content);
                vehicule.setRegistrationCardFrontFileName(fileName);
                vehicule.setRegistrationCardFrontContentType(contentType);
            }
            case REGISTRATION_BACK -> {
                vehicule.setRegistrationCardBack(content);
                vehicule.setRegistrationCardBackFileName(fileName);
                vehicule.setRegistrationCardBackContentType(contentType);
            }
        }

        return Optional.of(vehiculeRepository.save(vehicule));
    }

    private void validateImage(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(label + " obligatoire");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException(label + " trop volumineuse (max 5 MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException(label + " doit etre une image");
        }
    }

    private String normalizeSerie(String registrationnumbers) {
        if (registrationnumbers == null) {
            return null;
        }
        return registrationnumbers.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    public void delete(Long id) {
        vehiculeRepository.deleteById(id);
    }

    public boolean deleteForUser(Long id, String userEmail) {
        Optional<Vehicule> existingOpt = vehiculeRepository.findByIdAndUserEmail(id, userEmail);
        if (existingOpt.isEmpty()) {
            return false;
        }

        vehiculeRepository.delete(existingOpt.get());
        return true;
    }

    private enum ImageType {
        VEHICLE_PHOTO("Photo du vehicule"),
        REGISTRATION_FRONT("Carte grise recto"),
        REGISTRATION_BACK("Carte grise verso");

        private final String label;

        ImageType(String label) {
            this.label = label;
        }
    }
}

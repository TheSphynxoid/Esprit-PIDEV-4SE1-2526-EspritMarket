package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Delivery.Entity.Delivery;
import net.thesphynx.espritmarket.Delivery.Entity.DeliveryAddressDetails;
import net.thesphynx.espritmarket.Delivery.Repository.IDeliveryAddressDetailsRepository;
import net.thesphynx.espritmarket.Delivery.Repository.IDeliveryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DeliveryAddressDetailsService {

    private final IDeliveryAddressDetailsRepository addressDetailsRepository;
    private final IDeliveryRepository deliveryRepository;

    public DeliveryAddressDetailsService(IDeliveryAddressDetailsRepository addressDetailsRepository,
                                         IDeliveryRepository deliveryRepository) {
        this.addressDetailsRepository = addressDetailsRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public List<DeliveryAddressDetails> getAll() {
        return addressDetailsRepository.findAll();
    }

    public List<DeliveryAddressDetails> getAllForUser(Long connectedUserId) {
        return addressDetailsRepository.findByConnectedUserIdOrderByIdDesc(connectedUserId);
    }

    public Optional<DeliveryAddressDetails> getById(Long id) {
        return addressDetailsRepository.findById(id);
    }

    public Optional<DeliveryAddressDetails> getByIdForUser(Long id, Long connectedUserId) {
        return addressDetailsRepository.findByIdAndConnectedUserId(id, connectedUserId);
    }

    public DeliveryAddressDetails createForUser(DeliveryAddressDetails details, Long connectedUserId) {
        details.setConnectedUserId(connectedUserId);
        return addressDetailsRepository.save(details);
    }

    public Optional<DeliveryAddressDetails> updateForUser(Long id, DeliveryAddressDetails updates, Long connectedUserId) {
        Optional<DeliveryAddressDetails> existingOpt = addressDetailsRepository.findByIdAndConnectedUserId(id, connectedUserId);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        DeliveryAddressDetails existing = existingOpt.get();
        existing.setDeliveryAddress(updates.getDeliveryAddress());
        existing.setCity(updates.getCity());
        existing.setPostalCode(updates.getPostalCode());
        existing.setPhoneNumber(updates.getPhoneNumber());

        return Optional.of(addressDetailsRepository.save(existing));
    }

    public boolean deleteForUser(Long id, Long connectedUserId) {
        Optional<DeliveryAddressDetails> existingOpt = addressDetailsRepository.findByIdAndConnectedUserId(id, connectedUserId);
        if (existingOpt.isEmpty()) {
            return false;
        }

        addressDetailsRepository.delete(existingOpt.get());
        return true;
    }

    public DeliveryAddressDetails create(DeliveryAddressDetails details, Long deliveryId) {
        DeliveryAddressDetails savedDetails = addressDetailsRepository.save(details);
        attachDeliveryIfPresent(savedDetails, deliveryId);
        return savedDetails;
    }

    public DeliveryAddressDetails update(Long id, DeliveryAddressDetails details, Long deliveryId) {
        details.setId(id);
        DeliveryAddressDetails savedDetails = addressDetailsRepository.save(details);
        attachDeliveryIfPresent(savedDetails, deliveryId);
        return savedDetails;
    }

    public void delete(Long id) {
        addressDetailsRepository.deleteById(id);
    }

    private void attachDeliveryIfPresent(DeliveryAddressDetails details, Long deliveryId) {
        if (deliveryId == null) {
            return;
        }

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));

        delivery.setDeliveryAddressDetails(details);
        delivery.setDeliveryAddress(details.getDeliveryAddress());
        delivery.setCity(details.getCity());
        delivery.setPostalCode(details.getPostalCode());
        delivery.setPhoneNumber(details.getPhoneNumber());
        details.setConnectedUserId(delivery.getConnectedUserId());
        deliveryRepository.save(delivery);
    }
}

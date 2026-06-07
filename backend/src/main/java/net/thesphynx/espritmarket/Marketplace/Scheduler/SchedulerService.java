package net.thesphynx.espritmarket.Marketplace.Scheduler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.thesphynx.espritmarket.Marketplace.Entity.Product;
import net.thesphynx.espritmarket.Marketplace.Entity.Store;
import net.thesphynx.espritmarket.Marketplace.Repository.IProductRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.IStoreRepository;

@Service
public class SchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private static final String PRODUCT_STATUS_ACTIVE = "ACTIVE";
    private static final String PRODUCT_STATUS_INACTIVE = "INACTIVE";

    private final IStoreRepository storeRepository;
    private final IProductRepository productRepository;

    @Value("${app.marketplace.scheduler.store-inactive-days:30}")
    private long storeInactiveDays;

    public SchedulerService(IStoreRepository storeRepository,
                            IProductRepository productRepository) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public NightlyMaintenanceResult runNightlyMaintenance() {
        LocalDateTime now = LocalDateTime.now();

        int productStatusesUpdated = updateProductStatuses(now);
        int storesDeactivated = deactivateStoresWithoutRecentActiveProducts(now.minusDays(storeInactiveDays));

        return new NightlyMaintenanceResult(productStatusesUpdated, storesDeactivated);
    }

    private int updateProductStatuses(LocalDateTime now) {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return 0;
        }

        int statusChanges = 0;
        List<Product> dirtyProducts = new ArrayList<>();

        for (Product product : products) {
            boolean dirty = false;

            String currentStatus = normalizeProductStatus(product.getStatus());
            String expectedStatus = isProductActiveByBusinessRule(product) ? PRODUCT_STATUS_ACTIVE : PRODUCT_STATUS_INACTIVE;
            if (!expectedStatus.equals(currentStatus)) {
                product.setStatus(expectedStatus);
                statusChanges++;
                dirty = true;
            }

            if (product.getUpdatedAt() == null) {
                product.setUpdatedAt(now);
                dirty = true;
            }

            if (dirty) {
                dirtyProducts.add(product);
            }
        }

        if (!dirtyProducts.isEmpty()) {
            productRepository.saveAll(dirtyProducts);
        }

        return statusChanges;
    }

    private int deactivateStoresWithoutRecentActiveProducts(LocalDateTime cutoff) {
        List<Store> storesToDeactivate = storeRepository.findActiveStoresWithoutRecentActiveProducts(cutoff);
        if (storesToDeactivate.isEmpty()) {
            return 0;
        }

        storesToDeactivate.forEach(store -> store.setActive(false));
        storeRepository.saveAll(storesToDeactivate);
        return storesToDeactivate.size();
    }

    private boolean isProductActiveByBusinessRule(Product product) {
        Integer stock = product.getStock();
        return stock != null && stock > 0;
    }

    private String normalizeProductStatus(String status) {
        if (status == null || status.isBlank()) {
            return PRODUCT_STATUS_ACTIVE;
        }
        return status.trim().toUpperCase();
    }

    public record NightlyMaintenanceResult(
            int productStatusesUpdated,
            int storesDeactivated
    ) {
    }

    public void logNightlySummary(NightlyMaintenanceResult result) {
        logger.info("marketplace.scheduler.summary productsUpdated={}, storesDeactivated={}",
                result.productStatusesUpdated(),
            result.storesDeactivated());
    }
}

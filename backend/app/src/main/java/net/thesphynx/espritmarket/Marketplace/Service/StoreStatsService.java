package net.thesphynx.espritmarket.Marketplace.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.thesphynx.espritmarket.Common.Entity.Role;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Exception.ForbiddenException;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Marketplace.Dto.StoreStatsResponse;
import net.thesphynx.espritmarket.Marketplace.Entity.Store;
import net.thesphynx.espritmarket.Marketplace.Repository.IStoreRepository;
import net.thesphynx.espritmarket.Marketplace.Repository.StoreStatsRepository;

@Service
public class StoreStatsService {

    private final StoreStatsRepository storeStatsRepository;
    private final IStoreRepository storeRepository;
    private final UserRepository userRepository;

    public StoreStatsService(
            StoreStatsRepository storeStatsRepository,
            IStoreRepository storeRepository,
            UserRepository userRepository) {
        this.storeStatsRepository = storeStatsRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public StoreStatsResponse getStoreStats(Long storeId, String userEmail) {

        // 1. Vérifier que le store existe
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        // 2. Vérifier que l'utilisateur existe
        User user = userRepository.findByEmail(userEmail)
                .or(() -> userRepository.findByName(userEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 3. Un SELLER ne peut voir que son propre store
        boolean isSeller = user.getRole() == Role.SELLER;
        boolean isOwner  = store.getOwner() != null && store.getOwner().getId().equals(user.getId());

        if (isSeller && !isOwner) {
            throw new ForbiddenException("You cannot access stats for this store");
        }

        // 4. Récupérer le chiffre d'affaires total
        Double totalRevenue = storeStatsRepository.findTotalRevenueByStoreId(storeId);
        if (totalRevenue == null) totalRevenue = 0.0;

        // 5. Récupérer le nombre total de commandes
        Long totalOrders = storeStatsRepository.countDistinctOrdersByStoreId(storeId);
        if (totalOrders == null) totalOrders = 0L;

        // 6. Récupérer le produit best-seller
        StoreStatsResponse.BestSeller bestSeller = getBestSeller(storeId);

        // 7. Récupérer le revenu par mois
        List<StoreStatsResponse.MonthlyRevenue> monthlyRevenue = getMonthlyRevenue(storeId);

        // 8. Retourner tout dans le DTO
        return new StoreStatsResponse(totalRevenue, totalOrders, bestSeller, monthlyRevenue);
    }

    // ── Best seller ────────────────────────────────────────────────────────────
    private StoreStatsResponse.BestSeller getBestSeller(Long storeId) {

        // On prend uniquement le 1er résultat (le plus vendu)
        List<Object[]> rows = storeStatsRepository.findBestSellerByStoreId(
                storeId, PageRequest.of(0, 1));

        // Pas de commandes = pas de best seller
        if (rows.isEmpty()) {
            return null;
        }

        Object[] row = rows.get(0);

        // La requête retourne : [p.id, p.name, SUM(quantity), SUM(subtotal)]
        Long   productId    = toLong(row[0]);
        String productName  = row[1] != null ? row[1].toString() : "";
        Long   totalQty     = toLong(row[2]);
        Double revenue      = toDouble(row[3]);

        return new StoreStatsResponse.BestSeller(productId, productName, totalQty, revenue);
    }

    // ── Revenu mensuel ─────────────────────────────────────────────────────────
    private List<StoreStatsResponse.MonthlyRevenue> getMonthlyRevenue(Long storeId) {

        List<StoreStatsResponse.MonthlyRevenue> result = new ArrayList<>();

        // La requête retourne : [YEAR, MONTH, SUM(subtotal)]
        for (Object[] row : storeStatsRepository.findMonthlyRevenueByStoreId(storeId)) {

            int    year    = toInt(row[0]);
            int    month   = toInt(row[1]);
            Double revenue = toDouble(row[2]);

            // Formater en "2024-03" pour le frontend
            String monthLabel = year + "-" + (month < 10 ? "0" + month : "" + month);

            result.add(new StoreStatsResponse.MonthlyRevenue(monthLabel, revenue));
        }

        return result;
    }

    // ── Helpers de conversion simples ──────────────────────────────────────────

    private Long toLong(Object value) {
        if (value == null) return 0L;
        return ((Number) value).longValue();
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        return ((Number) value).doubleValue();
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        return ((Number) value).intValue();
    }
}
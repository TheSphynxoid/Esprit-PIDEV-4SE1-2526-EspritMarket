package net.thesphynx.espritmarket.Marketplace.Controller;

import net.thesphynx.espritmarket.Marketplace.Dto.ProductResponse;
import net.thesphynx.espritmarket.Marketplace.Service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des produits affichés dans la boutique publique (Market).
 */
@RestController
@RequestMapping("/api/market")
public class MarketProductController {

    private final ProductService productService;

    public MarketProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Récupère la liste des produits disponibles pour la vente dans la place de marché.
     *
     * @return Liste de ProductResponse contenant les détails des produits du marché.
     */
    @GetMapping("/products")
    public List<ProductResponse> getMarketProducts() {
        return productService.getMarketProducts();
    }
}

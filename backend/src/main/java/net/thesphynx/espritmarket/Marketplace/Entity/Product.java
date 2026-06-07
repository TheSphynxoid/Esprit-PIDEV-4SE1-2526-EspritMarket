package net.thesphynx.espritmarket.Marketplace.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Check;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.thesphynx.espritmarket.Common.Entity.User;

@Entity
@Table(name = "product")
@Check(constraints = "discount_percent IS NULL OR (discount_percent > 0 AND discount_percent <= 100)")
@Check(constraints = "(promo_start_at IS NULL AND promo_end_at IS NULL) OR (promo_start_at IS NOT NULL AND promo_end_at IS NOT NULL AND promo_end_at > promo_start_at)")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    @Column(name = "status", length = 30)
    private String status = "ACTIVE";
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    private Double price;
    private Double originalPrice;
    private Double discountPercent;
    private LocalDateTime promoStartAt;
    private LocalDateTime promoEndAt;

    @Column(name = "stock", nullable = true)
    private Integer stock;

    public Integer getStock() {
        return this.stock;
    }

    public void setStock(Integer stock) {
        System.out.println("💎 ENTITY SETTING STOCK TO: " + stock);
        this.stock = stock;
    }

    @Column(name = "dimensions_label")
    private String dimensionsLabel;

    @Column(name = "weight")
    private Double weight;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnoreProperties({ "products", "categories" })
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties("products")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("product")
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("product")
    private List<OrderLine> orderLines = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "product_users", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonIgnoreProperties({ "password" })
    private List<User> users = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
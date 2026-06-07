package net.thesphynx.espritmarket.Marketplace.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import net.thesphynx.espritmarket.Marketplace.Entity.Review;

@Repository
public interface IReviewRepository extends JpaRepository<Review, Long> {
}

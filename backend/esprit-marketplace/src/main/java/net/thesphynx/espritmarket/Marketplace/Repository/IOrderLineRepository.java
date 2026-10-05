package net.thesphynx.espritmarket.Marketplace.Repository;

import net.thesphynx.espritmarket.Marketplace.Entity.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IOrderLineRepository extends JpaRepository<OrderLine, Long> {
}

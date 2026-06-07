package net.thesphynx.espritmarket.Marketplace.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import net.thesphynx.espritmarket.Marketplace.Entity.Order;

@Repository
public interface IOrderRepository extends JpaRepository<Order, Long> {
}
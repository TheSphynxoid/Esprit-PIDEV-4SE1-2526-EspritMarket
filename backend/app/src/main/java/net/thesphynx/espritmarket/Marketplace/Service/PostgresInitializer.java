package net.thesphynx.espritmarket.Marketplace.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PostgresInitializer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            System.out.println("🐘 Initialisation de PostgreSQL - Activation de pg_trgm...");
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm;");
            System.out.println("✅ Extension pg_trgm activée avec succès.");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'activation de pg_trgm: " + e.getMessage());
            System.err.println("⚠️ La recherche floue (fuzzy search) pourrait ne pas fonctionner.");
        }
    }
}

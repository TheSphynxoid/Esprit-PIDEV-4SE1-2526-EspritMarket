package net.thesphynx.espritmarket.Marketplace.Config;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import net.thesphynx.espritmarket.Marketplace.Entity.Category;
import net.thesphynx.espritmarket.Marketplace.Repository.ICategoryRepository;

/**
 * Initializes default categories on application startup.
 * Creates default categories (clothes, meuble, clectronic, others) if they don't already exist.
 */
@Component
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final ICategoryRepository categoryRepository;

    // Default categories to initialize
    private static final List<String> DEFAULT_CATEGORY_NAMES = Arrays.asList(
        "clothes",
        "furniture",
        "electronic",
        "others"
    );

    private static final List<String> DEFAULT_CATEGORY_DESCRIPTIONS = Arrays.asList(
        "clothes et accessoires",
        "furniture et décoration",
        "Électronique et technologie",
        "Autres catégories"
    );

    public DataInitializer(ICategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @PostConstruct
    public void initializeDefaultCategories() {
        try {
            for (int i = 0; i < DEFAULT_CATEGORY_NAMES.size(); i++) {
                String categoryName = DEFAULT_CATEGORY_NAMES.get(i);
                String categoryDescription = DEFAULT_CATEGORY_DESCRIPTIONS.get(i);

                // Check if category already exists
                boolean categoryExists = categoryRepository.findAll()
                    .stream()
                    .anyMatch(cat -> cat.getName() != null && 
                                   cat.getName().equalsIgnoreCase(categoryName));

                // Create category if it doesn't exist
                if (!categoryExists) {
                    Category category = new Category();
                    category.setName(categoryName);
                    category.setDescription(categoryDescription);
                    // store_id is left null for global/system categories
                    categoryRepository.save(category);
                    log.info("Created default category: {}", categoryName);
                }
            }
        } catch (Exception e) {
            log.error("Error initializing default categories: {}", e.getMessage());
        }
    }
}

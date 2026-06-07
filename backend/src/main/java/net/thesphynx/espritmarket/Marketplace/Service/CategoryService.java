package net.thesphynx.espritmarket.Marketplace.Service;

import net.thesphynx.espritmarket.Marketplace.Dto.CategoryRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.CategoryResponse;
import net.thesphynx.espritmarket.Marketplace.Mapper.CategoryMapper;
import net.thesphynx.espritmarket.Marketplace.Repository.ICategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final ICategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(ICategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<CategoryResponse> getById(Long id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::toResponse);
    }

    public CategoryResponse create(CategoryRequest request) {
        return categoryMapper.toResponse(
                categoryRepository.save(categoryMapper.toEntity(request))
        );
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        var entity = categoryMapper.toEntity(request);
        entity.setId(id);
        return categoryMapper.toResponse(categoryRepository.save(entity));
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}

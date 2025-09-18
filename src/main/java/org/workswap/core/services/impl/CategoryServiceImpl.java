package org.workswap.core.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.workswap.common.dto.CategoryDTO;
import org.workswap.common.enums.SearchModelParamType;
import org.workswap.datasource.central.model.listingModels.Category;
import org.workswap.datasource.central.repository.CategoryRepository;
import org.workswap.core.services.CategoryService;
import org.workswap.core.services.LocalizationService;
import org.workswap.core.services.components.ServiceUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile("production")
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ServiceUtils serviceUtils;
    private final LocalizationService localizationService;

    private Category findCategoryFromRepostirory(String param, SearchModelParamType paramType) {
        switch (paramType) {
            case ID:
                return categoryRepository.findById(Long.parseLong(param)).orElse(null);
            case NAME:
                return categoryRepository.findByName(param);
            default:
                throw new IllegalArgumentException("Unknown param type: " + paramType);
        }
    }

    @Override
    public Category findCategory(String param) {
        if (param == null) {
            return null;
        }
        SearchModelParamType paramType = serviceUtils.detectParamType(param);
        return findCategoryFromRepostirory(param, paramType);
    }

    @Override
    public void save(Category category) {
        categoryRepository.save(category);
    }
    
    @Override
    public Category saveAndReturn(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category createCategory(CategoryDTO dto, List<String> translations) throws IOException {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists");
        }

        Category parent = null;
        if (dto.getParentId() != null) {
                        
            if (findCategory(dto.getParentId().toString()).isLeaf()) {
                throw new IllegalStateException("Cannot add subcategory to a leaf category");
            }
        }

        Category category = new Category(dto.getName(), parent);
        category.setLeaf(dto.isLeaf());

        if (translations != null) {
            for (String translation : translations) {
                String[] parts = translation.split("\\.");

                if (parts.length == 2) {
                    String text = parts[0];   // "перевод"
                    String lang = parts[1];   // "ru"
                    localizationService.createTranslation("localization/categories/categories", "category." + category.getName(), lang, text);
                } else {
                    // Обработка ошибки: неверный формат
                    throw new IllegalArgumentException("Неверный формат строки. Ожидалось: текст.язык");
                }
            }
        }

        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategoryTree() {
        List<Category> roots = categoryRepository.findByParentIsNull();
        roots.forEach(this::loadChildrenRecursively);
        return roots;
    }

    private void loadChildrenRecursively(Category parent) {
        List<Category> children = categoryRepository.findByParentId(parent.getId());
        parent.setChildren(children);
        children.forEach(this::loadChildrenRecursively);
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return findCategory(id.toString());
    }

    @Override
    @Transactional
    public void deleteCategory(Long id)  {
        Category category = getCategoryById(id);
        
        if (!category.getChildren().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with subcategories");
        }
        
        if (!category.getListings().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with associated listings");
        }

        try {
            localizationService.removeTranslation("localization", "category." + category.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }

        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getLeafCategories() {
        return categoryRepository.findByLeaf(true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getChildCategories(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    @Override
    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    @Override
    public boolean isLeafCategory(Long categoryId) {
        if (categoryId == null) {
            return false;
        }
        
        return categoryRepository.findById(categoryId)
                .map(Category::isLeaf)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Категория с ID " + categoryId + " не найдена"
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategoryPath(Long categoryId) {

        if (categoryId == null) {
            return Collections.emptyList();
        }

        return categoryRepository.findCategoryPathWithNativeQuery(categoryId); 
    }

    @Override
    public CategoryDTO toDTO(Category category, Locale locale) {
        Long parentId = category.getParent() != null ? category.getParent().getId() : null;
        return new CategoryDTO(
            category.getId(), 
            category.getName(), 
            parentId,
            category.isLeaf());
    }

    private void collectDescendants(Category parent, Map<Long, List<Category>> parentMap, List<Category> result) {
        result.add(parent);
        List<Category> children = parentMap.getOrDefault(parent.getId(), Collections.emptyList());
        for (Category child : children) {
            collectDescendants(child, parentMap, result);
        }
    }

    @Override
    public List<Category> getAllDescendants(Category parent) {
        List<Category> allCategories = categoryRepository.findAll();

        // строим карту parentId -> список детей
        Map<Long, List<Category>> parentMap = allCategories.stream()
            .filter(c -> c.getParent() != null)
            .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        List<Category> result = new ArrayList<>();
        collectDescendants(parent, parentMap, result);

        return result;
    }

    public List<Category> getAllDescendantsById(Long parentId) {
        List<Category> allCategories = categoryRepository.findAll(); // 1 запрос
        Map<Long, List<Category>> parentMap = allCategories.stream()
            .filter(c -> c.getParent() != null)
            .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        List<Category> result = new ArrayList<>();
        Category parent = allCategories.stream()
            .filter(c -> c.getId().equals(parentId))
            .findFirst()
            .orElse(null);

        if (parent != null) {
            collectDescendants(parent, parentMap, result);
        }
        return result;
    }
}
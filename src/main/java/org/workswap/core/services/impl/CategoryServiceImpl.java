package org.workswap.core.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.workswap.datasource.central.model.DTOs.CategoryDTO;
import org.workswap.datasource.central.model.enums.SearchModelParamType;
import org.workswap.datasource.central.model.listingModels.Category;
import org.workswap.datasource.central.repository.CategoryRepository;
import org.workswap.core.services.CategoryService;
import org.workswap.core.services.LocalizationService;
import org.workswap.core.services.components.ServiceUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final MessageSource messageSource;
    private final ServiceUtils serviceUtils;
    private final LocalizationService localizationService;

    private Category findCategoryFromRepostirory(String param, SearchModelParamType paramType) {
        switch (paramType) {
            case ID:
                return categoryRepository.findById(Long.parseLong(param)).orElse(null);
            default:
                throw new IllegalArgumentException("Unknown param type: " + paramType);
        }
    }

    @Override
    public Category findCategory(String param) {
        SearchModelParamType paramType = serviceUtils.detectParamType(param);
        return findCategoryFromRepostirory(param, paramType);
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
        System.out.println("Перевод категории " + category.getName() + ": " + messageSource.getMessage("category." + category.getName(), null, locale));
        return new CategoryDTO(category.getId(), category.getName(), parentId, category.isLeaf(), messageSource.getMessage("category." + category.getName(), null, locale));
    }

    @Override
    public List<Category> getAllDescendants(Category parent) {
        List<Category> descendants = new ArrayList<>();
        descendants.add(parent);
        List<Category> children = categoryRepository.findByParent(parent);
        for (Category child : children) {
            descendants.add(child);
            descendants.addAll(getAllDescendants(child));
            System.out.println("Дочерняя категория найдена: " + child.getName());
        }
        return descendants;
    }
}
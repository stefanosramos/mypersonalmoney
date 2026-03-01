package br.com.mypersonalmoney.category;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class CategoryService {

    @Transactional
    public Category createCategory(String name, CategoryType type) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (type == null) throw new IllegalArgumentException("type is required");

        Category c = new Category();
        c.name = name.trim();
        c.type = type;
        c.active = true;
        c.persist();
        return c;
    }

    @Transactional
    public Category updateCategory(Long id, String name, Boolean active) {
        Category c = Category.findById(id);
        if (c == null) throw new IllegalArgumentException("category not found");

        if (name != null) {
            String n = name.trim();
            if (n.isBlank()) throw new IllegalArgumentException("name cannot be blank");
            c.name = n;
        }
        if (active != null) c.active = active;

        c.persist();
        return c;
    }

    public List<Category> listCategories(CategoryType type, Boolean active) {
        if (type == null && active == null) return Category.listAll();
        if (type != null && active == null) return Category.list("type = ?1 order by name", type);
        if (type == null) return Category.list("active = ?1 order by name", active);
        return Category.list("type = ?1 and active = ?2 order by name", type, active);
    }

    public Category getCategory(Long id) {
        return Category.findById(id);
    }

    // -------- SubCategory --------

    @Transactional
    public SubCategory createSubCategory(Long categoryId, String name) {
        if (categoryId == null) throw new IllegalArgumentException("categoryId is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");

        Category parent = Category.findById(categoryId);
        if (parent == null) throw new IllegalArgumentException("category not found");
        if (!parent.active) throw new IllegalArgumentException("category inactive");

        SubCategory s = new SubCategory();
        s.category = parent;
        s.name = name.trim();
        s.active = true;
        s.persist();
        return s;
    }

    public record SubCategoryResult(Long id, Long categoryId, String name, boolean active) {}

    @Transactional
    public SubCategoryResult updateSubCategory(Long id, String name, Boolean active) {
        SubCategory s = SubCategory.findById(id);
        if (s == null) throw new IllegalArgumentException("subcategory not found");

        if (name != null) {
            String n = name.trim();
            if (n.isBlank()) throw new IllegalArgumentException("name cannot be blank");
            s.name = n;
        }
        if (active != null) s.active = active;

        s.persist();

        Long categoryId = s.category.id; // dentro da transação é seguro
        return new SubCategoryResult(s.id, categoryId, s.name, s.active);
    }

    public List<SubCategory> listSubCategories(Long categoryId, Boolean active) {
        if (categoryId == null && active == null) return SubCategory.listAll();
        if (categoryId != null && active == null) return SubCategory.list("category.id = ?1 order by name", categoryId);
        if (categoryId == null) return SubCategory.list("active = ?1 order by name", active);
        return SubCategory.list("category.id = ?1 and active = ?2 order by name", categoryId, active);
    }

    public SubCategory getSubCategory(Long id) {
        return SubCategory.findById(id);
    }
}
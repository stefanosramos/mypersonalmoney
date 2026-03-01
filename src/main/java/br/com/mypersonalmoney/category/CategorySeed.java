package br.com.mypersonalmoney.category;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class CategorySeed {

    private static final Logger LOG = Logger.getLogger(CategorySeed.class);

    void onStart(@Observes StartupEvent event) {
        LOG.info("CategorySeed starting...");
        seed();
        LOG.info("CategorySeed finished.");
    }

    @Transactional
    void seed() {
        if (Category.count() > 0) {
            LOG.infof("Skipping seed: already have %d categories", Category.count());
            return;
        }

        // INCOME
        createCategory("Salário", CategoryType.INCOME);
        createCategory("Freelance", CategoryType.INCOME);
        createCategory("Investimentos", CategoryType.INCOME);
        createCategory("Reembolso", CategoryType.INCOME);

        // EXPENSE
        Category alimentacao = createCategory("Alimentação", CategoryType.EXPENSE);
        createSubCategories(alimentacao, List.of("Mercado", "Restaurantes", "Delivery"));

        Category moradia = createCategory("Moradia", CategoryType.EXPENSE);
        createSubCategories(moradia, List.of("Aluguel", "Condomínio", "Luz", "Água"));

        Category transporte = createCategory("Transporte", CategoryType.EXPENSE);
        createSubCategories(transporte, List.of("Combustível", "Uber", "Manutenção"));

        createCategory("Lazer", CategoryType.EXPENSE);
        createCategory("Saúde", CategoryType.EXPENSE);
        createCategory("Educação", CategoryType.EXPENSE);
        createCategory("Assinaturas", CategoryType.EXPENSE);

        LOG.info("Seed applied successfully.");
    }

    private Category createCategory(String name, CategoryType type) {
        Category c = new Category();
        c.name = name;
        c.type = type;
        c.active = true;
        c.persist();
        return c;
    }

    private void createSubCategories(Category parent, List<String> names) {
        for (String name : names) {
            SubCategory s = new SubCategory();
            s.category = parent;
            s.name = name;
            s.active = true;
            s.persist();
        }
    }
}
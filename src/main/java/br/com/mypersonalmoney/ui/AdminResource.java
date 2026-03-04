package br.com.mypersonalmoney.ui;

import br.com.mypersonalmoney.account.Account;
import br.com.mypersonalmoney.account.AccountType;
import br.com.mypersonalmoney.category.Category;
import br.com.mypersonalmoney.category.CategoryType;
import br.com.mypersonalmoney.category.SubCategory;
import br.com.mypersonalmoney.ledger.LedgerService;
import br.com.mypersonalmoney.util.MoneyParser;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.LocalDate;

@Path("/admin")
@Produces(MediaType.TEXT_HTML)
public class AdminResource {

    @Inject @Location("ui/admin.html")
    Template admin;

    @Inject @Location("ui/admin_accounts.html")
    Template adminAccounts;

    @Inject @Location("ui/admin_categories.html")
    Template adminCategories;

    @Inject
    LedgerService ledger;

    @GET
    public TemplateInstance page() {
        return admin
                .data("accounts", Account.<Account>list("order by active desc, name"))
                .data("accountTypes", AccountType.values())
                .data("categories", Category.<Category>list("order by active desc, type, name"))
                .data("categoryTypes", CategoryType.values())
                .data("subcategories", SubCategory.<SubCategory>list("order by active desc, name"))
                .data("today", java.time.LocalDate.now().toString());
    }


    @POST
    @Path("/accounts")
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance createAccount(@FormParam("name") String name,
                                          @FormParam("type") String type,
                                          @FormParam("openingBalance") String openingBalance,
                                          @FormParam("openingDate") String openingDate) {

        if (name == null || name.isBlank()) throw new BadRequestException("name is required");
        if (type == null || type.isBlank()) throw new BadRequestException("type is required");

        Long accountId = ledger.createAccount(name.trim(), AccountType.valueOf(type));

        if (openingBalance != null && !openingBalance.isBlank()) {
            BigDecimal bal = MoneyParser.parseBRL(openingBalance);
            if (bal.signum() != 0) {
                LocalDate dt = (openingDate == null || openingDate.isBlank())
                        ? LocalDate.now()
                        : LocalDate.parse(openingDate.trim());

                ledger.createOpeningBalance(accountId, bal, dt);
            }
        }

        return adminAccounts
                .data("accounts", Account.list("order by active desc, name"))
                .data("accountTypes", AccountType.values())
                .data("today", LocalDate.now().toString());
    }
    // -------- Accounts --------

    // -------- Categories --------

    @POST
    @Path("/categories")
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance createCategory(@FormParam("name") String name,
                                           @FormParam("type") String type) {

        if (name == null || name.isBlank()) throw new BadRequestException("name is required");
        if (type == null || type.isBlank()) throw new BadRequestException("type is required");

        Category c = new Category();
        c.name = name.trim();
        c.type = CategoryType.valueOf(type);
        c.active = true;
        c.persist();

        return renderCategoriesBlock();
    }

    @POST
    @Path("/subcategories")
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance createSubCategory(@FormParam("categoryId") Long categoryId,
                                              @FormParam("name") String name) {

        if (categoryId == null) throw new BadRequestException("categoryId is required");
        if (name == null || name.isBlank()) throw new BadRequestException("name is required");

        Category parent = Category.findById(categoryId);
        if (parent == null) throw new NotFoundException("category not found");
        if (!parent.active) throw new BadRequestException("category inactive");

        SubCategory s = new SubCategory();
        s.category = parent;
        s.name = name.trim();
        s.active = true;
        s.persist();

        return renderCategoriesBlock();
    }

    private TemplateInstance renderCategoriesBlock() {
        return adminCategories
                .data("categories", Category.<Category>list("order by active desc, type, name"))
                .data("categoryTypes", CategoryType.values())
                .data("subcategories", SubCategory.<SubCategory>list("order by active desc, name"));
    }
}
package br.com.mypersonalmoney.ui;

import br.com.mypersonalmoney.category.Category;
import br.com.mypersonalmoney.category.SubCategory;
import br.com.mypersonalmoney.ledger.LedgerQueryService;
import br.com.mypersonalmoney.ledger.LedgerService;
import br.com.mypersonalmoney.util.MoneyParser;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Path("/")
public class UiResource {

    @Inject
    @Location("ui/index.html")
    Template index;

    @Inject
    @Location("ui/account_card.html")
    Template accountCard;

    @Inject
    LedgerQueryService query;

    @Inject
    LedgerService ledger;

    @Inject
    @Location("ui/subcategory_options.html")
    Template subCategoryOptions;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance home() {
        return index
                .data("accounts", query.listAccountCards())
                .data("categories", Category.<Category>list("active=true order by type, name"))
                .data("today", java.time.LocalDate.now().toString());
    }

    // HTMX: criar INCOME (form urlencoded)
    @POST
    @Path("/ui/accounts/{accountId}/income")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance income(@PathParam("accountId") Long accountId,
                                   @FormParam("categoryId") Long categoryId,
                                   @FormParam("subCategoryId") Long subCategoryId,
                                   @FormParam("amount") String amount,
                                   @FormParam("date") String date,
                                   @FormParam("description") String description) {

        ledger.createIncome(
                accountId,
                categoryId,
                subCategoryId,
                MoneyParser.parseBRL(amount),
                parseDateOrToday(date),
                description
        );

        return renderSingleCard(accountId);
    }

    // HTMX: criar EXPENSE (form urlencoded)
    @POST
    @Path("/ui/accounts/{accountId}/expense")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance expense(@PathParam("accountId") Long accountId,
                                    @FormParam("categoryId") Long categoryId,
                                    @FormParam("subCategoryId") Long subCategoryId,
                                    @FormParam("amount") String amount,
                                    @FormParam("date") String date,
                                    @FormParam("description") String description) {

        ledger.createExpense(
                accountId,
                categoryId,
                subCategoryId,
                MoneyParser.parseBRL(amount),
                parseDateOrToday(date),
                description
        );

        return renderSingleCard(accountId);
    }

    private TemplateInstance renderSingleCard(Long accountId) {
        var card = query.listAccountCards().stream()
                .filter(a -> a.id().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("account not found"));

        var cats = br.com.mypersonalmoney.category.Category.<br.com.mypersonalmoney.category.Category>
                list("active = true order by type, name");

        return accountCard
                .data("a", card)
                .data("categories", cats)
                .data("today", java.time.LocalDate.now().toString());
    }

    private static LocalDate parseDateOrToday(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        return LocalDate.parse(s.trim());
    }

    @POST
    @Path("/ui/accounts/{accountId}/posting")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance posting(@PathParam("accountId") Long accountId,
                                    @FormParam("categoryId") Long categoryId,
                                    @FormParam("subCategoryId") Long subCategoryId,
                                    @FormParam("amount") String amount,
                                    @FormParam("date") String date,
                                    @FormParam("description") String description) {

        ledger.createPosting(
                accountId,
                categoryId,
                subCategoryId,
                MoneyParser.parseBRL(amount),      // ✅ aceita 50,50
                parseDateOrToday(date),
                description
        );

        return renderSingleCard(accountId);
    }

    @GET
    @Path("/ui/subcategories/options")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance subCategoryOptions(@QueryParam("categoryId") Long categoryId) {

        if (categoryId == null) {
            return subCategoryOptions.data("subs", List.of());
        }

        var subs = SubCategory.<SubCategory>list(
                "category.id = ?1 and active = true order by name",
                categoryId
        );

        return subCategoryOptions.data("subs", subs);
    }

}
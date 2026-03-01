package br.com.mypersonalmoney.ui;

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

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance home() {
        return index.data("accounts", query.listAccountCards());
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
                new BigDecimal(amount),
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

        return accountCard.data("a", card);
    }

    private static LocalDate parseDateOrToday(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        return LocalDate.parse(s.trim());
    }
}
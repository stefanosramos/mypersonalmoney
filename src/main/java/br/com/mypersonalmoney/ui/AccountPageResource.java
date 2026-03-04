package br.com.mypersonalmoney.ui;

import br.com.mypersonalmoney.category.Category;
import br.com.mypersonalmoney.ledger.LedgerQueryService;
import br.com.mypersonalmoney.ledger.LedgerService;
import br.com.mypersonalmoney.util.MoneyParser;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;

import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Path("/accounts")
@Produces(MediaType.TEXT_HTML)
public class AccountPageResource {

    @Inject LedgerQueryService query;
    @Inject LedgerService ledger;

    @Inject @Location("ui/account_page.html")
    Template page;

    @Inject @Location("ui/account_entries.html")
    Template entries;

    @Inject @Location("ui/account_expense_summary.html")
    Template expenseSummary;

    @GET
    @Path("/{accountId}")
    public TemplateInstance view(@PathParam("accountId") Long accountId,
                                 @QueryParam("range") String range) {
        var cards = query.listAccountCards();
        var card = cards.stream().filter(a -> a.id().equals(accountId)).findFirst()
                .orElseThrow(() -> new NotFoundException("account not found"));

        var cats = Category.<Category>list("active=true order by type, name");

        Range r = Range.fromKey(range);
        String rk = (range == null || range.isBlank()) ? "month_current" : range;

        return page
                .data("a", card)
                .data("categories", cats)
                .data("today", LocalDate.now().toString())
                .data("range", rk)
                .data("entries", query.listEntries(accountId, r.from, r.to, 50))
                .data("expenseRows", query.expenseByCategory(accountId, r.from, r.to));
    }

    // HTMX: trocar listagem
    @GET
    @Path("/{accountId}/entries")
    public TemplateInstance entries(@PathParam("accountId") Long accountId,
                                    @QueryParam("range") String range) {
        Range r = Range.fromKey(range);
        return entries
                .data("entries", query.listEntries(accountId, r.from, r.to, r.limit))
                .data("accountId", accountId)
                .data("range", range);
    }

    // HTMX: trocar resumo despesas
    @GET
    @Path("/{accountId}/expense-summary")
    public TemplateInstance expenseSummary(@PathParam("accountId") Long accountId,
                                           @QueryParam("range") String range) {
        Range r = Range.fromKey(range);
        return expenseSummary.data("expenseRows", query.expenseByCategory(accountId, r.from, r.to));
    }

    // POST lançamento (mesma lógica da home)
    @POST
    @Path("/{accountId}/posting")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response posting(@PathParam("accountId") Long accountId,
                                    @FormParam("categoryId") Long categoryId,
                                    @FormParam("subCategoryId") Long subCategoryId,
                                    @FormParam("amount") String amount,
                                    @FormParam("date") String date,
                                    @FormParam("description") String description,
                                    @FormParam("range") String range) {

        ledger.createPosting(
                accountId,
                categoryId,
                subCategoryId,
                MoneyParser.parseBRL(amount),
                (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date),
                description
        );

        // após lançar, re-renderiza a página inteira (mais simples na V1)
        String r = (range == null || range.isBlank()) ? "month_current" : range;

        // 303 = “See Other” (POST -> GET)
        return Response.seeOther(java.net.URI.create("/accounts/" + accountId + "?range=" + r))
                .status(303)
                .build();
    }

    static class Range {
        final LocalDate from;
        final LocalDate to;
        final Integer limit;

        Range(LocalDate from, LocalDate to, Integer limit) {
            this.from = from; this.to = to; this.limit = limit;
        }

        static Range currentMonth() {
            YearMonth ym = YearMonth.now();
            return new Range(ym.atDay(1), ym.atEndOfMonth(), 200);
        }

        static Range previousMonth() {
            YearMonth ym = YearMonth.now().minusMonths(1);
            return new Range(ym.atDay(1), ym.atEndOfMonth(), 200);
        }

        static Range lastDays(int days) {
            LocalDate to = LocalDate.now();
            LocalDate from = to.minusDays(days - 1L);
            return new Range(from, to, 500);
        }

        static Range all() {
            return new Range(null, null, 500);
        }

        static Range fromKey(String key) {
            if (key == null) return currentMonth();
            return switch (key) {
                case "all" -> all();
                case "month_current" -> currentMonth();
                case "month_prev" -> previousMonth();
                case "last10" -> lastDays(10);
                case "last15" -> lastDays(15);
                case "last30" -> lastDays(30);
                default -> currentMonth();
            };
        }
    }
    @GET
    @Path("/{accountId}/expense-summary.json")
    @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> expenseSummaryJson(@PathParam("accountId") Long accountId,
                                                        @QueryParam("range") String range) {

        Range r = Range.fromKey(range);
        var rows = query.expenseByCategory(accountId, r.from, r.to);

        // JSON simples: [{label, total}]
        return rows.stream()
                .map(x -> Map.<String, Object>of(
                        "label", x.categoryName(),
                        "total", x.total()   // BigDecimal vai como number/string dependendo do Jackson
                ))
                .toList();
    }

    @POST
    @Path("/{accountId}/txns/{txnId}/delete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteTxn(@PathParam("accountId") Long accountId,
                              @PathParam("txnId") Long txnId,
                              @FormParam("range") String range) {

        ledger.deleteTxn(accountId, txnId);

        String r = (range == null || range.isBlank()) ? "month_current" : range;

        return Response.seeOther(URI.create("/accounts/" + accountId + "?range=" + r)).build();
    }
}
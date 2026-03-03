package br.com.mypersonalmoney.ledger;

import br.com.mypersonalmoney.account.AccountType;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.LocalDate;

@Path("/ledger")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LedgerResource {

    private final LedgerService service;

    public LedgerResource(LedgerService service) {
        this.service = service;
    }

    public record CreateAccountRequest(String name, AccountType type) {}
    public record CreateAccountResponse(Long id) {}

    @POST
    @Path("/accounts")
    public CreateAccountResponse createAccount(CreateAccountRequest req) {
        if (req == null) throw new BadRequestException("body is required");
        Long id = service.createAccount(req.name(), req.type());
        return new CreateAccountResponse(id);
    }

    public record TransferRequest(Long fromAccountId, Long toAccountId, BigDecimal amount, LocalDate date, String description) {}
    public record TransferResponse(Long ledgerTxnId) {}

    @POST
    @Path("/transfer")
    public TransferResponse transfer(TransferRequest req) {
        if (req == null) throw new BadRequestException("body is required");
        Long txnId = service.createTransfer(req.fromAccountId(), req.toAccountId(), req.amount(), req.date(), req.description());
        return new TransferResponse(txnId);
    }

    public record BalanceResponse(Long accountId, BigDecimal balance, LocalDate from, LocalDate to) {}

    @GET
    @Path("/accounts/{id}/balance")
    public BalanceResponse balance(@PathParam("id") Long accountId,
                                   @QueryParam("from") String from,
                                   @QueryParam("to") String to) {

        LocalDate fromDate = (from == null || from.isBlank()) ? null : LocalDate.parse(from);
        LocalDate toDate   = (to == null || to.isBlank()) ? null : LocalDate.parse(to);

        BigDecimal bal = service.balance(accountId, fromDate, toDate);
        return new BalanceResponse(accountId, bal, fromDate, toDate);
    }

    public record DepositRequest(Long accountId, BigDecimal amount, LocalDate date, String description) {}
    public record DepositResponse(Long ledgerTxnId) {}

    @POST
    @Path("/deposit")
    public DepositResponse deposit(DepositRequest req) {
        if (req == null) throw new BadRequestException("body is required");
        Long id = service.createDeposit(req.accountId(), req.amount(), req.date(), req.description());
        return new DepositResponse(id);
    }

    @GET
    @Path("/ping")
    public Response ping() {
        return Response.ok("pong").build();
    }

    public record IncomeRequest(Long accountId, Long categoryId, Long subCategoryId, BigDecimal amount, LocalDate date, String description) {}
    public record IncomeResponse(Long ledgerTxnId) {}

    @POST
    @Path("/income")
    public IncomeResponse income(IncomeRequest req) {
        if (req == null) throw new BadRequestException("body is required");
        Long id = service.createIncome(req.accountId(), req.categoryId(), req.subCategoryId(), req.amount(), req.date(), req.description());
        return new IncomeResponse(id);
    }

    public record ExpenseRequest(Long accountId, Long categoryId, Long subCategoryId, BigDecimal amount, LocalDate date, String description) {}
    public record ExpenseResponse(Long ledgerTxnId) {}

    @POST
    @Path("/expense")
    public ExpenseResponse expense(ExpenseRequest req) {
        if (req == null) throw new BadRequestException("body is required");
        Long id = service.createExpense(req.accountId(), req.categoryId(), req.subCategoryId(), req.amount(), req.date(), req.description());
        return new ExpenseResponse(id);
    }
}
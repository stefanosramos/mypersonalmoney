package br.com.mypersonalmoney.ledger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import br.com.mypersonalmoney.category.Category;
import br.com.mypersonalmoney.category.CategoryType;
import br.com.mypersonalmoney.category.SubCategory;

import java.math.BigDecimal;
import java.time.LocalDate;


@ApplicationScoped
public class LedgerService {

    private final EntityManager em;

    public LedgerService(EntityManager em) {
        this.em = em;
    }

    @Transactional
    public Long createTransfer(Long fromAccountId,
                               Long toAccountId,
                               BigDecimal amount,
                               LocalDate date,
                               String description) {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("fromAccountId and toAccountId must be different");
        }

        Account from = Account.findById(fromAccountId);
        Account to = Account.findById(toAccountId);

        if (from == null) throw new IllegalArgumentException("from account not found");
        if (to == null) throw new IllegalArgumentException("to account not found");
        if (!from.active) throw new IllegalArgumentException("from account inactive");
        if (!to.active) throw new IllegalArgumentException("to account inactive");

        LedgerTxn txn = new LedgerTxn();
        txn.txnDate = date != null ? date : LocalDate.now();
        txn.description = description;
        txn.persist();

        LedgerEntry debit = new LedgerEntry();
        debit.txn = txn;
        debit.account = from;
        debit.amount = amount;
        debit.direction = EntryDirection.DEBIT;
        debit.memo = "Transfer out";
        debit.persist();

        LedgerEntry credit = new LedgerEntry();
        credit.txn = txn;
        credit.account = to;
        credit.amount = amount;
        credit.direction = EntryDirection.CREDIT;
        credit.memo = "Transfer in";
        credit.persist();

        return txn.id;
    }

    public BigDecimal balance(Long accountId, LocalDate from, LocalDate to) {
        // Native SQL performático: soma créditos - débitos (por data)
        String sql = """
            select coalesce(sum(
                case e.direction
                    when 'CREDIT' then e.amount
                    when 'DEBIT' then -e.amount
                end
            ), 0)
            from ledger_entry e
            join ledger_txn t on t.id = e.ledger_txn_id
            where e.account_id = :accountId
              and (:fromDate is null or t.txn_date >= :fromDate)
              and (:toDate   is null or t.txn_date <= :toDate)
            """;

        Object result = em.createNativeQuery(sql)
                .setParameter("accountId", accountId)
                .setParameter("fromDate", from)
                .setParameter("toDate", to)
                .getSingleResult();

        return (BigDecimal) result;
    }

    @Transactional
    public Long createDeposit(Long accountId,
                              BigDecimal amount,
                              LocalDate date,
                              String description) {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

        Account account = Account.findById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("account not found");
        }
        if (!account.active) {
            throw new IllegalArgumentException("account inactive");
        }

        LedgerTxn txn = new LedgerTxn();
        txn.txnDate = date != null ? date : LocalDate.now();
        txn.description = description;
        txn.persist();

        LedgerEntry credit = new LedgerEntry();
        credit.txn = txn;
        credit.account = account;
        credit.amount = amount;
        credit.direction = EntryDirection.CREDIT;
        credit.memo = "Deposit";
        credit.persist();

        return txn.id;
    }


    @Transactional
    public Long createAccount(String name, AccountType type) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (type == null) throw new IllegalArgumentException("type is required");

        Account a = new Account();
        a.name = name.trim();
        a.type = type;
        a.active = true;
        a.persist();
        return a.id;
    }

    @Transactional
    public Long createIncome(Long accountId,
                             Long categoryId,
                             Long subCategoryId,
                             BigDecimal amount,
                             LocalDate date,
                             String description) {
        return createSingleEntry(accountId, categoryId, subCategoryId, amount, date, description, EntryDirection.CREDIT, CategoryType.INCOME);
    }

    @Transactional
    public Long createExpense(Long accountId,
                              Long categoryId,
                              Long subCategoryId,
                              BigDecimal amount,
                              LocalDate date,
                              String description) {
        return createSingleEntry(accountId, categoryId, subCategoryId, amount, date, description, EntryDirection.DEBIT, CategoryType.EXPENSE);
    }

    private Long createSingleEntry(Long accountId,
                                   Long categoryId,
                                   Long subCategoryId,
                                   BigDecimal amount,
                                   LocalDate date,
                                   String description,
                                   EntryDirection direction,
                                   CategoryType requiredCategoryType) {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

        Account account = Account.findById(accountId);
        if (account == null) throw new IllegalArgumentException("account not found");
        if (!account.active) throw new IllegalArgumentException("account inactive");

        // Category/subcategory validation:
        Category category = null;
        SubCategory subCategory = null;

        if (subCategoryId != null) {
            subCategory = SubCategory.findById(subCategoryId);
            if (subCategory == null) throw new IllegalArgumentException("subcategory not found");
            if (!subCategory.active) throw new IllegalArgumentException("subcategory inactive");

            category = subCategory.category; // implicit
            if (category == null) throw new IllegalArgumentException("subcategory has no category");
        } else {
            if (categoryId == null) throw new IllegalArgumentException("category is required when subcategory is not provided");
            category = Category.findById(categoryId);
        }

        if (category == null) throw new IllegalArgumentException("category not found");
        if (!category.active) throw new IllegalArgumentException("category inactive");
        if (category.type != requiredCategoryType) {
            throw new IllegalArgumentException("category type must be " + requiredCategoryType);
        }

        // If both provided, ensure coherence
        if (subCategory != null && categoryId != null && !category.id.equals(categoryId)) {
            throw new IllegalArgumentException("categoryId must match subcategory.category");
        }

        LedgerTxn txn = new LedgerTxn();
        txn.txnDate = (date != null) ? date : LocalDate.now();
        txn.description = description;
        txn.persist();

        LedgerEntry entry = new LedgerEntry();
        entry.txn = txn;
        entry.account = account;
        entry.amount = amount;
        entry.direction = direction;
        entry.memo = direction == EntryDirection.CREDIT ? "Income" : "Expense";
        entry.category = category;
        entry.subCategory = subCategory;
        entry.persist();

        return txn.id;
    }
}
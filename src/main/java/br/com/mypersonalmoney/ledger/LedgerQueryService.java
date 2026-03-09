package br.com.mypersonalmoney.ledger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class LedgerQueryService {

    private final EntityManager em;

    public LedgerQueryService(EntityManager em) {
        this.em = em;
    }

    // Thread-safe access to NumberFormat via synchronization
    private static final NumberFormat BRL_FORMAT =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public record AccountCard(
            Long id,
            String name,
            String type,
            boolean active,
            BigDecimal balance,
            String balanceFormatted
    ) {}

    public List<AccountCard> listAccountCards() {

        String sql = """
            select
              a.id,
              a.name,
              a.type,
              a.active,
              coalesce(sum(
                case e.direction
                  when 'CREDIT' then e.amount
                  when 'DEBIT'  then -e.amount
                end
              ), 0) as balance
            from account a
            left join ledger_entry e on e.account_id = a.id
            group by a.id, a.name, a.type, a.active
            order by a.active desc, a.name
            """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        return rows.stream()
                .map(this::mapRowToAccountCard)
                .toList();
    }

    private AccountCard mapRowToAccountCard(Object[] r) {
        // Columns:
        // 0: id (number)
        // 1: name (string)
        // 2: type (string)
        // 3: active (boolean)
        // 4: balance (numeric)

        Long id = toLong(r[0]);
        String name = (String) r[1];
        String type = (String) r[2];
        boolean active = (Boolean) r[3];

        BigDecimal balance = toBigDecimal(r[4]);
        String balanceFormatted = formatBRL(balance);

        return new AccountCard(id, name, type, active, balance, balanceFormatted);
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;

        // Some drivers may return other numeric types
        if (v instanceof Number n) {
            // For currency, numeric(19,2) should normally be BigDecimal, but this is safe fallback
            return BigDecimal.valueOf(n.doubleValue());
        }

        return new BigDecimal(v.toString());
    }

    private static String formatBRL(BigDecimal value) {
        synchronized (BRL_FORMAT) {
            return BRL_FORMAT.format(value);
        }
    }

    public record EntryRow(
            Long txnId,
            java.time.LocalDate date,
            String description,
            String direction,
            java.math.BigDecimal amount,
            String amountFormatted,
            String categoryName,
            String subCategoryName
    ) {}

    public record ExpenseByCategoryRow(
            String categoryName,
            java.math.BigDecimal total,
            String totalFormatted
    ) {}

    public List<EntryRow> listEntries(Long accountId, LocalDate from, LocalDate to, Integer limit) {

        StringBuilder sql = new StringBuilder("""
        select
          t.id as txn_id,
          t.txn_date,
          t.description,
          e.direction,
          e.amount,
          c.name as category_name,
          s.name as subcategory_name
        from ledger_entry e
        join ledger_txn t on t.id = e.ledger_txn_id
        left join category c on c.id = e.category_id
        left join subcategory s on s.id = e.subcategory_id
        where e.account_id = :accountId
        """);

        if (from != null) sql.append(" and t.txn_date >= :from");
        if (to != null)   sql.append(" and t.txn_date <= :to");

        sql.append(" order by t.txn_date desc, t.id desc, e.id desc");

        var q = em.createNativeQuery(sql.toString(), Object[].class)
                .setParameter("accountId", accountId);

        if (from != null) q.setParameter("from", from);
        if (to != null)   q.setParameter("to", to);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        var mapped = rows.stream()
                .map(r -> {
                    BigDecimal amount = toBigDecimal(r[4]);

                    return new EntryRow(
                            ((Number) r[0]).longValue(),
                            (LocalDate) r[1],
                            (String) r[2],
                            (String) r[3],
                            amount,
                            formatBRL(amount),
                            (String) r[5],
                            (String) r[6]
                    );
                })
                .toList();

        if (limit == null || limit <= 0 || mapped.size() <= limit) return mapped;
        return mapped.subList(0, limit);
    }

    public List<ExpenseByCategoryRow> expenseByCategory(Long accountId, LocalDate from, LocalDate to) {

        StringBuilder sql = new StringBuilder("""
        select
          coalesce(c.name, '(sem categoria)') as category_name,
          sum(e.amount) as total
        from ledger_entry e
        join ledger_txn t on t.id = e.ledger_txn_id
        left join category c on c.id = e.category_id
        where e.account_id = :accountId
          and e.direction = 'DEBIT'
        """);

        if (from != null) sql.append(" and t.txn_date >= :from");
        if (to != null)   sql.append(" and t.txn_date <= :to ");

        sql.append("""
        group by coalesce(c.name, '(sem categoria)')
        order by total desc
        """);

        var q = em.createNativeQuery(sql.toString(), Object[].class)
                .setParameter("accountId", accountId);

        if (from != null) q.setParameter("from", from);
        if (to != null)   q.setParameter("to", to);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        return rows.stream()
                .map(r -> {
                    var total = toBigDecimal(r[1]);
                    return new ExpenseByCategoryRow((String) r[0], total, formatBRL(total));
                })
                .toList();
    }

    public BigDecimal consolidatedBalance() {
        String sql = """
        select coalesce(sum(x.balance), 0)
        from (
            select
              a.id,
              coalesce(sum(
                case e.direction
                  when 'CREDIT' then e.amount
                  when 'DEBIT'  then -e.amount
                end
              ), 0) as balance
            from account a
            left join ledger_entry e on e.account_id = a.id
            where a.active = true
            group by a.id
        ) x
        """;

        Object result = em.createNativeQuery(sql).getSingleResult();
        return toBigDecimal(result);
    }

    public String consolidatedBalanceFormatted() {
        return formatBRL(consolidatedBalance());
    }

}
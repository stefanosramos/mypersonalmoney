package br.com.mypersonalmoney.ledger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.text.NumberFormat;
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
}
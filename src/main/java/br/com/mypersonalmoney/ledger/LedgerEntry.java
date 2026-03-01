package br.com.mypersonalmoney.ledger;

import br.com.mypersonalmoney.category.Category;
import br.com.mypersonalmoney.category.SubCategory;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ledger_entry")
public class LedgerEntry extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_txn_id", nullable = false)
    public LedgerTxn txn;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    public Account account;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    public EntryDirection direction;

    @Column(length = 500)
    public String memo;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    public Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    public SubCategory subCategory;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
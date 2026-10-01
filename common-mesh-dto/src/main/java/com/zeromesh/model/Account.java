package com.zeromesh.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Represents a user's UPI account.
 * Maps to the "accounts" table in PostgreSQL.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upi_id", unique = true, nullable = false)
    private String upiId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    /**
     * Optimistic locking — prevents double-spend race conditions.
     * If two settlements try to debit the same account simultaneously,
     * the second write detects a version mismatch and retries.
     */
    @Version
    private Long version;

    public Account() {}

    public Account(String upiId, String name, BigDecimal balance) {
        this.upiId   = upiId;
        this.name    = name;
        this.balance = balance;
    }

    public Long getId()            { return id; }
    public String getUpiId()       { return upiId; }
    public void setUpiId(String v) { this.upiId = v; }
    public String getName()        { return name; }
    public void setName(String v)  { this.name = v; }
    public BigDecimal getBalance()          { return balance; }
    public void setBalance(BigDecimal v)    { this.balance = v; }
    public Long getVersion()       { return version; }

    @Override
    public String toString() {
        return "Account{upiId='" + upiId + "', name='" + name + "', balance=" + balance + "}";
    }
}

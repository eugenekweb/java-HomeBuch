package dev.micartera.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.*;

@Data
public class Wallet {
    private final UUID userId;
    private BigDecimal balance;
    private List<Category> categories;
    private Map<UUID, Budget> budgets;
    private List<Transaction> activeTransactions;
    private List<Transaction> transactionHistory;

    public Wallet(UUID userId) {
        this.userId = userId;
        this.balance = BigDecimal.ZERO;
        this.categories = new ArrayList<>();
        this.budgets = new HashMap<>();
        this.activeTransactions = new ArrayList<>();
        this.transactionHistory = new ArrayList<>();
    }

    @JsonCreator
    public Wallet(
            @JsonProperty("userId") UUID userId,
            @JsonProperty("balance") BigDecimal balance,
            @JsonProperty("categories") List<Category> categories,
            @JsonProperty("budgets") Map<UUID, Budget> budgets,
            @JsonProperty("activeTransactions") List<Transaction> activeTransactions,
            @JsonProperty("transactionHistory") List<Transaction> transactionHistory
    ) {
        this.userId = userId;
        this.balance = balance;
        this.categories = categories;
        this.budgets = budgets;
        this.activeTransactions = activeTransactions;
        this.transactionHistory = transactionHistory;
    }
}
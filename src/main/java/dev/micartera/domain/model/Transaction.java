package dev.micartera.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.micartera.domain.model.Category;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Transaction {
    private final UUID id;
    private final TransactionType type;
    private final BigDecimal amount;
    private final Category category;
    private final LocalDateTime created;
    private final TransactionStatus status;
    private final String description;
    private UUID senderId;
    private UUID receiverId;
    private LocalDateTime expires;

    public enum TransactionType {
        INCOME, EXPENSE, TRANSFER
    }

    public enum TransactionStatus {
        PENDING, APPROVED, REJECTED, CANCELLED
    }

    @JsonCreator
    public Transaction(
            @JsonProperty("id") UUID id,
            @JsonProperty("type") TransactionType type,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("category") Category category,
            @JsonProperty("created") LocalDateTime created,
            @JsonProperty("status") TransactionStatus status,
            @JsonProperty("description") String description,
            @JsonProperty("senderId") UUID senderId,
            @JsonProperty("receiverId") UUID receiverId,
            @JsonProperty("expires") LocalDateTime expires) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.created = created;
        this.status = status;
        this.description = description;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.expires = expires;
    }
}

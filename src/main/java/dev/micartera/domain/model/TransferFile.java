package dev.micartera.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TransferFile {
    private UUID transactionId;
    private LocalDateTime created;
    private String senderLogin;
    private String receiverLogin;
    private UUID receiverId;
    private BigDecimal amount;
    private byte[] encryptedData;

    @JsonCreator
    public TransferFile(
            @JsonProperty("transactionId") UUID transactionId,
            @JsonProperty("created") LocalDateTime created,
            @JsonProperty("senderLogin") String senderLogin,
            @JsonProperty("receiverLogin") String receiverLogin,
            @JsonProperty("receiverId") UUID receiverId,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("encryptedData") byte[] encryptedData) {
        this.transactionId = transactionId;
        this.created = created;
        this.senderLogin = senderLogin;
        this.receiverLogin = receiverLogin;
        this.receiverId = receiverId;
        this.amount = amount;
        this.encryptedData = encryptedData;
    }
}

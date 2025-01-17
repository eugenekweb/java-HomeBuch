package dev.micartera.domain.service;

import dev.micartera.domain.model.*;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionService {
    private final WalletService walletService;

    public TransactionService(WalletService walletService) {
        this.walletService = walletService;
    }

    // Заглушки для методов переводов
    public void createTransfer(UUID fromUserId, String toUserLogin, BigDecimal amount) {
        throw new UnsupportedOperationException("Переводы временно недоступны");
    }

    public void approveTransfer(UUID userId, UUID transferId) {
        throw new UnsupportedOperationException("Переводы временно недоступны");
    }

    public void rejectTransfer(UUID userId, UUID transferId) {
        throw new UnsupportedOperationException("Переводы временно недоступны");
    }

    public List<Transaction> getTransactionHistory(UUID currentUserId, LocalDateTime startDate, LocalDateTime endDate) {
        return null; // TODO: реализовать
    }
}
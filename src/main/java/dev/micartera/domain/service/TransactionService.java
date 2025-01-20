package dev.micartera.domain.service;

import dev.micartera.domain.model.Transaction;
import dev.micartera.domain.model.User;
import dev.micartera.domain.model.Wallet;
import dev.micartera.presentation.service.SessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private final WalletService walletService;
    private final SessionState sessionState;

    public TransactionService(WalletService walletService, SessionState sessionState) {
        this.walletService = walletService;
        this.sessionState = sessionState;
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

    public List<Transaction> getTransactionHistory(LocalDateTime startDate, LocalDateTime endDate) {
        User user = sessionState.getCurrentUser();
        logger.debug("Запрос истории транзакций: userId={}, период: {} - {}", user.getLogin(), startDate, endDate);

        Wallet wallet = sessionState.getCurrentWallet();
        try {
            List<Transaction> transactions = wallet.getTransactionHistory().stream()
                    .filter(tx -> !tx.getCreated().isBefore(startDate) && !tx.getCreated().isAfter(endDate))
                    .sorted(Comparator.comparing(Transaction::getCreated).reversed())
                    .toList();
            logger.info("История транзакций успешно получена для пользователя: {}", user.getLogin());
            return transactions;
        } catch (Exception e) {
            logger.error("Ошибка при получении истории транзакций", e);
            throw e;
        }
    }
}
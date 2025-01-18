package dev.micartera.domain.service;

import dev.micartera.domain.model.*;
import dev.micartera.presentation.service.SessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
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
        logger.debug("Запрос истории транзакций: userId={}, период: {} - {}",
                user.getId(), startDate, endDate);
        try {

            return null; // TODO: реализовать

//            logger.info("История транзакций успешно получена для пользователя: {}", user.getId());
        } catch (Exception e) {
            logger.error("Ошибка при получении истории транзакций", e);
            throw e;
        }
    }
}
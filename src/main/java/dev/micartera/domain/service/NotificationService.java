package dev.micartera.domain.service;

import dev.micartera.domain.model.Budget;
import dev.micartera.domain.model.Category;
import dev.micartera.domain.model.Transaction;
import dev.micartera.presentation.service.SessionState;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

@RequiredArgsConstructor
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final SessionState sessionState;
    private final List<String> notifications = new ArrayList<>();
    private final Map<UUID, Boolean> notificationSettings = new HashMap<>();

    public void notifyBudgetExceeded(Category category, Budget budget) {
        if (!isNotificationsEnabled(sessionState.getCurrentUser().getId())) return;
        String message = String.format("Превышение бюджета в категории '%s'. Лимит: %.2f, Потрачено: %.2f",
                category.getName(), budget.getLimit(), budget.getSpent());
        addNotification(message);
    }

    public void notifyLowBalance(BigDecimal balance, BigDecimal threshold) {
        if (!isNotificationsEnabled(sessionState.getCurrentUser().getId())) return;
        if (balance.compareTo(threshold) < 0) {
            String message = String.format("Низкий баланс: %.2f", balance);
            addNotification(message);
        }
    }

    public void notifyTransaction(Transaction transaction) {
        if (!isNotificationsEnabled(sessionState.getCurrentUser().getId())) return;
        String type = transaction.getType() == Transaction.TransactionType.INCOME ? "Доход" : "Расход";
        String message = String.format("%s: %.2f (%s)",
                type, transaction.getAmount(), transaction.getCategory().getName());
        addNotification(message);
    }

    public List<String> getNotifications() {
        return new ArrayList<>(notifications);
    }

    public void clearNotifications() {
        notifications.clear();
    }

    public void enableNotifications(UUID userId) {
        notificationSettings.put(userId, true);
        logger.info("Уведомления включены для пользователя: {}", userId);
    }

    public void disableNotifications(UUID userId) {
        notificationSettings.put(userId, false);
        logger.info("Уведомления отключены для пользователя: {}", userId);
    }

    public boolean isNotificationsEnabled(UUID userId) {
        return notificationSettings.getOrDefault(userId, true);
    }

    private void addNotification(String message) {
        notifications.add(message);
        logger.info("Notification: {}", message);
    }
}
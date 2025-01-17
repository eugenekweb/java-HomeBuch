package dev.micartera.domain.service;

import dev.micartera.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final List<String> notifications = new ArrayList<>();

    public void notifyBudgetExceeded(Category category, Budget budget) {
        String message = String.format("Превышение бюджета в категории '%s'. Лимит: %.2f, Потрачено: %.2f",
                category.getName(), budget.getLimit(), budget.getSpent());
        addNotification(message);
    }

    public void notifyLowBalance(BigDecimal balance, BigDecimal threshold) {
        if (balance.compareTo(threshold) < 0) {
            String message = String.format("Низкий баланс: %.2f", balance);
            addNotification(message);
        }
    }

    public void notifyTransaction(Transaction transaction) {
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

    private void addNotification(String message) {
        notifications.add(message);
        logger.info("Notification: {}", message);
    }
}
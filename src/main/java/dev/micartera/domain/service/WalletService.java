package dev.micartera.domain.service;

import dev.micartera.domain.model.*;
import dev.micartera.infrastructure.repository.WalletRepository;
import dev.micartera.domain.exception.ValidationException;
import dev.micartera.presentation.service.SessionState;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@RequiredArgsConstructor
public class WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final WalletRepository walletRepository;
    private final ValidationService validationService;
    private final NotificationService notificationService;
    private final SessionState sessionState;
    private static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("6350.00");


    public Wallet createWallet(UUID userId) {
        Wallet wallet = new Wallet(userId);
        return walletRepository.save(wallet);
    }

    public void addIncome(BigDecimal amount, Category category, String description) {
        if (!validationService.validateAmount(amount)) {
            throw new ValidationException("Некорректная сумма");
        }

        Wallet wallet = sessionState.getCurrentWallet();
        Transaction transaction = createTransaction(amount, category, description, Transaction.TransactionType.INCOME);

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.getTransactionHistory().add(transaction);
        notificationService.notifyTransaction(transaction);
        walletRepository.save(wallet);
    }

    public void addExpense(BigDecimal amount, Category category, String description) {
        if (!validationService.validateAmount(amount)) {
            throw new ValidationException("Некорректная сумма расхода");
        }

        Wallet wallet = sessionState.getCurrentWallet();
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new ValidationException("Недостаточно средств");
        }

        Transaction transaction = createTransaction(amount, category, description, Transaction.TransactionType.EXPENSE);
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.getTransactionHistory().add(transaction);
        notificationService.notifyTransaction(transaction);

        updateBudget(wallet, category, amount);
        checkLowBalance(wallet);
        walletRepository.save(wallet);
    }
    public void addCategory(String name, Category.CategoryType type) {
        Wallet wallet = sessionState.getCurrentWallet();
        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name(name)
                .type(type)
                .build();

        wallet.getCategories().add(category);
        walletRepository.save(wallet);
    }

    public void setBudget(UUID categoryId, BigDecimal limit) {
        if (!validationService.validateAmount(limit)) {
            throw new ValidationException("Некорректный лимит бюджета");
        }

        Wallet wallet = sessionState.getCurrentWallet();
        Budget budget = Budget.builder()
                .categoryId(categoryId)
                .limit(limit)
                .spent(BigDecimal.ZERO)
                .enabled(true)
                .setAtDate(LocalDateTime.now())
                .build();

        wallet.getBudgets().put(categoryId, budget);
        walletRepository.save(wallet);
    }

    private void updateBudget(Wallet wallet, Category category, BigDecimal amount) {
        if (category.getType() == Category.CategoryType.EXPENSE) {
            Budget budget = wallet.getBudgets().get(category.getId());
            if (budget != null && budget.isEnabled()) {
                budget.setSpent(budget.getSpent().add(amount));

                // Проверяем превышение бюджета и отправляем уведомление
                if (budget.getSpent().compareTo(budget.getLimit()) > 0) {
                    notificationService.notifyBudgetExceeded(category, budget);
                }
            }
        }
        walletRepository.save(wallet);
    }

    public void deleteBudget(UUID categoryId) {
        Wallet wallet = sessionState.getCurrentWallet();
        wallet.getBudgets().remove(categoryId);
        walletRepository.save(wallet);
    }

    // TODO: нужны еще методы для работы с бюджетом -
    //  приостановка, изменение лимита, изменение даты с которой он действует

    private Transaction createTransaction(BigDecimal amount, Category category, String description, Transaction.TransactionType type) {
        Transaction transaction = new Transaction(UUID.randomUUID(),
                type,
                amount,
                category,
                LocalDateTime.now(),
                Transaction.TransactionStatus.APPROVED,
                description,
                null, null, null);
        return transaction;
    }

    public Optional<Wallet> findWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId);
    }

    public void deleteCategory(UUID categoryId) {
        Wallet wallet = sessionState.getCurrentWallet();
        wallet.getCategories().removeIf(c -> c.getId().equals(categoryId));
        wallet.getBudgets().remove(categoryId);
        walletRepository.save(wallet);
    }

    private void checkLowBalance(Wallet wallet) {
        if (wallet.getBalance().compareTo(LOW_BALANCE_THRESHOLD) < 0) {
            notificationService.notifyLowBalance(wallet.getBalance(), LOW_BALANCE_THRESHOLD);
        }
    }

    public BigDecimal getCategoryMonthTotal(Category category) {
        LocalDateTime firstDayOfCurrentMonth = LocalDateTime.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .with(LocalTime.MIN);
        Wallet wallet = sessionState.getCurrentWallet();
        return wallet.getTransactionHistory().stream()
                .filter(t -> t.getCreated().isAfter(firstDayOfCurrentMonth))
                .filter(t -> t.getCategory().equals(category))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getFormattedCategoryType(Category category) {
        return switch (category.getType()) {
            case INCOME -> "Доходы";
            case EXPENSE -> "Расходы";
        };
    }
}

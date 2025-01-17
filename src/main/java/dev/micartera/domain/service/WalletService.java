package dev.micartera.domain.service;

import dev.micartera.domain.model.*;
import dev.micartera.domain.repository.WalletRepository;
import dev.micartera.domain.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class WalletService {
    private final WalletRepository walletRepository;
    private final ValidationService validationService;

    public WalletService(WalletRepository walletRepository, ValidationService validationService) {
        this.walletRepository = walletRepository;
        this.validationService = validationService;
    }

    public Wallet createWallet(UUID userId) {
        Wallet wallet = new Wallet(userId);
        return walletRepository.save(wallet);
    }

    public void addIncome(UUID userId, BigDecimal amount, Category category, String description) {
        if (!validationService.validateAmount(amount)) {
            throw new ValidationException("Некорректная сумма");
        }

        Wallet wallet = getWallet(userId);
        Transaction transaction = createTransaction(amount, category, description, Transaction.TransactionType.INCOME);

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.getTransactionHistory().add(transaction);

        updateBudget(wallet, category, amount);
        walletRepository.save(wallet);
    }

    public void addExpense(UUID userId, BigDecimal amount, Category category, String description) {
        if (!validationService.validateAmount(amount)) {
            throw new ValidationException("Некорректная сумма");
        }

        Wallet wallet = getWallet(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new ValidationException("Недостаточно средств");
        }

        Transaction transaction = createTransaction(amount, category, description, Transaction.TransactionType.EXPENSE);

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.getTransactionHistory().add(transaction);

        updateBudget(wallet, category, amount);
        walletRepository.save(wallet);
    }

    public void addCategory(UUID userId, String name, Category.CategoryType type) {
        Wallet wallet = getWallet(userId);
        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name(name)
                .type(type)
                .build();

        wallet.getCategories().add(category);
        walletRepository.save(wallet);
    }

    public void setBudget(UUID userId, UUID categoryId, BigDecimal limit) {
        if (!validationService.validateAmount(limit)) {
            throw new ValidationException("Некорректный лимит бюджета");
        }

        Wallet wallet = getWallet(userId);
        Budget budget = Budget.builder()
                .categoryId(categoryId)
                .limit(limit)
                .spent(BigDecimal.ZERO)
                .enabled(true)
                .build();

        wallet.getBudgets().put(categoryId, budget);
        walletRepository.save(wallet);
    }

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

    private void updateBudget(Wallet wallet, Category category, BigDecimal amount) {
        Budget budget = wallet.getBudgets().get(category.getId());
        if (budget != null && budget.isEnabled() && category.getType() == Category.CategoryType.EXPENSE) {
            budget.setSpent(budget.getSpent().add(amount));
        }
    }

    private Wallet getWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ValidationException("Кошелек не найден"));
    }

    public Optional<Wallet> findWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId);
    }

    public void deleteCategory(UUID currentUserId, UUID id) {
        // TODO: delete category
    }
}

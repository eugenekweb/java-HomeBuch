package dev.micartera.presentation.service;

import dev.micartera.domain.model.User;
import dev.micartera.domain.model.Wallet;
import dev.micartera.infrastructure.repository.impl.UserRepositoryImpl;
import dev.micartera.infrastructure.repository.impl.WalletRepositoryImpl;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SessionState {
    private static final Logger logger = LoggerFactory.getLogger(SessionState.class);
    @Getter private User currentUser;
    @Getter private Wallet currentWallet;
    private final WalletRepositoryImpl walletRepository;
    private final UserRepositoryImpl userRepository;

    public SessionState(WalletRepositoryImpl walletRepository, UserRepositoryImpl userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    public void setCurrentSession(UUID userId) {
        currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
        currentWallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Кошелек не найден"));
    }

    public void saveAndClose() {
        if (currentUser != null && currentWallet != null) {
            userRepository.save(currentUser);
            walletRepository.save(currentWallet);
            clear();
        }
    }

    private void clear() {
        currentUser = null;
        currentWallet = null;
    }
}
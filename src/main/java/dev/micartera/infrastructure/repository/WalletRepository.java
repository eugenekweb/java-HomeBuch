package dev.micartera.infrastructure.repository;

import dev.micartera.domain.model.Wallet;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
    Wallet save(Wallet wallet);

    Optional<Wallet> findByUserId(UUID userId);

    void delete(UUID userId);
}



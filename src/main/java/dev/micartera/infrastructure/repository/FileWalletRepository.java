package dev.micartera.infrastructure.repository;

import dev.micartera.domain.model.Wallet;
import dev.micartera.domain.repository.WalletRepository;
import dev.micartera.infrastructure.config.ApplicationConfig;
import dev.micartera.infrastructure.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class FileWalletRepository implements WalletRepository {
    private static final Logger logger = LoggerFactory.getLogger(FileWalletRepository.class);
    private final String walletsPath;

    public FileWalletRepository() {
        this.walletsPath = ApplicationConfig.getProperty("app.storage.path") + "/wallets/";
        new File(walletsPath).mkdirs();
    }

    @Override
    public Wallet save(Wallet wallet) {
        try {
            String json = JsonUtils.toJson(wallet);
            File file = new File(walletsPath + wallet.getUserId() + "_wallet.json");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json);
            }
            return wallet;
        } catch (IOException e) {
            logger.error("Error saving wallet: {}", wallet.getUserId(), e);
            throw new RuntimeException("Could not save wallet", e);
        }
    }

    @Override
    public Optional<Wallet> findByUserId(UUID userId) {
        File file = new File(walletsPath + userId + "_wallet.json");
        if (!file.exists()) {
            return Optional.empty();
        }

        try {
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            System.out.println(content);
            return Optional.of(JsonUtils.fromJson(content, Wallet.class));
        } catch (IOException e) {
            logger.error("Error reading wallet: {}", userId, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(UUID userId) {
        File file = new File(walletsPath + userId + "_wallet.json");
        if (!file.delete() && file.exists()) {
            logger.error("Could not delete wallet file: {}", userId);
            throw new RuntimeException("Could not delete wallet");
        }
    }
}
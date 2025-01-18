package dev.micartera.infrastructure.repository.impl;

import dev.micartera.domain.model.Transaction;
import dev.micartera.domain.model.TransferFile;
import dev.micartera.infrastructure.config.ApplicationConfig;
import dev.micartera.infrastructure.repository.TransactionRepository;
import dev.micartera.infrastructure.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionRepositoryImpl implements TransactionRepository {
    private static final Logger logger = LoggerFactory.getLogger(TransactionRepositoryImpl.class);
    private final String transfersPath;
    private final String usersPath;

    public TransactionRepositoryImpl() {
        String storagePath = ApplicationConfig.getProperty("app.storage.path");
        this.transfersPath = storagePath + "/transfers/";
        this.usersPath = storagePath + "/users/";
        new File(transfersPath).mkdirs();
    }

    @Override
    public Transaction saveTransaction(Transaction transaction) {
        try {
            String json = JsonUtils.toJson(transaction);
            File file = new File(usersPath + transaction.getSenderId() + "_transactions.json");
            List<Transaction> transactions = readTransactionList(file);
            transactions.add(transaction);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(JsonUtils.toJson(transactions));
            }
            return transaction;
        } catch (IOException e) {
            logger.error("Error saving transaction: " + transaction.getId(), e);
            throw new RuntimeException("Could not save transaction", e);
        }
    }

    private List<Transaction> readTransactionList(File file) throws IOException {
        if (!file.exists()) {
            return new ArrayList<>();
        }
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        return Arrays.asList(JsonUtils.fromJson(content, Transaction[].class));
    }

    @Override
    public TransferFile saveTransferFile(TransferFile transferFile, UUID userId, boolean isIncoming) {
        try {
            String directory = transfersPath + userId + "/" + (isIncoming ? "in" : "out") + "/";
            new File(directory).mkdirs();

            String json = JsonUtils.toJson(transferFile);
            File file = new File(directory + transferFile.getTransactionId() + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json);
            }
            return transferFile;
        } catch (IOException e) {
            logger.error("Error saving transfer file: " + transferFile.getTransactionId(), e);
            throw new RuntimeException("Could not save transfer file", e);
        }
    }

    @Override
    public Optional<Transaction> findTransactionById(UUID id) {
        File dir = new File(usersPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith("_transactions.json"));
        if (files == null) return Optional.empty();

        for (File file : files) {
            try {
                List<Transaction> transactions = readTransactionList(file);
                Optional<Transaction> found = transactions.stream()
                        .filter(t -> t.getId().equals(id))
                        .findFirst();
                if (found.isPresent()) {
                    return found;
                }
            } catch (IOException e) {
                logger.error("Error reading transactions file: " + file.getName(), e);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Transaction> findActiveTransactionsByUserId(UUID userId) {
        File file = new File(usersPath + userId + "_transactions.json");
        try {
            List<Transaction> transactions = readTransactionList(file);
            return transactions.stream()
                    .filter(t -> t.getStatus() == Transaction.TransactionStatus.PENDING)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error reading transactions: " + userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Transaction> findTransactionHistoryByUserId(UUID userId, LocalDateTime from, LocalDateTime to) {
        File file = new File(usersPath + userId + "_transactions.json");
        try {
            List<Transaction> transactions = readTransactionList(file);
            return transactions.stream()
                    .filter(t -> !t.getCreated().isBefore(from) && !t.getCreated().isAfter(to))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error reading transaction history: " + userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<TransferFile> findIncomingTransfers(UUID userId) {
        return findTransfers(userId, true);
    }

    @Override
    public List<TransferFile> findOutgoingTransfers(UUID userId) {
        return findTransfers(userId, false);
    }

    private List<TransferFile> findTransfers(UUID userId, boolean incoming) {
        String directory = transfersPath + userId + "/" + (incoming ? "in" : "out") + "/";
        File dir = new File(directory);
        if (!dir.exists()) {
            return new ArrayList<>();
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return new ArrayList<>();

        List<TransferFile> transfers = new ArrayList<>();
        for (File file : files) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                transfers.add(JsonUtils.fromJson(content, TransferFile.class));
            } catch (IOException e) {
                logger.error("Error reading transfer file: " + file.getName(), e);
            }
        }
        return transfers;
    }

    @Override
    public void deleteTransferFile(UUID transactionId, UUID userId, boolean isIncoming) {
        String directory = transfersPath + userId + "/" + (isIncoming ? "in" : "out") + "/";
        File file = new File(directory + transactionId + ".json");
        if (!file.delete() && file.exists()) {
            logger.error("Could not delete transfer file: " + transactionId);
            throw new RuntimeException("Could not delete transfer file");
        }
    }
}
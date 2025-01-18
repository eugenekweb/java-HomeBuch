package dev.micartera.infrastructure.repository;

import dev.micartera.domain.model.Transaction;
import dev.micartera.domain.model.TransferFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {
    Transaction saveTransaction(Transaction transaction);

    TransferFile saveTransferFile(TransferFile transferFile, UUID userId, boolean isIncoming);

    Optional<Transaction> findTransactionById(UUID id);

    List<Transaction> findActiveTransactionsByUserId(UUID userId);

    List<Transaction> findTransactionHistoryByUserId(UUID userId, LocalDateTime from, LocalDateTime to);

    List<TransferFile> findIncomingTransfers(UUID userId);

    List<TransferFile> findOutgoingTransfers(UUID userId);

    void deleteTransferFile(UUID transactionId, UUID userId, boolean isIncoming);
}

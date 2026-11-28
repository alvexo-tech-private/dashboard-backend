package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.PayoutBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayoutBatchRepository extends JpaRepository<PayoutBatch, Long> {

    @Query("SELECT b FROM PayoutBatch b JOIN FETCH b.mechanic WHERE b.batchDate = :day ORDER BY b.mechanic.id, b.id")
    List<PayoutBatch> findByBatchDate(@Param("day") LocalDate day);

    @Query("SELECT b FROM PayoutBatch b JOIN FETCH b.mechanic WHERE b.batchReference = :ref")
    Optional<PayoutBatch> findByReference(@Param("ref") String batchReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM PayoutBatch b WHERE b.batchReference = :ref")
    Optional<PayoutBatch> findByReferenceForUpdate(@Param("ref") String batchReference);

    // ── WPO-YYYYMMDD-NNNNNN payout references: the same per-day sequence table the app's payout run uses ──

    @Modifying
    @Query(value = "INSERT INTO payout_reference_sequences (seq_date, last_value) VALUES (:d, 0) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    void insertSequenceIfMissing(@Param("d") LocalDate day);

    @Query(value = "SELECT last_value FROM payout_reference_sequences WHERE seq_date = :d FOR UPDATE", nativeQuery = true)
    Integer lockSequence(@Param("d") LocalDate day);

    @Modifying
    @Query(value = "UPDATE payout_reference_sequences SET last_value = :v WHERE seq_date = :d", nativeQuery = true)
    void updateSequence(@Param("d") LocalDate day, @Param("v") int value);
}

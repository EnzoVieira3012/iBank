package com.ibank.card.repository;

import com.ibank.card.model.Invoice;
import com.ibank.card.model.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByCardId(UUID cardId);

    Page<Invoice> findByCardId(UUID cardId, Pageable pageable);

    List<Invoice> findByCardIdAndStatus(UUID cardId, InvoiceStatus status);

    Optional<Invoice> findByCardIdAndReferenceMonth(UUID cardId, LocalDate referenceMonth);

    List<Invoice> findByCardIdAndReferenceMonthBetween(UUID cardId, LocalDate start, LocalDate end);

    List<Invoice> findByStatus(InvoiceStatus status);

    List<Invoice> findByDueDateBefore(LocalDate date);

    List<Invoice> findByDueDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT i FROM Invoice i WHERE i.cardId = :cardId AND i.status IN (:statuses) ORDER BY i.dueDate ASC")
    List<Invoice> findOpenInvoicesByCard(@Param("cardId") UUID cardId, @Param("statuses") List<InvoiceStatus> statuses);
}
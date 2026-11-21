package com.ibank.card.repository;

import com.ibank.card.model.InvoiceInstallment;
import com.ibank.card.model.InstallmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceInstallmentRepository extends JpaRepository<InvoiceInstallment, UUID> {

    List<InvoiceInstallment> findByInvoiceId(UUID invoiceId);

    List<InvoiceInstallment> findByInvoiceIdOrderByInstallmentNumberAsc(UUID invoiceId);

    List<InvoiceInstallment> findByInvoiceIdAndStatus(UUID invoiceId, InstallmentStatus status);

    List<InvoiceInstallment> findByStatus(InstallmentStatus status);

    List<InvoiceInstallment> findByDueDateBefore(LocalDate date);

    List<InvoiceInstallment> findByDueDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT SUM(i.amount - i.paidAmount) FROM InvoiceInstallment i WHERE i.invoiceId = :invoiceId AND i.status != 'PAID'")
    BigDecimal sumPendingByInvoiceId(@Param("invoiceId") UUID invoiceId);
}
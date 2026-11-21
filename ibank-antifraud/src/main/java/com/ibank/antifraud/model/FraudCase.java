package com.ibank.antifraud.model;

import com.ibank.core.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_cases", indexes = {
    @Index(name = "idx_cases_customer", columnList = "customer_id"),
    @Index(name = "idx_cases_status", columnList = "status"),
    @Index(name = "idx_cases_assigned", columnList = "assigned_to"),
    @Index(name = "idx_cases_created", columnList = "created_at"),
    @Index(name = "idx_cases_sar", columnList = "sar_filed")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCase extends BaseEntity {

    @Column(name = "case_number", unique = true, nullable = false, length = 30)
    private String caseNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "card_id")
    private UUID cardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CaseStatus status;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(name = "first_transaction_date")
    private LocalDate firstTransactionDate;

    @Column(name = "last_transaction_date")
    private LocalDate lastTransactionDate;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "investigator_notes", columnDefinition = "text")
    private String investigatorNotes;

    @Column(name = "resolution", length = 1000)
    private String resolution;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "sar_filed", nullable = false)
    private Boolean sarFiled;

    @Column(name = "sar_filed_at")
    private LocalDateTime sarFiledAt;

    @Column(name = "sar_reference", length = 50)
    private String sarReference;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "escalated_to")
    private UUID escalatedTo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alert_ids", columnDefinition = "jsonb")
    private String alertIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence", columnDefinition = "jsonb")
    private String evidence;

    @Version
    private Long version;
}
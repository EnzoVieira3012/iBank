package com.ibank.fee.model;

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
@Table(name = "interest_rates", indexes = {
    @Index(name = "idx_interest_rate_index", columnList = "index_type"),
    @Index(name = "idx_interest_rate_product", columnList = "product_code"),
    @Index(name = "idx_interest_rate_date", columnList = "rate_date"),
    @Index(name = "idx_interest_rate_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestRate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "index_type", nullable = false, length = 20)
    private InterestIndex indexType;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "rate_value", nullable = false, precision = 15, scale = 8)
    private BigDecimal rateValue;

    @Column(name = "spread", precision = 10, scale = 6)
    private BigDecimal spread;

    @Column(name = "effective_rate", precision = 15, scale = 8)
    private BigDecimal effectiveRate;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Version
    private Long version;
}
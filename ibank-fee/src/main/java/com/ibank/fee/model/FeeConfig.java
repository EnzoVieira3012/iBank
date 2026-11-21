package com.ibank.fee.model;

import com.ibank.core.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_configs", indexes = {
    @Index(name = "idx_fee_config_type", columnList = "fee_type"),
    @Index(name = "idx_fee_config_product", columnList = "product_code"),
    @Index(name = "idx_fee_config_channel", columnList = "channel"),
    @Index(name = "idx_fee_config_active", columnList = "active"),
    @Index(name = "idx_fee_config_dates", columnList = "valid_from,valid_to")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeConfig extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 30)
    private FeeType feeType;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "channel", length = 20)
    private String channel;

    @Column(name = "customer_segment", length = 30)
    private String customerSegment;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 20)
    private FeePeriod period;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false, length = 20)
    private FeeCalculationType calculationType;

    @Column(name = "fixed_amount", precision = 19, scale = 4)
    private BigDecimal fixedAmount;

    @Column(name = "percentage_rate", precision = 10, scale = 6)
    private BigDecimal percentageRate;

    @Column(name = "min_amount", precision = 19, scale = 4)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tier_config", columnDefinition = "jsonb")
    private String tierConfig;

    @Column(name = "formula", length = 500)
    private String formula;

    @Column(name = "tax_included")
    private Boolean taxIncluded;

    @Column(name = "tax_rate", precision = 10, scale = 6)
    private BigDecimal taxRate;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "priority")
    private Integer priority;

    @Version
    private Long version;
}
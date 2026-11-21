package com.ibank.credit.model;

import com.ibank.core.model.BaseEntity;
import com.ibank.core.util.Money;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "collaterals", indexes = {
    @Index(name = "idx_collaterals_loan_id", columnList = "loan_id"),
    @Index(name = "idx_collaterals_type", columnList = "collateral_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Collateral extends BaseEntity {

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "collateral_type", length = 20, nullable = false)
    private CollateralType collateralType;

    @Column(name = "description", length = 500)
    private String description;

    @AttributeOverride(name = "amount", column = @Column(name = "appraised_value", precision = 19, scale = 2, nullable = false))
    private Money appraisedValue;

    @AttributeOverride(name = "amount", column = @Column(name = "pledged_value", precision = 19, scale = 2, nullable = false))
    private Money pledgedValue;

    @Column(name = "documentation_ref", length = 200)
    private String documentationRef;

    @Column(name = "registered_at")
    private OffsetDateTime registeredAt;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String metadata;
}
package com.ibank.credit.event;

import com.ibank.core.util.Money;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class LoanSettledEvent extends ApplicationEvent {

    private final UUID loanId;
    private final String contractNumber;
    private final UUID accountId;
    private final Money totalPaid;
    private final Money totalInterestPaid;
    private final int totalInstallments;
    private final boolean earlySettlement;
    private final Money discountAmount;

    public LoanSettledEvent(UUID loanId, String contractNumber, UUID accountId,
                            Money totalPaid, Money totalInterestPaid, int totalInstallments,
                            boolean earlySettlement, Money discountAmount) {
        super(loanId);
        this.loanId = loanId;
        this.contractNumber = contractNumber;
        this.accountId = accountId;
        this.totalPaid = totalPaid;
        this.totalInterestPaid = totalInterestPaid;
        this.totalInstallments = totalInstallments;
        this.earlySettlement = earlySettlement;
        this.discountAmount = discountAmount;
    }
}
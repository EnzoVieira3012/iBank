package com.ibank.credit.event;

import com.ibank.core.util.Money;
import com.ibank.credit.model.AmortizationType;
import com.ibank.credit.model.LoanType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;
import java.util.UUID;

@Getter
public class LoanContractedEvent extends ApplicationEvent {

    private final UUID loanId;
    private final String contractNumber;
    private final LoanType loanType;
    private final AmortizationType amortizationType;
    private final UUID accountId;
    private final String holderCpf;
    private final Money principalAmount;
    private final Money installmentAmount;
    private final int termMonths;
    private final LocalDate firstDueDate;
    private final java.math.BigDecimal interestRate;

    public LoanContractedEvent(UUID loanId, String contractNumber, LoanType loanType,
                               AmortizationType amortizationType, UUID accountId, String holderCpf,
                               Money principalAmount, Money installmentAmount, int termMonths,
                               LocalDate firstDueDate, java.math.BigDecimal interestRate) {
        super(loanId);
        this.loanId = loanId;
        this.contractNumber = contractNumber;
        this.loanType = loanType;
        this.amortizationType = amortizationType;
        this.accountId = accountId;
        this.holderCpf = holderCpf;
        this.principalAmount = principalAmount;
        this.installmentAmount = installmentAmount;
        this.termMonths = termMonths;
        this.firstDueDate = firstDueDate;
        this.interestRate = interestRate;
    }
}
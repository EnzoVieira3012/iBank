package com.ibank.credit.event;

import com.ibank.core.util.Money;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;
import java.util.UUID;

@Getter
public class InstallmentPaidEvent extends ApplicationEvent {

    private final UUID installmentId;
    private final UUID loanId;
    private final int installmentNumber;
    private final Money principalPaid;
    private final Money interestPaid;
    private final Money totalPaid;
    private final LocalDate dueDate;
    private final UUID transactionId;

    public InstallmentPaidEvent(UUID installmentId, UUID loanId, int installmentNumber,
                                Money principalPaid, Money interestPaid, Money totalPaid,
                                LocalDate dueDate, UUID transactionId) {
        super(installmentId);
        this.installmentId = installmentId;
        this.loanId = loanId;
        this.installmentNumber = installmentNumber;
        this.principalPaid = principalPaid;
        this.interestPaid = interestPaid;
        this.totalPaid = totalPaid;
        this.dueDate = dueDate;
        this.transactionId = transactionId;
    }
}
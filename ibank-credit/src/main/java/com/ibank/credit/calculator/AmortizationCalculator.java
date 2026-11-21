package com.ibank.credit.calculator;

import com.ibank.core.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public interface AmortizationCalculator {

    List<InstallmentSchedule> calculate(Money principal, BigDecimal monthlyRate, int termMonths, LocalDate firstDueDate);

    record InstallmentSchedule(
        int installmentNumber,
        LocalDate dueDate,
        Money principalAmount,
        Money interestAmount,
        Money totalAmount,
        Money remainingBalance
    ) {}
}
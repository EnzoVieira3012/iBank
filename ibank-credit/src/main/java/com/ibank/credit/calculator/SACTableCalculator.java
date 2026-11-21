package com.ibank.credit.calculator;

import com.ibank.core.util.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class SACTableCalculator implements AmortizationCalculator {

    @Override
    public List<InstallmentSchedule> calculate(Money principal, BigDecimal monthlyRate, int termMonths, LocalDate firstDueDate) {
        List<InstallmentSchedule> schedule = new ArrayList<>();
        
        BigDecimal rate = monthlyRate.setScale(10, RoundingMode.HALF_EVEN);
        Money principalPerInstallment = Money.of(principal.toBigDecimal()
            .divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_EVEN));
        
        Money remainingBalance = principal;

        for (int i = 1; i <= termMonths; i++) {
            // Adjust last installment for rounding differences
            if (i == termMonths) {
                principalPerInstallment = remainingBalance;
            }
            
            Money interestAmount = Money.of(remainingBalance.toBigDecimal()
                .multiply(rate)
                .setScale(2, RoundingMode.HALF_EVEN));
            
            Money installmentAmount = principalPerInstallment.add(interestAmount);
            remainingBalance = remainingBalance.subtract(principalPerInstallment);
            
            if (remainingBalance.isNegative() || remainingBalance.toBigDecimal().abs().compareTo(new BigDecimal("0.01")) < 0) {
                remainingBalance = Money.ZERO;
            }

            schedule.add(new InstallmentSchedule(
                i,
                firstDueDate.plusMonths(i - 1),
                principalPerInstallment,
                interestAmount,
                installmentAmount,
                remainingBalance
            ));
        }

        return schedule;
    }

    public Money calculateFirstInstallment(Money principal, BigDecimal monthlyRate, int termMonths) {
        BigDecimal rate = monthlyRate.setScale(10, RoundingMode.HALF_EVEN);
        Money principalPerInstallment = Money.of(principal.toBigDecimal()
            .divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_EVEN));
        
        Money interestAmount = Money.of(principal.toBigDecimal()
            .multiply(rate)
            .setScale(2, RoundingMode.HALF_EVEN));
        
        return principalPerInstallment.add(interestAmount);
    }

    public Money calculateLastInstallment(Money principal, BigDecimal monthlyRate, int termMonths) {
        BigDecimal rate = monthlyRate.setScale(10, RoundingMode.HALF_EVEN);
        Money principalPerInstallment = Money.of(principal.toBigDecimal()
            .divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_EVEN));
        
        Money lastPrincipal = principal.subtract(principalPerInstallment.multiply(termMonths - 1));
        Money interestAmount = Money.of(lastPrincipal.toBigDecimal()
            .multiply(rate)
            .setScale(2, RoundingMode.HALF_EVEN));
        
        return lastPrincipal.add(interestAmount);
    }
}
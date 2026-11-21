package com.ibank.credit.calculator;

import com.ibank.core.util.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class SacadorTableCalculator implements AmortizationCalculator {

    @Override
    public List<InstallmentSchedule> calculate(Money principal, BigDecimal monthlyRate, int termMonths, LocalDate firstDueDate) {
        List<InstallmentSchedule> schedule = new ArrayList<>();
        
        BigDecimal rate = monthlyRate.setScale(10, RoundingMode.HALF_EVEN);
        
        // Sacador: amortização crescente (progressão aritmética)
        // Soma dos n primeiros números naturais: n(n+1)/2
        BigDecimal sumOfNumbers = BigDecimal.valueOf(termMonths * (termMonths + 1L) / 2);
        BigDecimal principalPerUnit = principal.toBigDecimal().divide(sumOfNumbers, 10, RoundingMode.HALF_EVEN);
        
        Money remainingBalance = principal;

        for (int i = 1; i <= termMonths; i++) {
            // Amortização crescente: i * principalPerUnit
            Money principalAmount = Money.of(principalPerUnit
                .multiply(BigDecimal.valueOf(i))
                .setScale(2, RoundingMode.HALF_EVEN));
            
            // Adjust last installment
            if (i == termMonths) {
                principalAmount = remainingBalance;
            }
            
            Money interestAmount = Money.of(remainingBalance.toBigDecimal()
                .multiply(rate)
                .setScale(2, RoundingMode.HALF_EVEN));
            
            Money installmentAmount = principalAmount.add(interestAmount);
            remainingBalance = remainingBalance.subtract(principalAmount);
            
            if (remainingBalance.isNegative() || remainingBalance.toBigDecimal().abs().compareTo(new BigDecimal("0.01")) < 0) {
                remainingBalance = Money.ZERO;
            }

            schedule.add(new InstallmentSchedule(
                i,
                firstDueDate.plusMonths(i - 1),
                principalAmount,
                interestAmount,
                installmentAmount,
                remainingBalance
            ));
        }

        return schedule;
    }
}
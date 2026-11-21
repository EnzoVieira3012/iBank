package com.ibank.credit.calculator;

import com.ibank.core.util.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PriceTableCalculator implements AmortizationCalculator {

    @Override
    public List<InstallmentSchedule> calculate(Money principal, BigDecimal monthlyRate, int termMonths, LocalDate firstDueDate) {
        List<InstallmentSchedule> schedule = new ArrayList<>();
        
        BigDecimal rate = monthlyRate.setScale(10, RoundingMode.HALF_EVEN);
        BigDecimal onePlusRate = BigDecimal.ONE.add(rate);
        BigDecimal onePlusRatePowN = onePlusRate.pow(termMonths);
        
        // Price formula: PMT = P * i * (1+i)^n / ((1+i)^n - 1)
        BigDecimal installmentValue = principal.toBigDecimal()
            .multiply(rate)
            .multiply(onePlusRatePowN)
            .divide(onePlusRatePowN.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_EVEN);
        
        Money installmentAmount = Money.of(installmentValue);
        Money remainingBalance = principal;

        for (int i = 1; i <= termMonths; i++) {
            Money interestAmount = Money.of(remainingBalance.toBigDecimal()
                .multiply(rate)
                .setScale(2, RoundingMode.HALF_EVEN));
            
            Money principalAmount = Money.of(installmentAmount.toBigDecimal()
                .subtract(interestAmount.toBigDecimal())
                .setScale(2, RoundingMode.HALF_EVEN));
            
            // Adjust last installment for rounding differences
            if (i == termMonths) {
                principalAmount = remainingBalance;
                installmentAmount = principalAmount.add(interestAmount);
            }
            
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

    public Money calculateInstallment(Money principal, BigDecimal monthlyRate, int termMonths) {
        BigDecimal rate = monthlyRate.setScale(10, RoundingMode.HALF_EVEN);
        BigDecimal onePlusRate = BigDecimal.ONE.add(rate);
        BigDecimal onePlusRatePowN = onePlusRate.pow(termMonths);
        
        BigDecimal installmentValue = principal.toBigDecimal()
            .multiply(rate)
            .multiply(onePlusRatePowN)
            .divide(onePlusRatePowN.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_EVEN);
        
        return Money.of(installmentValue);
    }
}
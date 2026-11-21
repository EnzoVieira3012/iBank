package com.ibank.core.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

@EqualsAndHashCode
public final class Money implements Comparable<Money>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final Money ZERO = new Money(BigDecimal.ZERO);
    public static final Currency BRL = Currency.getInstance("BRL");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    @Getter
    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money of(BigDecimal amount) {
        if (amount == null) return ZERO;
        return new Money(amount);
    }

    public static Money of(String amount) {
        return of(new BigDecimal(amount.replace(",", ".")));
    }

    public static Money of(long amount) {
        return of(BigDecimal.valueOf(amount));
    }

    public static Money of(double amount) {
        return of(BigDecimal.valueOf(amount));
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor));
    }

    public Money multiply(double factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    public Money divide(BigDecimal divisor) {
        return new Money(this.amount.divide(divisor, SCALE, ROUNDING));
    }

    public Money divide(double divisor) {
        return divide(BigDecimal.valueOf(divisor));
    }

    public Money abs() {
        return new Money(this.amount.abs());
    }

    public Money negate() {
        return new Money(this.amount.negate());
    }

    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    public boolean isZero() {
        return BigDecimal.ZERO.compareTo(this.amount) == 0;
    }

    public boolean isPositive() {
        return this.amount.signum() > 0;
    }

    public boolean isNegative() {
        return this.amount.signum() < 0;
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        return this.amount.compareTo(other.amount) <= 0;
    }

    public Money min(Money other) {
        return this.isLessThan(other) ? this : other;
    }

    public Money max(Money other) {
        return this.isGreaterThan(other) ? this : other;
    }

    @Override
    public String toString() {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(amount);
    }

    public String toPlainString() {
        return amount.toPlainString();
    }

    public BigDecimal toBigDecimal() {
        return amount;
    }

    public long toLong() {
        return amount.longValue();
    }
}
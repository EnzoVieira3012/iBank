package com.ibank.credit.calculator;

import com.ibank.credit.model.AmortizationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CalculatorFactory {

    private final PriceTableCalculator priceCalculator;
    private final SACTableCalculator sacCalculator;
    private final SacadorTableCalculator sacadorCalculator;

    public AmortizationCalculator getCalculator(AmortizationType type) {
        return switch (type) {
            case PRICE -> priceCalculator;
            case SAC -> sacCalculator;
            case SACADOR -> sacadorCalculator;
        };
    }
}
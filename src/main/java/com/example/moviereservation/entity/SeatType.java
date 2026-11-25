package com.example.moviereservation.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum SeatType {
    REGULAR(new BigDecimal("1.0")),
    PREMIUM(new BigDecimal("1.5")),
    RECLINER(new BigDecimal("2.0"));

    private final BigDecimal multiplier;

    SeatType(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    public BigDecimal calculatePrice(BigDecimal basePrice) {
        if (basePrice == null) return BigDecimal.ZERO;
        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}

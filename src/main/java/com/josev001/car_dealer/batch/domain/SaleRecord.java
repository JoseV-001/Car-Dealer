package com.josev001.car_dealer.batch.domain;

import java.math.BigDecimal;

public record SaleRecord(
        String dealerId,
        String saleDate,
        String model,
        String paymentType,
        BigDecimal salePriceBrl
) {
}

package com.ashraf.customer.service;

import com.ashraf.commerce.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Tunable order rules, bound from {@code order.pricing.*}. Defaults suit local development. */
@Component
@ConfigurationProperties(prefix = "order.pricing")
@Getter
@Setter
public class OrderPricingProperties {
    private BigDecimal deliveryFee = new BigDecimal("40");
    private BigDecimal freeDeliveryAbove = new BigDecimal("499");
    private BigDecimal taxRate = new BigDecimal("0.05");
    private BigDecimal minOrderAmount = BigDecimal.ZERO;
    private int baseDeliveryMinutes = 20;
    private int maxQuantityPerItem = 20;
    private List<PaymentMethod> allowedPaymentMethods = List.of(PaymentMethod.CASH);
}

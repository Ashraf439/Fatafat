package com.ashraf.customer.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Single source of truth for order money math, shared by quote and place-order. */
@Service
public class OrderPricingService {

    public record PriceBreakdown(BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal taxAmount,
                                 BigDecimal discountAmount, BigDecimal totalAmount) {}

    private final OrderPricingProperties props;

    public OrderPricingService(OrderPricingProperties props) {
        this.props = props;
    }

    public PriceBreakdown price(BigDecimal subtotal) {
        BigDecimal sub = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = sub.compareTo(props.getFreeDeliveryAbove()) >= 0
                ? BigDecimal.ZERO.setScale(2)
                : props.getDeliveryFee().setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = sub.multiply(props.getTaxRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = BigDecimal.ZERO.setScale(2); // hook for coupons later
        BigDecimal total = sub.add(fee).add(tax).subtract(discount);
        return new PriceBreakdown(sub, fee, tax, discount, total);
    }

    public BigDecimal amountForFreeDelivery(BigDecimal subtotal) {
        BigDecimal gap = props.getFreeDeliveryAbove().subtract(subtotal);
        return gap.signum() > 0 ? gap.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }

    public int estimateDeliveryMinutes(int longestPrepMinutes) {
        return props.getBaseDeliveryMinutes() + Math.max(longestPrepMinutes, 0);
    }
}

package com.ashraf.restaurant.onboarding.payment;

public interface PaymentGatewayService {
    OrderResult createOrder(Long restaurantId, Long amount);
}

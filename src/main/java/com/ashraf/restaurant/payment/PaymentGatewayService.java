package com.ashraf.restaurant.payment;

public interface PaymentGatewayService {
    OrderResult createOrder(Long restaurantId, Long amount);
}

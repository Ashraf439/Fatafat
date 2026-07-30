package com.ashraf.restaurant.onboarding.payment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("dev")
public class DummyPaymentGatewayService implements PaymentGatewayService{
    @Override
    public OrderResult createOrder(Long restaurantId, Long amount) {
        String fakeOrderId = "DUMMY_" + UUID.randomUUID();
        return new OrderResult(fakeOrderId, amount);
    }
}

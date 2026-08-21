package com.ashraf.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class RazorpayConfig {
    @Value("${razor.api.key}")
    private String apiKey;
    @Value("${razor.api.secret}")
    private String apiSecret;
    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        return new RazorpayClient(apiKey, apiSecret);
    }
}

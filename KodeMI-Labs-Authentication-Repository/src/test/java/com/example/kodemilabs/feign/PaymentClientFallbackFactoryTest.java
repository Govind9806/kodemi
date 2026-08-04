package com.example.kodemilabs.feign;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaymentClientFallbackFactoryTest {

    private final PaymentClientFallbackFactory factory = new PaymentClientFallbackFactory();

    @Test
    void create_shouldReturnPaymentClient() {
        Throwable cause = new RuntimeException("service down");
        PaymentClient client = factory.create(cause);
        assertNotNull(client);
    }

    @Test
    void create_invokeCreateWallet_shouldNotThrow() {
        Throwable cause = new RuntimeException("service down");
        PaymentClient client = factory.create(cause);
        assertThatNoException().isThrownBy(() -> client.createWallet("Bearer token"));
    }
}

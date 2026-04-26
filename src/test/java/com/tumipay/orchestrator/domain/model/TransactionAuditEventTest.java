package com.tumipay.orchestrator.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionAuditEventTest {

    @Test
    void fromTransaction_shouldCreateEventWithAllFields() {
        // Given
        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .documentType("CC")
                .documentNumber("123456789")
                .build();

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .clientTransactionId("client-123")
                .amountCents(10000L)
                .currencyCode("COP")
                .countryCode("CO")
                .paymentMethodId("NEQUI")
                .webhookUrl("https://example.com/webhook")
                .redirectUrl("https://example.com/redirect")
                .description("Test transaction")
                .expirationSeconds(3600L)
                .status(TransactionStatus.PENDING)
                .processedAt(LocalDateTime.now())
                .customer(customer)
                .build();

        String changedBy = "TransactionService";

        // When
        TransactionAuditEvent event = TransactionAuditEvent.fromTransaction(
                transaction, AuditAction.INSERT, changedBy);

        // Then
        assertThat(event.getTransactionId()).isEqualTo(transaction.getId());
        assertThat(event.getAction()).isEqualTo(AuditAction.INSERT);
        assertThat(event.getClientTransactionId()).isEqualTo("client-123");
        assertThat(event.getAmountCents()).isEqualTo(10000L);
        assertThat(event.getCurrencyCode()).isEqualTo("COP");
        assertThat(event.getCountryCode()).isEqualTo("CO");
        assertThat(event.getPaymentMethodId()).isEqualTo("NEQUI");
        assertThat(event.getWebhookUrl()).isEqualTo("https://example.com/webhook");
        assertThat(event.getRedirectUrl()).isEqualTo("https://example.com/redirect");
        assertThat(event.getDescription()).isEqualTo("Test transaction");
        assertThat(event.getExpirationSeconds()).isEqualTo(3600L);
        assertThat(event.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(event.getProcessedAt()).isEqualTo(transaction.getProcessedAt());
        assertThat(event.getCustomerId()).isEqualTo(customer.getId());
        assertThat(event.getChangedBy()).isEqualTo(changedBy);
        assertThat(event.getChangedAt()).isNotNull();
        assertThat(event.getOldStatus()).isNull();
    }

    @Test
    void fromTransaction_withOldStatus_shouldCreateEventWithOldStatus() {
        // Given
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .clientTransactionId("client-456")
                .amountCents(5000L)
                .currencyCode("USD")
                .countryCode("US")
                .paymentMethodId("PAYPAL")
                .status(TransactionStatus.APPROVED)
                .customer(Customer.builder().id(UUID.randomUUID()).build())
                .build();

        TransactionStatus oldStatus = TransactionStatus.PENDING;

        // When
        TransactionAuditEvent event = TransactionAuditEvent.fromTransaction(
                transaction, AuditAction.UPDATE, "TestService", oldStatus);

        // Then
        assertThat(event.getTransactionId()).isEqualTo(transaction.getId());
        assertThat(event.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(event.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(event.getOldStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void fromTransaction_withNullCustomer_shouldHandleNullCustomerId() {
        // Given
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .clientTransactionId("client-789")
                .amountCents(1000L)
                .currencyCode("EUR")
                .countryCode("DE")
                .paymentMethodId("STRIPE")
                .status(TransactionStatus.REJECTED)
                .customer(null)
                .build();

        // When
        TransactionAuditEvent event = TransactionAuditEvent.fromTransaction(
                transaction, AuditAction.INSERT, "TestService");

        // Then
        assertThat(event.getCustomerId()).isNull();
        assertThat(event.getStatus()).isEqualTo(TransactionStatus.REJECTED);
    }

    @Test
    void builder_shouldCreateEventWithAllFields() {
        // Given
        UUID transactionId = UUID.randomUUID();

        // When
        TransactionAuditEvent event = TransactionAuditEvent.builder()
                .transactionId(transactionId)
                .action(AuditAction.DELETE)
                .clientTransactionId("client-999")
                .amountCents(7500L)
                .status(TransactionStatus.EXPIRED)
                .changedBy("TestService")
                .build();

        // Then
        assertThat(event.getTransactionId()).isEqualTo(transactionId);
        assertThat(event.getAction()).isEqualTo(AuditAction.DELETE);
        assertThat(event.getClientTransactionId()).isEqualTo("client-999");
        assertThat(event.getAmountCents()).isEqualTo(7500L);
        assertThat(event.getStatus()).isEqualTo(TransactionStatus.EXPIRED);
    }
}

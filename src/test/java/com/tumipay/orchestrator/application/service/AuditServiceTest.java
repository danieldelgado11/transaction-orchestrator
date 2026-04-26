package com.tumipay.orchestrator.application.service;

import com.tumipay.orchestrator.domain.model.AuditAction;
import com.tumipay.orchestrator.domain.model.Customer;
import com.tumipay.orchestrator.domain.model.CustomerAuditEvent;
import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.model.TransactionAuditEvent;
import com.tumipay.orchestrator.domain.model.TransactionStatus;
import com.tumipay.orchestrator.domain.port.out.AuditPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AuditService.
 * Verifica la publicación correcta de eventos de auditoría.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditPublisherPort auditPublisher;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditPublisher);
    }

    @Test
    @DisplayName("Debe publicar eventos de auditoría de cliente y transacción en creación sin transacción activa")
    void auditTransactionCreation_withoutActiveTransaction_shouldPublishImmediately() {
        Customer customer = createTestCustomer();

        Transaction transaction = createTestTransaction(customer);

        auditService.auditTransactionCreation(transaction, true);
        verify(auditPublisher).publishCustomerAudit(any(CustomerAuditEvent.class));
        verify(auditPublisher).publishTransactionAudit(any(TransactionAuditEvent.class));
    }

    @Test
    @DisplayName("Debe publicar auditoría de transacción pero omitir cliente cuando cliente es null")
    void auditTransactionCreation_withNullCustomer_shouldSkipCustomerAudit() {
        Transaction transaction = createTestTransaction(null);

        auditService.auditTransactionCreation(transaction, true);

        verify(auditPublisher, never()).publishCustomerAudit(any());
        verify(auditPublisher).publishTransactionAudit(any(TransactionAuditEvent.class));
    }

    @Test
    @DisplayName("Debe publicar auditoría de transacción pero omitir cliente cuando id de cliente es null")
    void auditTransactionCreation_withNullCustomerId_shouldSkipCustomerAudit() {
        Customer customerWithoutId = Customer.builder()
                .id(null)
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();
        Transaction transaction = createTestTransaction(customerWithoutId);

        auditService.auditTransactionCreation(transaction, true);

        verify(auditPublisher, never()).publishCustomerAudit(any());
        verify(auditPublisher).publishTransactionAudit(any(TransactionAuditEvent.class));
    }

    @Test
    @DisplayName("Debe publicar evento de auditoría de actualización de transacción")
    void auditTransactionUpdate_shouldPublishUpdateEvent() {
        Customer customer = createTestCustomer();
        Transaction transaction = createTestTransaction(customer);
        TransactionStatus oldStatus = TransactionStatus.PENDING;

        auditService.auditTransactionUpdate(transaction, oldStatus);

        ArgumentCaptor<TransactionAuditEvent> captor = ArgumentCaptor.forClass(TransactionAuditEvent.class);
        verify(auditPublisher).publishTransactionAudit(captor.capture());

        TransactionAuditEvent capturedEvent = captor.getValue();
        assertThat(capturedEvent.getTransactionId()).isEqualTo(transaction.getId());
        assertThat(capturedEvent.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(capturedEvent.getOldStatus()).isEqualTo(oldStatus);
        assertThat(capturedEvent.getStatus()).isEqualTo(transaction.getStatus());
    }

    @Test
    @DisplayName("Debe manejar excepción de publicador para auditoría de cliente sin lanzar error")
    void auditTransactionCreation_whenCustomerPublisherFails_shouldNotThrow() {
        Customer customer = createTestCustomer();
        Transaction transaction = createTestTransaction(customer);

        doThrow(new RuntimeException("Kafka unavailable"))
                .when(auditPublisher).publishCustomerAudit(any());

        assertThatNoException().isThrownBy(() ->
                auditService.auditTransactionCreation(transaction, true)
        );

        verify(auditPublisher).publishTransactionAudit(any(TransactionAuditEvent.class));
    }

    @Test
    @DisplayName("Debe manejar excepción de publicador para auditoría de transacción sin lanzar error")
    void auditTransactionCreation_whenTransactionPublisherFails_shouldNotThrow() {
        Customer customer = createTestCustomer();
        Transaction transaction = createTestTransaction(customer);

        doThrow(new RuntimeException("Kafka unavailable"))
                .when(auditPublisher).publishTransactionAudit(any());

        assertThatNoException().isThrownBy(() ->
                auditService.auditTransactionCreation(transaction, true)
        );
    }

    @Test
    @DisplayName("Debe manejar excepción de publicador para actualización de transacción sin lanzar error")
    void auditTransactionUpdate_whenPublisherFails_shouldNotThrow() {
        Customer customer = createTestCustomer();
        Transaction transaction = createTestTransaction(customer);

        doThrow(new RuntimeException("Kafka unavailable"))
                .when(auditPublisher).publishTransactionAudit(any());

        assertThatNoException().isThrownBy(() ->
                auditService.auditTransactionUpdate(transaction, TransactionStatus.PENDING)
        );
    }

    @Test
    @DisplayName("Debe crear CustomerAuditEvent correcto con todos los campos")
    void auditTransactionCreation_shouldCreateCorrectCustomerAuditEvent() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(customerId)
                .documentType("CC")
                .documentNumber("1234567890")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("juan@example.com")
                .firstName("Juan")
                .middleName("Carlos")
                .lastName("Perez")
                .secondLastName("Gomez")
                .build();

        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .clientTransactionId("CLIENT-001")
                .amountCents(100000L)
                .currencyCode("COP")
                .countryCode("CO")
                .paymentMethodId("MOCK_PSP")
                .webhookUrl("https://example.com/webhook")
                .redirectUrl("https://example.com/return")
                .description("Test transaction")
                .expirationSeconds(1800L)
                .status(TransactionStatus.PENDING)
                .customer(customer)
                .build();

        auditService.auditTransactionCreation(transaction, true);

        ArgumentCaptor<CustomerAuditEvent> captor = ArgumentCaptor.forClass(CustomerAuditEvent.class);
        verify(auditPublisher).publishCustomerAudit(captor.capture());

        CustomerAuditEvent event = captor.getValue();
        assertThat(event.getCustomerId()).isEqualTo(customerId);
        assertThat(event.getTransactionId()).isEqualTo(transactionId);
        assertThat(event.getAction()).isEqualTo(AuditAction.INSERT);
        assertThat(event.getDocumentType()).isEqualTo("CC");
        assertThat(event.getDocumentNumber()).isEqualTo("1234567890");
        assertThat(event.getEmail()).isEqualTo("juan@example.com");
        assertThat(event.getFirstName()).isEqualTo("Juan");
        assertThat(event.getLastName()).isEqualTo("Perez");
        assertThat(event.getChangedBy()).isEqualTo("TransactionService");
        assertThat(event.getChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("Debe crear TransactionAuditEvent correcto con todos los campos")
    void auditTransactionCreation_shouldCreateCorrectTransactionAuditEvent() {
        UUID transactionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(customerId)
                .documentType("CC")
                .documentNumber("1234567890")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .clientTransactionId("CLIENT-001")
                .amountCents(150000L)
                .currencyCode("COP")
                .countryCode("CO")
                .paymentMethodId("MOCK_PSP")
                .webhookUrl("https://example.com/webhook")
                .redirectUrl("https://example.com/return")
                .description("Test payment")
                .expirationSeconds(1800L)
                .status(TransactionStatus.APPROVED)
                .customer(customer)
                .build();

        auditService.auditTransactionCreation(transaction, true);

        ArgumentCaptor<TransactionAuditEvent> captor = ArgumentCaptor.forClass(TransactionAuditEvent.class);
        verify(auditPublisher).publishTransactionAudit(captor.capture());

        TransactionAuditEvent event = captor.getValue();
        assertThat(event.getTransactionId()).isEqualTo(transactionId);
        assertThat(event.getAction()).isEqualTo(AuditAction.INSERT);
        assertThat(event.getClientTransactionId()).isEqualTo("CLIENT-001");
        assertThat(event.getAmountCents()).isEqualTo(150000L);
        assertThat(event.getCurrencyCode()).isEqualTo("COP");
        assertThat(event.getCountryCode()).isEqualTo("CO");
        assertThat(event.getPaymentMethodId()).isEqualTo("MOCK_PSP");
        assertThat(event.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(event.getCustomerId()).isEqualTo(customerId);
        assertThat(event.getChangedBy()).isEqualTo("TransactionService");
        assertThat(event.getChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("Debe registrar sincronización cuando transacción está activa")
    void auditTransactionCreation_withActiveTransaction_shouldRegisterSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            Customer customer = createTestCustomer();
            Transaction transaction = createTestTransaction(customer);

            auditService.auditTransactionCreation(transaction, true);

            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

            verify(auditPublisher, never()).publishCustomerAudit(any());
            verify(auditPublisher, never()).publishTransactionAudit(any());
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    // Helper methods

    private Customer createTestCustomer() {
        return Customer.builder()
                .id(UUID.randomUUID())
                .documentType("CC")
                .documentNumber("1234567890")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();
    }

    private Transaction createTestTransaction(Customer customer) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .clientTransactionId("CLIENT-001")
                .amountCents(100000L)
                .currencyCode("COP")
                .countryCode("CO")
                .paymentMethodId("MOCK_PSP")
                .webhookUrl("https://example.com/webhook")
                .redirectUrl("https://example.com/return")
                .description("Test transaction")
                .expirationSeconds(1800L)
                .status(TransactionStatus.PENDING)
                .customer(customer)
                .build();
    }
}

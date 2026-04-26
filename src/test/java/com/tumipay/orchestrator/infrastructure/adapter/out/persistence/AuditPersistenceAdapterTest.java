package com.tumipay.orchestrator.infrastructure.adapter.out.persistence;

import com.tumipay.orchestrator.domain.model.*;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository.CustomerAuditJpaRepository;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository.TransactionAuditJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

/**
 * Tests unitarios para AuditPersistenceAdapter.
 * Verifica el guardado de eventos de auditoría de clientes y transacciones,
 * incluyendo validación de campos mapeados y manejo de nulls.
 */
@ExtendWith(MockitoExtension.class)
class AuditPersistenceAdapterTest {

    @Mock
    private CustomerAuditJpaRepository customerAuditRepository;

    @Mock
    private TransactionAuditJpaRepository transactionAuditRepository;

    @InjectMocks
    private AuditPersistenceAdapter adapter;

    @Test
    @DisplayName("Debe guardar evento de auditoría de cliente")
    void saveCustomerAudit_shouldSaveEntity() {
        UUID customerId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        CustomerAuditEvent event = CustomerAuditEvent.builder()
                .customerId(customerId)
                .transactionId(transactionId)
                .action(AuditAction.INSERT)
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .middleName("Carlos")
                .lastName("Perez")
                .secondLastName("Gomez")
                .changedBy("TransactionService")
                .changedAt(now)
                .build();

        adapter.saveCustomerAudit(event);

        verify(customerAuditRepository).save(argThat(entity ->
                entity.getCustomerId().equals(customerId) &&
                        entity.getTransactionId().equals(transactionId) &&
                        entity.getAction().equals("INSERT") &&
                        entity.getDocumentType().equals("CC") &&
                        entity.getDocumentNumber().equals("12345") &&
                        entity.getEmail().equals("test@example.com") &&
                        entity.getChangedBy().equals("TransactionService")
        ));
    }

    @Test
    @DisplayName("Debe guardar evento de auditoría de transacción")
    void saveTransactionAudit_shouldSaveEntity() {
        UUID transactionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        TransactionAuditEvent event = TransactionAuditEvent.builder()
                .transactionId(transactionId)
                .clientTransactionId("CLIENT-001")
                .action(AuditAction.INSERT)
                .amountCents(100000L)
                .currencyCode("COP")
                .countryCode("CO")
                .paymentMethodId("MOCK_PSP")
                .webhookUrl("https://webhook.com")
                .redirectUrl("https://return.com")
                .description("Test")
                .expirationSeconds(1800L)
                .status(TransactionStatus.PENDING)
                .oldStatus(null)
                .customerId(customerId)
                .changedBy("TransactionService")
                .changedAt(now)
                .build();

        adapter.saveTransactionAudit(event);

        verify(transactionAuditRepository).save(argThat(entity ->
                entity.getTransactionId().equals(transactionId) &&
                        entity.getClientTransactionId().equals("CLIENT-001") &&
                        entity.getAction().equals("INSERT") &&
                        entity.getAmountCents().equals(100000L) &&
                        entity.getCurrencyCode().equals("COP") &&
                        entity.getStatus().equals("PENDING") &&
                        entity.getCustomerId().equals(customerId)
        ));
    }

    @Test
    @DisplayName("Debe lanzar NullPointerException para evento de auditoría de cliente null")
    void saveCustomerAudit_nullEvent_shouldThrow() {
        assertThatThrownBy(() -> adapter.saveCustomerAudit(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("CustomerAuditEvent no puede ser null");
    }

    @Test
    @DisplayName("Debe lanzar NullPointerException para evento de auditoría de transacción null")
    void saveTransactionAudit_nullEvent_shouldThrow() {
        assertThatThrownBy(() -> adapter.saveTransactionAudit(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("TransactionAuditEvent no puede ser null");
    }
}

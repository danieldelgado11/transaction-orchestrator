package com.tumipay.orchestrator.infrastructure.adapter.out.persistence;

import com.tumipay.orchestrator.domain.model.Customer;
import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.model.TransactionStatus;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository.JpaCustomerRepository;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository.JpaTransactionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * Tests unitarios para TransactionPersistenceAdapter.
 * Verifica la persistencia de transacciones, actualización de campos,
 * manejo de clientes detached y optimistic locking.
 */
@ExtendWith(MockitoExtension.class)
class TransactionPersistenceAdapterTest {

    @Mock
    private JpaTransactionRepository jpaRepository;

    @Mock
    private JpaCustomerRepository jpaCustomerRepository;

    @Mock
    private TransactionPersistenceMapper mapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TransactionPersistenceAdapter adapter;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        setField(adapter, "entityManager", entityManager);
    }

    @Test
    @DisplayName("Debe guardar transacción nueva con cliente adjunto")
    void save_newTransaction_shouldAttachCustomerAndSave() {
        UUID txId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Customer customer = Customer.builder()
                .id(customerId)
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        Transaction transaction = Transaction.create(
                "CLIENT-001", 100000L, "COP", "CO", "MOCK_PSP",
                "https://webhook.com", "https://return.com", "desc", 1800L, customer
        );

        TransactionEntity entity = new TransactionEntity();
        entity.setId(txId);
        entity.setCustomer(new CustomerEntity());

        CustomerEntity attachedCustomer = new CustomerEntity();
        TransactionEntity savedEntity = new TransactionEntity();
        Transaction savedTransaction = Transaction.builder()
                .id(txId)
                .clientTransactionId("CLIENT-001")
                .amountCents(100000L)
                .currencyCode("COP")
                .countryCode("CO")
                .paymentMethodId("MOCK_PSP")
                .webhookUrl("https://webhook.com")
                .redirectUrl("https://return.com")
                .description("desc")
                .expirationSeconds(1800L)
                .status(TransactionStatus.PENDING)
                .customer(customer)
                .build();

        when(jpaRepository.findById(any())).thenReturn(Optional.empty());
        when(mapper.toEntity(transaction)).thenReturn(entity);
        when(jpaCustomerRepository.findById(any())).thenReturn(Optional.of(attachedCustomer));
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedTransaction);

        Transaction result = adapter.save(transaction);

        assertThat(result.getId()).isEqualTo(txId);
        verify(jpaCustomerRepository).findById(customerId);
        verify(entityManager).flush();
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando cliente no se encuentra para transacción nueva")
    void save_newTransaction_customerNotFound_shouldThrow() {
        UUID txId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Customer customer = Customer.builder()
                .id(customerId)
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        Transaction transaction = Transaction.create(
                "CLIENT-001", 100000L, "COP", "CO", "MOCK_PSP",
                "https://webhook.com", "https://return.com", "desc", 1800L, customer
        );

        TransactionEntity entity = new TransactionEntity();
        entity.setId(txId);

        when(jpaRepository.findById(any())).thenReturn(Optional.empty());
        when(mapper.toEntity(transaction)).thenReturn(entity);
        when(jpaCustomerRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> adapter.save(transaction));
    }

    @Test
    @DisplayName("Debe actualizar campos de transacción existente")
    void save_existingTransaction_shouldUpdateFields() {
        UUID txId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Customer customer = Customer.builder()
                .id(customerId)
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        Transaction transaction = Transaction.builder()
                .id(txId)
                .clientTransactionId("CLIENT-001")
                .amountCents(100000L)
                .currencyCode("COP")
                .countryCode("CO")
                .paymentMethodId("MOCK_PSP")
                .webhookUrl("https://webhook.com")
                .redirectUrl("https://return.com")
                .description("desc")
                .expirationSeconds(1800L)
                .status(TransactionStatus.APPROVED)
                .customer(customer)
                .build();

        TransactionEntity existingEntity = new TransactionEntity();
        existingEntity.setId(txId);
        existingEntity.setStatus("PENDING");
        TransactionEntity savedEntity = new TransactionEntity();
        Transaction savedTransaction = Transaction.builder()
                .id(txId)
                .clientTransactionId("CLIENT-001")
                .amountCents(100000L)
                .currencyCode("COP")
                .countryCode("CO")
                .paymentMethodId("MOCK_PSP")
                .webhookUrl("https://webhook.com")
                .redirectUrl("https://return.com")
                .description("desc")
                .expirationSeconds(1800L)
                .status(TransactionStatus.APPROVED)
                .customer(customer)
                .build();

        when(jpaRepository.findById(txId)).thenReturn(Optional.of(existingEntity));
        when(jpaRepository.save(existingEntity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedTransaction);

        Transaction result = adapter.save(transaction);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(existingEntity.getStatus()).isEqualTo("APPROVED");
        verify(entityManager).flush();
    }

    @Test
    @DisplayName("Debe encontrar transacción por id")
    void findById_existing_shouldReturnTransaction() {
        UUID txId = UUID.randomUUID();
        TransactionEntity entity = new TransactionEntity();
        Transaction transaction = Transaction.builder()
                .id(txId)
                .clientTransactionId("CLIENT-001")
                .amountCents(100000L)
                .currencyCode("COP")
                .countryCode("CO")
                .paymentMethodId("MOCK_PSP")
                .webhookUrl("https://webhook.com")
                .redirectUrl("https://return.com")
                .status(TransactionStatus.PENDING)
                .build();

        when(jpaRepository.findById(txId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(transaction);

        Optional<Transaction> result = adapter.findById(txId);

        assertThat(result).isPresent().hasValue(transaction);
    }

    @Test
    @DisplayName("Debe retornar vacío cuando transacción no se encuentra")
    void findById_notFound_shouldReturnEmpty() {
        UUID txId = UUID.randomUUID();
        when(jpaRepository.findById(txId)).thenReturn(Optional.empty());

        Optional<Transaction> result = adapter.findById(txId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe verificar existencia por id de transacción de cliente")
    void existsByClientTransactionId_shouldReturnTrue() {
        when(jpaRepository.existsByClientTransactionId("CLIENT-001")).thenReturn(true);

        boolean result = adapter.existsByClientTransactionId("CLIENT-001");

        assertThat(result).isTrue();
    }
}

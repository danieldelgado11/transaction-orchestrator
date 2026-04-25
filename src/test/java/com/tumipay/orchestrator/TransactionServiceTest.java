package com.tumipay.orchestrator.application.service;

import com.tumipay.orchestrator.domain.exception.DuplicateTransactionException;
import com.tumipay.orchestrator.domain.exception.PaymentProviderNotFoundException;
import com.tumipay.orchestrator.domain.exception.TransactionNotFoundException;
import com.tumipay.orchestrator.domain.model.Customer;
import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.model.TransactionStatus;
import com.tumipay.orchestrator.domain.port.in.CreateTransactionCommand;
import com.tumipay.orchestrator.domain.port.out.PaymentProviderPort;
import com.tumipay.orchestrator.domain.port.out.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private PaymentProviderPort mockProvider;

    private TransactionService transactionService;

    private CreateTransactionCommand validCommand;

    @BeforeEach
    void setUp() {
        mockProvider = new PaymentProviderPort() {
            @Override public String getSupportedPaymentMethodId() { return "MOCK_PSP"; }
            @Override public TransactionStatus process(Transaction t) { return TransactionStatus.APPROVED; }
        };

        transactionService = new TransactionService(transactionRepository, List.of(mockProvider));

        validCommand = CreateTransactionCommand.builder()
                .clientTransactionId("CLIENT-001")
                .amountCents(100000L)
                .currencyCode("COP")
                .countryCode("CO")
                .paymentMethodId("MOCK_PSP")
                .webhookUrl("https://example.com/webhook")
                .redirectUrl("https://example.com/return")
                .description("Test transaction")
                .customer(Customer.builder()
                        .documentType("CC")
                        .documentNumber("12345")
                        .countryCallingCode("+57")
                        .phoneNumber("3001234567")
                        .email("test@example.com")
                        .firstName("Juan")
                        .lastName("Perez")
                        .build())
                .build();
    }

    @Test
    @DisplayName("Should create and process a transaction successfully")
    void createTransaction_success() {
        when(transactionRepository.existsByClientTransactionId("CLIENT-001")).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionService.createTransaction(validCommand);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(result.getClientTransactionId()).isEqualTo("CLIENT-001");
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateTransactionException when clientTransactionId already exists")
    void createTransaction_duplicate() {
        when(transactionRepository.existsByClientTransactionId("CLIENT-001")).thenReturn(true);

        assertThatThrownBy(() -> transactionService.createTransaction(validCommand))
                .isInstanceOf(DuplicateTransactionException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw PaymentProviderNotFoundException for unknown payment method")
    void createTransaction_unknownProvider() {
        CreateTransactionCommand cmd = CreateTransactionCommand.builder()
                .clientTransactionId("CLIENT-002")
                .amountCents(1000L)
                .currencyCode("COP")
                .countryCode("CO")
                .paymentMethodId("UNKNOWN_PSP")
                .webhookUrl("https://example.com/wh")
                .redirectUrl("https://example.com/r")
                .customer(validCommand.getCustomer())
                .build();

        when(transactionRepository.existsByClientTransactionId(any())).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> transactionService.createTransaction(cmd))
                .isInstanceOf(PaymentProviderNotFoundException.class);
    }

    @Test
    @DisplayName("Should retrieve transaction by id")
    void getTransaction_found() {
        UUID id = UUID.randomUUID();
        Transaction tx = Transaction.create("C1", 1000L, "COP", "CO", "MOCK_PSP",
                "https://wh.com", "https://r.com", null, null, validCommand.getCustomer());

        when(transactionRepository.findById(id)).thenReturn(Optional.of(tx));

        Transaction result = transactionService.getTransaction(id);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should throw TransactionNotFoundException when not found")
    void getTransaction_notFound() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransaction(id))
                .isInstanceOf(TransactionNotFoundException.class);
    }
}

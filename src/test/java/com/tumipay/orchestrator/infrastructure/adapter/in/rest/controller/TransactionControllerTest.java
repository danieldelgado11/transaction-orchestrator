package com.tumipay.orchestrator.infrastructure.adapter.in.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tumipay.orchestrator.domain.model.Customer;
import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.model.TransactionStatus;
import com.tumipay.orchestrator.domain.port.in.CreateTransactionCommand;
import com.tumipay.orchestrator.domain.port.in.TransactionUseCase;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.request.CreateTransactionRequest;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.request.CustomerRequest;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.mapper.TransactionRestMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests unitarios para TransactionController usando MockMvc.
 * Verifica los endpoints REST para creación y consulta de transacciones,
 * incluyendo mapeo de DTOs y códigos de respuesta HTTP.
 */
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionUseCase transactionUseCase;

    @MockBean
    private TransactionRestMapper mapper;

    @Test
    @DisplayName("POST /v1/transactions debe crear transacción y retornar 201")
    void createTransaction_shouldReturn201() throws Exception {
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
                .description("Test")
                .expirationSeconds(1800L)
                .status(TransactionStatus.PENDING)
                .customer(customer)
                .build();

        CreateTransactionCommand command = CreateTransactionCommand.builder()
                .clientTransactionId("CLIENT-001")
                .amountCents(100000L)
                .currencyCode("COP")
                .countryCode("CO")
                .paymentMethodId("MOCK_PSP")
                .webhookUrl("https://webhook.com")
                .redirectUrl("https://return.com")
                .description("Test")
                .expirationSeconds(1800L)
                .customer(customer)
                .build();

        when(mapper.toCommand(any())).thenReturn(command);
        when(transactionUseCase.createTransaction(any())).thenReturn(transaction);

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setClientTransactionId("CLIENT-001");
        request.setAmountCents(100000L);
        request.setCurrencyCode("COP");
        request.setCountryCode("CO");
        request.setPaymentMethodId("MOCK_PSP");
        request.setWebhookUrl("https://webhook.com");
        request.setRedirectUrl("https://return.com");
        request.setDescription("Test");
        request.setExpirationSeconds(1800L);

        CustomerRequest customerReq = new CustomerRequest();
        customerReq.setDocumentType("CC");
        customerReq.setDocumentNumber("12345");
        customerReq.setCountryCallingCode("+57");
        customerReq.setPhoneNumber("3001234567");
        customerReq.setEmail("test@example.com");
        customerReq.setFirstName("Juan");
        customerReq.setLastName("Perez");
        request.setCustomer(customerReq);

        mockMvc.perform(post("/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.response_code").value("000"));
    }

    @Test
    @DisplayName("GET /v1/transactions/{id} debe retornar transacción")
    void getTransaction_shouldReturn200() throws Exception {
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
                .description("Test")
                .expirationSeconds(1800L)
                .status(TransactionStatus.PENDING)
                .customer(customer)
                .build();

        when(transactionUseCase.getTransaction(txId)).thenReturn(transaction);

        mockMvc.perform(get("/v1/transactions/{id}", txId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response_code").value("000"));
    }
}

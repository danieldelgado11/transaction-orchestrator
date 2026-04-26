package com.tumipay.orchestrator.integration;

import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.request.CreateTransactionRequest;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.request.CustomerRequest;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response.TransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para el flujo completo de transacciones.
 * Usa TestContainers para PostgreSQL y levanta el contexto completo de Spring.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@org.springframework.test.context.ActiveProfiles("test")
class TransactionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_orchestrator")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> kafka = new GenericContainer<>("docker.redpanda.com/redpandadata/redpanda:v23.2.14")
            .withExposedPorts(9092)
            .withCommand(
                "redpanda", "start",
                "--kafka-addr", "internal://0.0.0.0:9092,external://0.0.0.0:9093",
                "--advertise-kafka-addr", "internal://localhost:9092,external://localhost:9093",
                "--smp", "1",
                "--memory", "512M",
                "--mode", "dev-container",
                "--default-log-level=warn"
            )
            .waitingFor(Wait.forLogMessage(".*Successfully started Redpanda!.*", 1));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.kafka.bootstrap-servers", () -> kafka.getHost() + ":" + kafka.getMappedPort(9092));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Debe crear transacción y retornar 201 con datos válidos")
    void createTransaction_success() {
        // Given
        CreateTransactionRequest request = createValidTransactionRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateTransactionRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ApiResponse<TransactionResponse>> response = restTemplate.exchange(
                "/v1/transactions",
                HttpMethod.POST,
                entity,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResponseCode()).isEqualTo("000");
    }

    @Test
    @DisplayName("Debe retornar 409 cuando clientTransactionId es duplicado")
    void createTransaction_duplicate() {
        // Given - First transaction
        CreateTransactionRequest request = createValidTransactionRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateTransactionRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.exchange("/v1/transactions", HttpMethod.POST, entity,
                new org.springframework.core.ParameterizedTypeReference<ApiResponse<TransactionResponse>>() {});

        // When - Duplicate transaction
        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                "/v1/transactions",
                HttpMethod.POST,
                entity,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResponseCode()).isEqualTo("002");
    }

    @Test
    @DisplayName("Debe retornar 422 cuando falla validación de request")
    void createTransaction_validationError() {
        // Given - Invalid request (missing required fields)
        CreateTransactionRequest invalidRequest = new CreateTransactionRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateTransactionRequest> entity = new HttpEntity<>(invalidRequest, headers);

        // When
        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                "/v1/transactions",
                HttpMethod.POST,
                entity,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResponseCode()).isEqualTo("001");
    }

    @Test
    @DisplayName("Debe recuperar transacción por id después de creación")
    void getTransaction_afterCreation() {
        // Given - Create transaction first
        CreateTransactionRequest request = createValidTransactionRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateTransactionRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ApiResponse<TransactionResponse>> createResponse = restTemplate.exchange(
                "/v1/transactions",
                HttpMethod.POST,
                entity,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        String transactionId = createResponse.getBody().getData().getTransactionId().toString();

        // When
        ResponseEntity<ApiResponse<TransactionResponse>> getResponse = restTemplate.exchange(
                "/v1/transactions/{id}",
                HttpMethod.GET,
                null,
                new org.springframework.core.ParameterizedTypeReference<>() {},
                transactionId
        );

        // Then
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getData().getClientTransactionId())
                .isEqualTo(request.getClientTransactionId());
    }

    @Test
    @DisplayName("Debe retornar 404 cuando transacción no se encuentra")
    void getTransaction_notFound() {
        // Given
        String nonExistentId = "550e8400-e29b-41d4-a716-446655440000";

        // When
        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                "/v1/transactions/{id}",
                HttpMethod.GET,
                null,
                new org.springframework.core.ParameterizedTypeReference<>() {},
                nonExistentId
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResponseCode()).isEqualTo("003");
    }

    private CreateTransactionRequest createValidTransactionRequest() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setClientTransactionId("INT-TEST-" + System.currentTimeMillis());
        request.setAmountCents(100000L);
        request.setCurrencyCode("COP");
        request.setCountryCode("CO");
        request.setPaymentMethodId("MOCK_PSP");
        request.setWebhookUrl("https://example.com/webhook");
        request.setRedirectUrl("https://example.com/return");
        request.setDescription("Integration test transaction");
        request.setExpirationSeconds(1800L);

        CustomerRequest customer = new CustomerRequest();
        customer.setDocumentType("CC");
        customer.setDocumentNumber("1234567890");
        customer.setCountryCallingCode("+57");
        customer.setPhoneNumber("3001234567");
        customer.setEmail("test@example.com");
        customer.setFirstName("Juan");
        customer.setLastName("Perez");
        request.setCustomer(customer);

        return request;
    }
}

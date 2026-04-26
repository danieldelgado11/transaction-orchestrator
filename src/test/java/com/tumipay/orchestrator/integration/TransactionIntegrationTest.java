package com.tumipay.orchestrator.integration;

import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.request.CreateTransactionRequest;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.request.CustomerRequest;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response.TransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para el flujo completo de transacciones.
 * Usa TestContainers para PostgreSQL y levanta el contexto completo de Spring.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = TransactionIntegrationTest.TestConfig.class)
@org.springframework.test.context.ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionIntegrationTest {

    static class TestConfig implements org.springframework.context.ApplicationContextInitializer<org.springframework.context.ConfigurableApplicationContext> {
        static PostgreSQLContainer<?> postgres;
        static KafkaContainer kafka;

        @Override
        public void initialize(org.springframework.context.ConfigurableApplicationContext context) {
            // Start containers before Spring context loads
            if (postgres == null) {
                postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                        .withDatabaseName("test_orchestrator")
                        .withUsername("test")
                        .withPassword("test");
                postgres.start();
            }
            if (kafka == null) {
                kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
                kafka.start();
            }

            // Set properties after containers are running
            org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "spring.flyway.enabled=true",
                    "spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers()
            );
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @Order(1)
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
    @Order(2)
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
    @Order(3)
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
    @Order(4)
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
    @Order(5)
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

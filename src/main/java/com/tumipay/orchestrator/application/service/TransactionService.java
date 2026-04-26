package com.tumipay.orchestrator.application.service;

import com.tumipay.orchestrator.domain.exception.DuplicateTransactionException;
import com.tumipay.orchestrator.domain.exception.PaymentProviderNotFoundException;
import com.tumipay.orchestrator.domain.exception.TransactionNotFoundException;
import com.tumipay.orchestrator.domain.model.Customer;
import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.model.TransactionStatus;
import com.tumipay.orchestrator.domain.port.in.AuditUseCase;
import com.tumipay.orchestrator.domain.port.in.CreateTransactionCommand;
import com.tumipay.orchestrator.domain.port.in.TransactionUseCase;
import com.tumipay.orchestrator.domain.port.out.CircuitBreakerPort;
import com.tumipay.orchestrator.domain.port.out.CustomerRepository;
import com.tumipay.orchestrator.domain.port.out.PaymentProviderPort;
import com.tumipay.orchestrator.domain.port.out.TransactionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de Aplicación — orquesta el flujo de transacciones.
 *
 * Decisiones de diseño:
 * - Implementa el puerto de entrada (TransactionUseCase).
 * - Depende exclusivamente de puertos de salida (interfaces), nunca de adaptadores concretos.
 * - Los proveedores de pago se resuelven en tiempo de ejecución mediante un Mapa construido
 *   desde todos los adaptadores registrados (patrón Strategy + Registry), permitiendo
 *   extensión sin modificar código para nuevos PSPs.
 * - La transacción se persiste ANTES de enviarla al proveedor, garantizando
 *   semántica de entrega al-menos-una-vez y trazabilidad completa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService implements TransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final AuditUseCase auditUseCase;
    private final List<PaymentProviderPort> paymentProviders;
    private final CircuitBreakerPort circuitBreaker;

    private Map<String, PaymentProviderPort> providerRegistry;

    @PostConstruct
    void initializeProviderRegistry() {
        this.providerRegistry = paymentProviders.stream()
                .collect(Collectors.toMap(
                        PaymentProviderPort::getSupportedPaymentMethodId,
                        Function.identity()
                ));
    }

    @Override
    @Transactional
    public Transaction createTransaction(CreateTransactionCommand command) {
        log.info("Creando transacción para clientTransactionId={}", command.getClientTransactionId());

        validateIdempotency(command.getClientTransactionId());

        // Build transaction and get customer creation status
        var customerResult = resolveCustomer(command.getCustomer());
        Transaction transaction = buildTransaction(command, customerResult.customer());

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transacción persistida con id={}", saved.getId());

        // Pass isNewCustomer flag to audit only if customer was actually created
        auditUseCase.auditTransactionCreation(saved, customerResult.isNew());

        Transaction finalTransaction = processWithProvider(saved, command.getPaymentMethodId());

        log.info("Transacción id={} procesada con status={}", finalTransaction.getId(), finalTransaction.getStatus());
        return finalTransaction;
    }

    private CustomerRepository.CustomerResult resolveCustomer(Customer customer) {
        var result = customerRepository.findOrCreate(customer);
        log.debug("Cliente resuelto: id={}, isNew={}, document={}/{}",
                result.customer().getId(),
                result.isNew(),
                result.customer().getDocumentType(),
                result.customer().getDocumentNumber());
        return result;
    }

    private void validateIdempotency(String clientTransactionId) {
        if (transactionRepository.existsByClientTransactionId(clientTransactionId)) {
            throw new DuplicateTransactionException(clientTransactionId);
        }
    }

    private Transaction buildTransaction(CreateTransactionCommand command, Customer customer) {
        return Transaction.create(
                command.getClientTransactionId(),
                command.getAmountCents(),
                command.getCurrencyCode(),
                command.getCountryCode(),
                command.getPaymentMethodId(),
                command.getWebhookUrl(),
                command.getRedirectUrl(),
                command.getDescription(),
                command.getExpirationSeconds(),
                customer
        );
    }

    private Transaction processWithProvider(Transaction saved, String paymentMethodId) {
        PaymentProviderPort provider = resolveProvider(paymentMethodId);
        TransactionStatus resultStatus = circuitBreaker.executeWithCircuitBreaker(provider, saved);

        Transaction updated = saved.withStatus(resultStatus);
        Transaction finalTransaction = transactionRepository.save(updated);

        if (resultStatus != saved.getStatus()) {
            auditUseCase.auditTransactionUpdate(finalTransaction, saved.getStatus());
        }

        return finalTransaction;
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId.toString()));
    }

    private PaymentProviderPort resolveProvider(String paymentMethodId) {
        PaymentProviderPort provider = providerRegistry.get(paymentMethodId);
        if (provider == null) {
            throw new PaymentProviderNotFoundException(paymentMethodId);
        }
        return provider;
    }

}

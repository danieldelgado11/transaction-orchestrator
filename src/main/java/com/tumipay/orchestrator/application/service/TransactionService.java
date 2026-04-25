package com.tumipay.orchestrator.application.service;

import com.tumipay.orchestrator.domain.exception.DuplicateTransactionException;
import com.tumipay.orchestrator.domain.exception.PaymentProviderNotFoundException;
import com.tumipay.orchestrator.domain.exception.TransactionNotFoundException;
import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.model.TransactionStatus;
import com.tumipay.orchestrator.domain.port.in.CreateTransactionCommand;
import com.tumipay.orchestrator.domain.port.in.TransactionUseCase;
import com.tumipay.orchestrator.domain.port.out.PaymentProviderPort;
import com.tumipay.orchestrator.domain.port.out.TransactionRepository;
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
    private final List<PaymentProviderPort> paymentProviders;

    /**
     * Registro construido perezosamente: paymentMethodId → adaptador de proveedor.
     * Usar un Mapa en lugar de cadenas if/else hace que agregar proveedores sea O(1).
     */
    private Map<String, PaymentProviderPort> providerRegistry;

    @Override
    @Transactional
    public Transaction createTransaction(CreateTransactionCommand command) {
        log.info("Creando transacción para clientTransactionId={}", command.getClientTransactionId());

        // Guardia de idempotencia
        if (transactionRepository.existsByClientTransactionId(command.getClientTransactionId())) {
            throw new DuplicateTransactionException(command.getClientTransactionId());
        }

        // Construir objeto de dominio
        Transaction transaction = Transaction.create(
                command.getClientTransactionId(),
                command.getAmountCents(),
                command.getCurrencyCode(),
                command.getCountryCode(),
                command.getPaymentMethodId(),
                command.getWebhookUrl(),
                command.getRedirectUrl(),
                command.getDescription(),
                command.getExpirationSeconds(),
                command.getCustomer()
        );

        // Persistir ANTES de enviar al proveedor (auditoría + garantía al-menos-una-vez)
        Transaction saved = transactionRepository.save(transaction);
        log.info("Transacción persistida con id={}", saved.getId());

        // Enrutar al adaptador de proveedor correcto (patrón Strategy)
        PaymentProviderPort provider = resolveProvider(command.getPaymentMethodId());
        TransactionStatus resultStatus = provider.process(saved);

        // Actualizar estado después de la respuesta del proveedor
        Transaction updated = saved.withStatus(resultStatus);
        Transaction finalTransaction = transactionRepository.save(updated);

        log.info("Transacción id={} procesada con status={}", finalTransaction.getId(), resultStatus);
        return finalTransaction;
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId.toString()));
    }

    private PaymentProviderPort resolveProvider(String paymentMethodId) {
        if (providerRegistry == null) {
            providerRegistry = paymentProviders.stream()
                    .collect(Collectors.toMap(
                            PaymentProviderPort::getSupportedPaymentMethodId,
                            Function.identity()
                    ));
        }
        PaymentProviderPort provider = providerRegistry.get(paymentMethodId);
        if (provider == null) {
            throw new PaymentProviderNotFoundException(paymentMethodId);
        }
        return provider;
    }
}

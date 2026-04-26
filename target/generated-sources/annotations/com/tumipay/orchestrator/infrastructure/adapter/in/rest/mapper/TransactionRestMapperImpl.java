package com.tumipay.orchestrator.infrastructure.adapter.in.rest.mapper;

import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.domain.port.in.CreateTransactionCommand;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.request.CreateTransactionRequest;
import com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.response.TransactionResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-26T12:53:52-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class TransactionRestMapperImpl implements TransactionRestMapper {

    @Override
    public CreateTransactionCommand toCommand(CreateTransactionRequest request) {
        if ( request == null ) {
            return null;
        }

        CreateTransactionCommand.CreateTransactionCommandBuilder createTransactionCommand = CreateTransactionCommand.builder();

        createTransactionCommand.clientTransactionId( request.getClientTransactionId() );
        createTransactionCommand.amountCents( request.getAmountCents() );
        createTransactionCommand.currencyCode( request.getCurrencyCode() );
        createTransactionCommand.countryCode( request.getCountryCode() );
        createTransactionCommand.paymentMethodId( request.getPaymentMethodId() );
        createTransactionCommand.webhookUrl( request.getWebhookUrl() );
        createTransactionCommand.redirectUrl( request.getRedirectUrl() );
        createTransactionCommand.description( request.getDescription() );
        createTransactionCommand.expirationSeconds( request.getExpirationSeconds() );

        createTransactionCommand.customer( toCustomer(request.getCustomer()) );

        return createTransactionCommand.build();
    }

    @Override
    public TransactionResponse toResponse(Transaction transaction) {
        if ( transaction == null ) {
            return null;
        }

        TransactionResponse.TransactionResponseBuilder transactionResponse = TransactionResponse.builder();

        transactionResponse.transactionId( transaction.getId() );
        transactionResponse.processedAt( transaction.getProcessedAt() );
        transactionResponse.clientTransactionId( transaction.getClientTransactionId() );
        transactionResponse.paymentMethodId( transaction.getPaymentMethodId() );
        transactionResponse.currencyCode( transaction.getCurrencyCode() );
        transactionResponse.countryCode( transaction.getCountryCode() );
        transactionResponse.description( transaction.getDescription() );

        transactionResponse.status( transaction.getStatus().name() );

        return transactionResponse.build();
    }
}

package com.tumipay.orchestrator.infrastructure.adapter.out.persistence.mapper;

import com.tumipay.orchestrator.domain.model.Customer;
import com.tumipay.orchestrator.domain.model.Transaction;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-24T20:28:14-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class TransactionPersistenceMapperImpl implements TransactionPersistenceMapper {

    @Override
    public TransactionEntity toEntity(Transaction transaction) {
        if ( transaction == null ) {
            return null;
        }

        TransactionEntity.TransactionEntityBuilder transactionEntity = TransactionEntity.builder();

        transactionEntity.id( transaction.getId() );
        transactionEntity.clientTransactionId( transaction.getClientTransactionId() );
        transactionEntity.amountCents( transaction.getAmountCents() );
        transactionEntity.currencyCode( transaction.getCurrencyCode() );
        transactionEntity.countryCode( transaction.getCountryCode() );
        transactionEntity.paymentMethodId( transaction.getPaymentMethodId() );
        transactionEntity.webhookUrl( transaction.getWebhookUrl() );
        transactionEntity.redirectUrl( transaction.getRedirectUrl() );
        transactionEntity.description( transaction.getDescription() );
        transactionEntity.expirationSeconds( transaction.getExpirationSeconds() );
        transactionEntity.processedAt( transaction.getProcessedAt() );
        transactionEntity.createdAt( transaction.getCreatedAt() );

        transactionEntity.status( transaction.getStatus().name() );
        transactionEntity.customer( toCustomerEntity(transaction.getCustomer()) );

        return transactionEntity.build();
    }

    @Override
    public Transaction toDomain(TransactionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Transaction.TransactionBuilder transaction = Transaction.builder();

        transaction.id( entity.getId() );
        transaction.clientTransactionId( entity.getClientTransactionId() );
        transaction.amountCents( entity.getAmountCents() );
        transaction.currencyCode( entity.getCurrencyCode() );
        transaction.countryCode( entity.getCountryCode() );
        transaction.paymentMethodId( entity.getPaymentMethodId() );
        transaction.webhookUrl( entity.getWebhookUrl() );
        transaction.redirectUrl( entity.getRedirectUrl() );
        transaction.description( entity.getDescription() );
        transaction.expirationSeconds( entity.getExpirationSeconds() );
        transaction.processedAt( entity.getProcessedAt() );
        transaction.createdAt( entity.getCreatedAt() );

        transaction.status( com.tumipay.orchestrator.domain.model.TransactionStatus.valueOf(entity.getStatus()) );
        transaction.customer( toCustomer(entity.getCustomer()) );

        return transaction.build();
    }

    @Override
    public CustomerEntity toCustomerEntity(Customer customer) {
        if ( customer == null ) {
            return null;
        }

        CustomerEntity.CustomerEntityBuilder customerEntity = CustomerEntity.builder();

        customerEntity.documentType( customer.getDocumentType() );
        customerEntity.documentNumber( customer.getDocumentNumber() );
        customerEntity.countryCallingCode( customer.getCountryCallingCode() );
        customerEntity.phoneNumber( customer.getPhoneNumber() );
        customerEntity.email( customer.getEmail() );
        customerEntity.firstName( customer.getFirstName() );
        customerEntity.middleName( customer.getMiddleName() );
        customerEntity.lastName( customer.getLastName() );
        customerEntity.secondLastName( customer.getSecondLastName() );

        return customerEntity.build();
    }

    @Override
    public Customer toCustomer(CustomerEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Customer.CustomerBuilder customer = Customer.builder();

        customer.documentType( entity.getDocumentType() );
        customer.documentNumber( entity.getDocumentNumber() );
        customer.countryCallingCode( entity.getCountryCallingCode() );
        customer.phoneNumber( entity.getPhoneNumber() );
        customer.email( entity.getEmail() );
        customer.firstName( entity.getFirstName() );
        customer.middleName( entity.getMiddleName() );
        customer.lastName( entity.getLastName() );
        customer.secondLastName( entity.getSecondLastName() );

        return customer.build();
    }
}

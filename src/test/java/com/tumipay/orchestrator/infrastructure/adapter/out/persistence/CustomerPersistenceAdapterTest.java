package com.tumipay.orchestrator.infrastructure.adapter.out.persistence;

import com.tumipay.orchestrator.domain.model.Customer;
import com.tumipay.orchestrator.domain.port.out.CustomerRepository;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.tumipay.orchestrator.infrastructure.adapter.out.persistence.repository.JpaCustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para CustomerPersistenceAdapter.
 * Verifica la persistencia de clientes, búsqueda por documento
 * y la lógica de findOrCreate para idempotencia.
 */
@ExtendWith(MockitoExtension.class)
class CustomerPersistenceAdapterTest {

    @Mock
    private JpaCustomerRepository jpaRepository;

    @Mock
    private TransactionPersistenceMapper mapper;

    @InjectMocks
    private CustomerPersistenceAdapter adapter;

    @Test
    @DisplayName("Debe guardar cliente nuevo y retornar resultado mapeado")
    void save_newCustomer_shouldReturnSavedCustomer() {
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(null)
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        CustomerEntity entity = new CustomerEntity();
        CustomerEntity savedEntity = new CustomerEntity();
        Customer mappedCustomer = Customer.builder()
                .id(id)
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        when(mapper.toCustomerEntity(customer)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toCustomer(savedEntity)).thenReturn(mappedCustomer);

        Customer result = adapter.save(customer);

        assertThat(result.getId()).isEqualTo(id);
        verify(jpaRepository).save(entity);
    }

    @Test
    @DisplayName("Debe encontrar cliente por id")
    void findById_existingCustomer_shouldReturnCustomer() {
        UUID id = UUID.randomUUID();
        CustomerEntity entity = new CustomerEntity();
        Customer customer = Customer.builder()
                .id(id)
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toCustomer(entity)).thenReturn(customer);

        Optional<Customer> result = adapter.findById(id);

        assertThat(result).isPresent().hasValue(customer);
    }

    @Test
    @DisplayName("Debe retornar Optional vacío cuando cliente no se encuentra por id")
    void findById_notFound_shouldReturnEmpty() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Customer> result = adapter.findById(id);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe encontrar cliente por tipo y número de documento")
    void findByDocument_existing_shouldReturnCustomer() {
        CustomerEntity entity = new CustomerEntity();
        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("test@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        when(jpaRepository.findByDocumentTypeAndDocumentNumber("CC", "12345"))
                .thenReturn(Optional.of(entity));
        when(mapper.toCustomer(entity)).thenReturn(customer);

        Optional<Customer> result = adapter.findByDocument("CC", "12345");

        assertThat(result).isPresent().hasValue(customer);
    }

    @Test
    @DisplayName("Debe retornar vacío cuando documento no se encuentra")
    void findByDocument_notFound_shouldReturnEmpty() {
        when(jpaRepository.findByDocumentTypeAndDocumentNumber("CC", "99999"))
                .thenReturn(Optional.empty());

        Optional<Customer> result = adapter.findByDocument("CC", "99999");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findOrCreate debe retornar cliente existente por documento y no marcar como nuevo")
    void findOrCreate_existingByDocument_shouldReturnExistingNotNew() {
        UUID existingId = UUID.randomUUID();
        CustomerEntity existingEntity = new CustomerEntity();
        Customer existingCustomer = Customer.builder()
                .id(existingId)
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("old@example.com")
                .firstName("Juan")
                .lastName("Perez")
                .build();

        Customer newData = Customer.builder()
                .id(null)
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+1")
                .phoneNumber("5551234")
                .email("new@example.com")
                .firstName("Juan Carlos")
                .middleName("Alberto")
                .lastName("Perez")
                .secondLastName("Gomez")
                .build();

        when(jpaRepository.findByDocumentTypeAndDocumentNumber("CC", "12345"))
                .thenReturn(Optional.of(existingEntity));
        when(mapper.toCustomer(existingEntity)).thenReturn(existingCustomer);

        CustomerEntity updatedEntity = new CustomerEntity();
        CustomerEntity savedUpdatedEntity = new CustomerEntity();
        Customer savedUpdatedCustomer = Customer.builder()
                .id(existingId)
                .documentType("CC")
                .documentNumber("12345")
                .countryCallingCode("+1")
                .phoneNumber("5551234")
                .email("new@example.com")
                .firstName("Juan Carlos")
                .middleName("Alberto")
                .lastName("Perez")
                .secondLastName("Gomez")
                .build();

        when(mapper.toCustomerEntity(any(Customer.class))).thenReturn(updatedEntity);
        when(jpaRepository.save(updatedEntity)).thenReturn(savedUpdatedEntity);
        when(mapper.toCustomer(savedUpdatedEntity)).thenReturn(savedUpdatedCustomer);

        CustomerRepository.CustomerResult result = adapter.findOrCreate(newData);

        assertThat(result.isNew()).isFalse();
        assertThat(result.customer().getId()).isEqualTo(existingId);
        assertThat(result.customer().getEmail()).isEqualTo("new@example.com");
        assertThat(result.customer().getPhoneNumber()).isEqualTo("5551234");
    }

    @Test
    @DisplayName("findOrCreate debe crear cliente nuevo cuando documento no se encuentra")
    void findOrCreate_newCustomer_shouldCreateAndMarkAsNew() {
        UUID newId = UUID.randomUUID();
        Customer newData = Customer.builder()
                .id(null)
                .documentType("CC")
                .documentNumber("NEW-123")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("new@example.com")
                .firstName("Pedro")
                .lastName("Lopez")
                .build();

        when(jpaRepository.findByDocumentTypeAndDocumentNumber("CC", "NEW-123"))
                .thenReturn(Optional.empty());

        CustomerEntity entity = new CustomerEntity();
        CustomerEntity savedEntity = new CustomerEntity();
        Customer savedCustomer = Customer.builder()
                .id(newId)
                .documentType("CC")
                .documentNumber("NEW-123")
                .countryCallingCode("+57")
                .phoneNumber("3001234567")
                .email("new@example.com")
                .firstName("Pedro")
                .lastName("Lopez")
                .build();

        when(mapper.toCustomerEntity(newData)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toCustomer(savedEntity)).thenReturn(savedCustomer);

        CustomerRepository.CustomerResult result = adapter.findOrCreate(newData);

        assertThat(result.isNew()).isTrue();
        assertThat(result.customer().getId()).isEqualTo(newId);
    }
}

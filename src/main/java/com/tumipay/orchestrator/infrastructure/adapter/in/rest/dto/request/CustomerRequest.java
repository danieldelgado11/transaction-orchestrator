package com.tumipay.orchestrator.infrastructure.adapter.in.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerRequest {

    @NotBlank(message = "customer.document_type es obligatorio")
    @JsonProperty("document_type")
    private String documentType;

    @NotBlank(message = "customer.document_number es obligatorio")
    @JsonProperty("document_number")
    private String documentNumber;

    @NotBlank(message = "customer.country_calling_code es obligatorio")
    @Pattern(regexp = "^\\+\\d{1,4}$", message = "country_calling_code debe estar en formato +XX o +XXX")
    @JsonProperty("country_calling_code")
    private String countryCallingCode;

    @NotBlank(message = "customer.phone_number es obligatorio")
    @Pattern(regexp = "^\\d{5,15}$", message = "phone_number debe contener solo dígitos (5-15)")
    @JsonProperty("phone_number")
    private String phoneNumber;

    @NotBlank(message = "customer.email es obligatorio")
    @Email(message = "customer.email debe ser una dirección de correo válida")
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "customer.first_name es obligatorio")
    @Size(max = 100)
    @JsonProperty("first_name")
    private String firstName;

    @Size(max = 100)
    @JsonProperty("middle_name")
    private String middleName;

    @NotBlank(message = "customer.last_name es obligatorio")
    @Size(max = 100)
    @JsonProperty("last_name")
    private String lastName;

    @Size(max = 100)
    @JsonProperty("second_last_name")
    private String secondLastName;
}

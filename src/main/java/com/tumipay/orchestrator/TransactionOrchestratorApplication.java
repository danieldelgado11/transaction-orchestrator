package com.tumipay.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de la aplicación Spring Boot.
 * Punto de entrada para el microservicio de orquestación de transacciones.
 */
@SpringBootApplication
public class TransactionOrchestratorApplication {

    /**
     * Método principal que inicia la aplicación Spring Boot.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(TransactionOrchestratorApplication.class, args);
    }
}

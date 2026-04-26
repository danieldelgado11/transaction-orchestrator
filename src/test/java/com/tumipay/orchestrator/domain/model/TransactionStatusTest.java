package com.tumipay.orchestrator.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionStatusTest {

    @Test
    void enum_shouldContainAllValues() {
        // When & Then
        assertThat(TransactionStatus.values()).containsExactly(
                TransactionStatus.PENDING,
                TransactionStatus.PROCESSING,
                TransactionStatus.APPROVED,
                TransactionStatus.REJECTED,
                TransactionStatus.EXPIRED,
                TransactionStatus.FAILED,
                TransactionStatus.REVERSED
        );
    }

    @Test
    void enum_shouldHaveCorrectNumberOfValues() {
        // When & Then
        assertThat(TransactionStatus.values()).hasSize(7);
    }

    @Test
    void valueOf_shouldReturnCorrectEnum() {
        // When & Then
        assertThat(TransactionStatus.valueOf("PENDING")).isEqualTo(TransactionStatus.PENDING);
        assertThat(TransactionStatus.valueOf("PROCESSING")).isEqualTo(TransactionStatus.PROCESSING);
        assertThat(TransactionStatus.valueOf("APPROVED")).isEqualTo(TransactionStatus.APPROVED);
        assertThat(TransactionStatus.valueOf("REJECTED")).isEqualTo(TransactionStatus.REJECTED);
        assertThat(TransactionStatus.valueOf("EXPIRED")).isEqualTo(TransactionStatus.EXPIRED);
        assertThat(TransactionStatus.valueOf("FAILED")).isEqualTo(TransactionStatus.FAILED);
        assertThat(TransactionStatus.valueOf("REVERSED")).isEqualTo(TransactionStatus.REVERSED);
    }

    @Test
    void enum_shouldBeUsedInTransaction() {
        // Given
        TransactionStatus pending = TransactionStatus.PENDING;
        TransactionStatus approved = TransactionStatus.APPROVED;
        TransactionStatus rejected = TransactionStatus.REJECTED;

        // Then
        assertThat(pending.name()).isEqualTo("PENDING");
        assertThat(approved.name()).isEqualTo("APPROVED");
        assertThat(rejected.name()).isEqualTo("REJECTED");
    }
}

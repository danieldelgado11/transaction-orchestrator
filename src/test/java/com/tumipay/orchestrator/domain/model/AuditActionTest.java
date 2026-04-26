package com.tumipay.orchestrator.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditActionTest {

    @Test
    void enum_shouldContainAllValues() {
        // When & Then
        assertThat(AuditAction.values()).containsExactly(
                AuditAction.INSERT,
                AuditAction.UPDATE,
                AuditAction.DELETE
        );
    }

    @Test
    void enum_shouldHaveCorrectNumberOfValues() {
        // When & Then
        assertThat(AuditAction.values()).hasSize(3);
    }

    @Test
    void valueOf_shouldReturnCorrectEnum() {
        // When & Then
        assertThat(AuditAction.valueOf("INSERT")).isEqualTo(AuditAction.INSERT);
        assertThat(AuditAction.valueOf("UPDATE")).isEqualTo(AuditAction.UPDATE);
        assertThat(AuditAction.valueOf("DELETE")).isEqualTo(AuditAction.DELETE);
    }

    @Test
    void enum_shouldBeUsedInEvents() {
        // Given
        AuditAction insert = AuditAction.INSERT;
        AuditAction update = AuditAction.UPDATE;
        AuditAction delete = AuditAction.DELETE;

        // Then
        assertThat(insert.name()).isEqualTo("INSERT");
        assertThat(update.name()).isEqualTo("UPDATE");
        assertThat(delete.name()).isEqualTo("DELETE");
    }
}

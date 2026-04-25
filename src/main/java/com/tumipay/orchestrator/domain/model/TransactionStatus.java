package com.tumipay.orchestrator.domain.model;

public enum TransactionStatus {
    PENDING,
    PROCESSING,
    APPROVED,
    REJECTED,
    EXPIRED,
    FAILED,
    REVERSED
}

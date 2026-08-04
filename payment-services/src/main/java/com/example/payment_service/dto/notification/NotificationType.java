package com.example.payment_service.dto.notification;

public enum NotificationType {
    PAYMENT_SUCCESSFUL,
    PAYMENT_FAILED,
    REFUND_INITIATED,
    REFUND_COMPLETED,
    PAYOUT_REQUESTED,
    PAYOUT_PROCESSED,
    PAYOUT_REJECTED,
    PAYOUT_HOLD,
    WALLET_CREDITED,
    WALLET_TOPPED_UP
}

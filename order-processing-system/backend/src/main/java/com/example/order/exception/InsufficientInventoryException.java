package com.example.order.exception;

/**
 * Thrown when an order cannot be fulfilled because one or more
 * requested items don't have enough available stock.
 */
public class InsufficientInventoryException extends RuntimeException {

    private final Long productId;
    private final int requested;
    private final int available;

    public InsufficientInventoryException(Long productId, int requested, int available) {
        super(String.format(
                "Insufficient inventory for product %d: requested %d, available %d",
                productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public Long getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}

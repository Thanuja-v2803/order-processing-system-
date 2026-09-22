package com.example.order.service;

import com.example.order.entity.Inventory;
import com.example.order.exception.InsufficientInventoryException;
import com.example.order.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    /**
     * Reserves stock for a single product. Runs in its own transaction
     * with a pessimistic lock on the inventory row so concurrent order
     * processing threads can't oversell the same product.
     */
    @Transactional
    public void reserve(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InsufficientInventoryException(productId, quantity, 0));

        if (!inventory.hasSufficientStock(quantity)) {
            throw new InsufficientInventoryException(
                    productId, quantity, inventory.getAvailableQuantity());
        }

        inventory.reserve(quantity);
        inventoryRepository.save(inventory);
    }

    /**
     * Releases previously reserved stock, e.g. when a later item in the
     * same order fails validation and the whole order is rolled back.
     */
    @Transactional
    public void release(Long productId, int quantity) {
        inventoryRepository.findByProductIdForUpdate(productId)
                .ifPresent(inventory -> {
                    inventory.release(quantity);
                    inventoryRepository.save(inventory);
                });
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(Long productId, int quantity) {
        return inventoryRepository.findByProductId(productId)
                .map(inv -> inv.hasSufficientStock(quantity))
                .orElse(false);
    }
}

package com.example.order.service;

import com.example.order.config.RabbitMQConfig;
import com.example.order.dto.OrderEvent;
import com.example.order.entity.Order;
import com.example.order.exception.InsufficientInventoryException;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumes queued order-processing jobs. Each order is picked up off
 * RabbitMQ, its items are reserved against inventory one by one (rolling
 * back any partial reservation on failure), and the outcome is persisted
 * and pushed to subscribed clients over WebSocket. Listener concurrency
 * is what actually parallelizes processing; AsyncConfig's executor is
 * available for any additional fire-and-forget work triggered from here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE, concurrency = "3-10")
    public void handleOrderEvent(OrderEvent event) {
        Long orderId = event.getOrderId();
        log.info("Picked up order {} for processing", orderId);

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order {} no longer exists, skipping", orderId);
            return;
        }

        updateStatus(order, Order.OrderStatus.PROCESSING, "Reserving inventory");

        List<Order.OrderItem> reserved = new ArrayList<>();
        try {
            for (Order.OrderItem item : order.getItems()) {
                inventoryService.reserve(item.getProductId(), item.getQuantity());
                reserved.add(item);
            }
            updateStatus(order, Order.OrderStatus.CONFIRMED, "Order confirmed");
        } catch (InsufficientInventoryException ex) {
            log.warn("Order {} failed: {}", orderId, ex.getMessage());
            rollbackReservations(reserved);
            updateStatus(order, Order.OrderStatus.FAILED, ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error processing order {}", orderId, ex);
            rollbackReservations(reserved);
            updateStatus(order, Order.OrderStatus.FAILED, "Unexpected processing error");
        }
    }

    private void rollbackReservations(List<Order.OrderItem> reserved) {
        for (Order.OrderItem item : reserved) {
            inventoryService.release(item.getProductId(), item.getQuantity());
        }
    }

    // Note: deliberately not @Transactional here - it's a single
    // repository.save() (already transactional via Spring Data's own
    // proxy) followed by a non-transactional broadcast. Wrapping both in
    // one transaction on a private/protected method would silently not
    // apply anyway, since Spring's proxy-based AOP can't intercept
    // self-invocation within the same class.
    private void updateStatus(Order order, Order.OrderStatus status, String message) {
        order.setStatus(status);
        orderRepository.save(order);
        broadcast(OrderEvent.of(order.getId(), status, message));
    }

    private void broadcast(OrderEvent event) {
        messagingTemplate.convertAndSend("/topic/orders/" + event.getOrderId(), event);
        messagingTemplate.convertAndSend("/topic/orders", event);
    }
}

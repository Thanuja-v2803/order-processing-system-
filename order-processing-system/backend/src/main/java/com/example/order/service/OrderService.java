package com.example.order.service;

import com.example.order.config.RabbitMQConfig;
import com.example.order.dto.OrderEvent;
import com.example.order.dto.OrderRequest;
import com.example.order.entity.Inventory;
import com.example.order.entity.Order;
import com.example.order.repository.InventoryRepository;
import com.example.order.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Persists the order as PENDING and hands it off to the processing
     * queue. Returns immediately - inventory reservation and final
     * status happen asynchronously via OrderProcessingService, with
     * updates pushed over WebSocket.
     */
    @Transactional
    public Order createOrder(OrderRequest request) {
        List<Order.OrderItem> items = request.getItems().stream()
                .map(this::toOrderItem)
                .collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .customerName(request.getCustomerName())
                .items(items)
                .totalAmount(total)
                .status(Order.OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_ROUTING_KEY,
                OrderEvent.of(saved.getId(), Order.OrderStatus.PENDING, "Order received"));

        return saved;
    }

    private Order.OrderItem toOrderItem(OrderRequest.OrderItemRequest itemRequest) {
        Inventory inventory = inventoryRepository.findByProductId(itemRequest.getProductId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Unknown product: " + itemRequest.getProductId()));

        return Order.OrderItem.builder()
                .productId(inventory.getProductId())
                .productName(inventory.getProductName())
                .quantity(itemRequest.getQuantity())
                .unitPrice(BigDecimal.TEN) // placeholder: wire up a real pricing source
                .build();
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> listOrders() {
        return orderRepository.findAll();
    }
}

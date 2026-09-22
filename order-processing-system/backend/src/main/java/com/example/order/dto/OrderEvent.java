package com.example.order.dto;

import com.example.order.entity.Order;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Message shape used both as the RabbitMQ payload (queued for async
 * processing) and as the WebSocket broadcast payload (pushed to
 * subscribed clients once processing reaches a new state).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent implements Serializable {

    private Long orderId;
    private Order.OrderStatus status;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public static OrderEvent of(Long orderId, Order.OrderStatus status, String message) {
        return OrderEvent.builder()
                .orderId(orderId)
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

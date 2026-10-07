package com.example.checkout.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.example.checkout.entity.OrderEntity;
import com.example.checkout.entity.OrderItem;
import com.example.checkout.model.CartItem;
import com.example.checkout.model.Checkout;
import com.example.checkout.repository.OrderRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    @Inject
    OrderRepository orderRepository;

    @Transactional(TxType.REQUIRES_NEW)
    public Checkout createOrder(Checkout checkout) {
        OrderEntity order = new OrderEntity();
        order.orderNumber = nextOrderNumber();
        order.customerId = checkout.getCustomerId();
        order.requestId = IdempotencyService.normalize(checkout.getRequestId());
        order.items = checkout.getItems().stream().map(OrderItem::from).collect(Collectors.toList());
        order.itemCount = checkout.getItems().stream().mapToInt(CartItem::getQuantity).sum();
        order.subtotal = checkout.getSubtotal();
        order.shippingFee = checkout.getShippingFee();
        order.discount = checkout.getDiscount();
        order.total = checkout.getTotal();
        order.paymentMethod = checkout.getPaymentMethod();
        order.paymentReference = checkout.getPaymentReference();
        order.createdAt = Instant.now();
        orderRepository.persist(order);

        checkout.setOrderNumber(order.orderNumber);
        LOG.infof("[order] created %s for customer %s", order.orderNumber, order.customerId);
        return checkout;
    }

    private String nextOrderNumber() {
        return "ORD-" + LocalDate.now(ZoneOffset.UTC).format(DATE)
                + "-" + String.format("%04d", orderRepository.nextOrderSequence());
    }
}

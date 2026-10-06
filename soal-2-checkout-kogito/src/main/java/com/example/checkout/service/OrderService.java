package com.example.checkout.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.jboss.logging.Logger;

import com.example.checkout.model.CartItem;
import com.example.checkout.model.Checkout;
import com.example.checkout.persistence.OrderEntity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    static final String SEQUENCE = "order_number_seq";

    @Inject
    EntityManager em;

    @Transactional
    public Checkout createOrder(Checkout checkout) {
        OrderEntity order = new OrderEntity();
        order.orderNumber = nextOrderNumber();
        order.customerId = checkout.getCustomerId();
        order.itemCount = checkout.getItems().stream().mapToInt(CartItem::getQuantity).sum();
        order.subtotal = checkout.getSubtotal();
        order.shippingFee = checkout.getShippingFee();
        order.discount = checkout.getDiscount();
        order.total = checkout.getTotal();
        order.paymentMethod = checkout.getPaymentMethod();
        order.paymentReference = checkout.getPaymentReference();
        order.createdAt = Instant.now();
        order.persist();

        checkout.setOrderNumber(order.orderNumber);
        LOG.infof("[order] created %s for customer %s", order.orderNumber, order.customerId);
        return checkout;
    }

    private String nextOrderNumber() {
        return "ORD-" + LocalDate.now(ZoneOffset.UTC).format(DATE)
                + "-" + String.format("%04d", nextSequenceValue());
    }

    private long nextSequenceValue() {
        String sql = em.unwrap(Session.class).getSessionFactory().unwrap(SessionFactoryImplementor.class)
                .getJdbcServices().getDialect().getSequenceSupport().getSequenceNextValString(SEQUENCE);
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }
}

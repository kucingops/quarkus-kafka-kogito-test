package com.example.checkout.repository;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import com.example.checkout.entity.OrderEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderRepository implements PanacheRepository<OrderEntity> {

    private static final String ORDER_NUMBER_SEQUENCE = "order_number_seq";

    public List<OrderEntity> findLatest() {
        return listAll(Sort.descending("createdAt"));
    }

    public OrderEntity findByOrderNumber(String orderNumber) {
        return find("orderNumber", orderNumber).firstResult();
    }

    public OrderEntity findByRequest(String customerId, String requestId) {
        return find("customerId = ?1 and requestId = ?2", customerId, requestId).firstResult();
    }

    public long nextOrderSequence() {
        String sql = getEntityManager().unwrap(Session.class).getSessionFactory()
                .unwrap(SessionFactoryImplementor.class)
                .getJdbcServices().getDialect().getSequenceSupport()
                .getSequenceNextValString(ORDER_NUMBER_SEQUENCE);
        return ((Number) getEntityManager().createNativeQuery(sql).getSingleResult()).longValue();
    }
}

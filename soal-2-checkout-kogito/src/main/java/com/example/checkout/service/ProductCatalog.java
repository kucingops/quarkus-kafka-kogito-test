package com.example.checkout.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.example.checkout.persistence.ProductEntity;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Sort;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class ProductCatalog {

    public record Product(String sku, String name, long price, int stock) {
    }

    private static final List<Product> SEED = List.of(
            new Product("SKU-001", "Kaos Polos", 75_000, 10),
            new Product("SKU-002", "Celana Jeans", 250_000, 5),
            new Product("SKU-003", "Sepatu Sneakers", 650_000, 3),
            new Product("SKU-004", "Laptop Gaming", 15_000_000, 2),
            new Product("SKU-005", "Topi Bucket", 50_000, 0));

    @Inject
    EntityManager em;

    @Transactional
    void seedOnStartup(@Observes StartupEvent event) {
        if (ProductEntity.count() == 0) {
            insertSeed();
        }
    }

    @Transactional
    public void reset() {
        ProductEntity.deleteAll();
        insertSeed();
    }

    private void insertSeed() {
        for (Product p : SEED) {
            ProductEntity entity = new ProductEntity();
            entity.sku = p.sku();
            entity.name = p.name();
            entity.price = p.price();
            entity.stock = p.stock();
            entity.persist();
        }
    }

    @Transactional
    public Optional<Product> find(String sku) {
        if (sku == null) {
            return Optional.empty();
        }
        return ProductEntity.<ProductEntity>findByIdOptional(sku).map(ProductCatalog::toProduct);
    }

    @Transactional
    public Collection<Product> all() {
        return ProductEntity.<ProductEntity>listAll(Sort.ascending("sku")).stream()
                .map(ProductCatalog::toProduct)
                .toList();
    }

    @Transactional
    public int stockOf(String sku) {
        return em.createQuery("select p.stock from ProductEntity p where p.sku = :sku", Integer.class)
                .setParameter("sku", sku)
                .getResultStream()
                .findFirst()
                .orElse(0);
    }

    @Transactional(TxType.REQUIRES_NEW)
    public String tryReserve(Map<String, Integer> quantities) {
        for (Map.Entry<String, Integer> e : new TreeMap<>(quantities).entrySet()) {
            int updated = ProductEntity.update("stock = stock - ?1 where sku = ?2 and stock >= ?1",
                    e.getValue(), e.getKey());
            if (updated == 0) {
                String error = "insufficient stock for " + e.getKey() + " (requested " + e.getValue()
                        + ", available " + stockOf(e.getKey()) + ")";
                QuarkusTransaction.setRollbackOnly();
                return error;
            }
        }
        return null;
    }

    @Transactional(TxType.REQUIRES_NEW)
    public void release(Map<String, Integer> quantities) {
        new TreeMap<>(quantities).forEach((sku, qty) ->
                ProductEntity.update("stock = stock + ?1 where sku = ?2", qty, sku));
    }

    private static Product toProduct(ProductEntity entity) {
        return new Product(entity.sku, entity.name, entity.price, entity.stock);
    }
}

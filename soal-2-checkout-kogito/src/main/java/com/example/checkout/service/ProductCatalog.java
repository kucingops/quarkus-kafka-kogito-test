package com.example.checkout.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.example.checkout.entity.ProductEntity;
import com.example.checkout.repository.ProductRepository;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
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
    ProductRepository productRepository;

    @Transactional
    void seedOnStartup(@Observes StartupEvent event) {
        if (productRepository.count() == 0) {
            insertSeed();
        }
    }

    @Transactional
    public void reset() {
        productRepository.deleteAll();
        insertSeed();
    }

    private void insertSeed() {
        for (Product p : SEED) {
            ProductEntity entity = new ProductEntity();
            entity.sku = p.sku();
            entity.name = p.name();
            entity.price = p.price();
            entity.stock = p.stock();
            productRepository.persist(entity);
        }
    }

    @Transactional
    public Optional<Product> find(String sku) {
        if (sku == null) {
            return Optional.empty();
        }
        return productRepository.findByIdOptional(sku).map(ProductCatalog::toProduct);
    }

    @Transactional
    public Collection<Product> all() {
        return productRepository.findAllSortedBySku().stream()
                .map(ProductCatalog::toProduct)
                .toList();
    }

    @Transactional
    public int stockOf(String sku) {
        return productRepository.stockOf(sku);
    }

    @Transactional(TxType.REQUIRES_NEW)
    public String tryReserve(Map<String, Integer> quantities) {
        for (Map.Entry<String, Integer> e : new TreeMap<>(quantities).entrySet()) {
            if (!productRepository.decreaseStock(e.getKey(), e.getValue())) {
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
                productRepository.increaseStock(sku, qty));
    }

    private static Product toProduct(ProductEntity entity) {
        return new Product(entity.sku, entity.name, entity.price, entity.stock);
    }
}

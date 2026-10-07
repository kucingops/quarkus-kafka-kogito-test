package com.example.checkout.repository;

import java.util.List;

import com.example.checkout.entity.ProductEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductRepository implements PanacheRepositoryBase<ProductEntity, String> {

    public List<ProductEntity> findAllSortedBySku() {
        return listAll(Sort.ascending("sku"));
    }

    public int stockOf(String sku) {
        return getEntityManager()
                .createQuery("select p.stock from ProductEntity p where p.sku = :sku", Integer.class)
                .setParameter("sku", sku)
                .getResultStream()
                .findFirst()
                .orElse(0);
    }

    public boolean decreaseStock(String sku, int quantity) {
        return update("stock = stock - ?1 where sku = ?2 and stock >= ?1", quantity, sku) > 0;
    }

    public void increaseStock(String sku, int quantity) {
        update("stock = stock + ?1 where sku = ?2", quantity, sku);
    }
}

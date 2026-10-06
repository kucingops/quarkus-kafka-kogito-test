package com.example.checkout.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class ProductEntity extends PanacheEntityBase {

    @Id
    public String sku;

    @Column(nullable = false)
    public String name;

    public long price;

    public int stock;
}

package com.example.checkout.api;

import java.util.Collection;
import java.util.List;

import com.example.checkout.persistence.OrderEntity;
import com.example.checkout.service.ProductCatalog;
import com.example.checkout.service.ProductCatalog.Product;

import io.quarkus.panache.common.Sort;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class StoreResource {

    @Inject
    ProductCatalog catalog;

    @GET
    @Path("products")
    public Collection<Product> products() {
        return catalog.all();
    }

    @GET
    @Path("orders")
    public List<OrderEntity> orders() {
        return OrderEntity.listAll(Sort.descending("createdAt"));
    }

    @GET
    @Path("orders/{orderNumber}")
    public OrderEntity order(@PathParam("orderNumber") String orderNumber) {
        OrderEntity order = OrderEntity.findByOrderNumber(orderNumber);
        if (order == null) {
            throw new NotFoundException("Order " + orderNumber + " not found");
        }
        return order;
    }
}

package com.example.checkout.controller;

import java.util.Collection;
import java.util.List;

import com.example.checkout.entity.OrderEntity;
import com.example.checkout.repository.OrderRepository;
import com.example.checkout.service.ProductCatalog.Product;
import com.example.checkout.service.ProductCatalog;

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

    @Inject
    OrderRepository orderRepository;

    @GET
    @Path("products")
    public Collection<Product> products() {
        return catalog.all();
    }

    @GET
    @Path("orders")
    public List<OrderEntity> orders() {
        return orderRepository.findLatest();
    }

    @GET
    @Path("orders/{orderNumber}")
    public OrderEntity order(@PathParam("orderNumber") String orderNumber) {
        OrderEntity order = orderRepository.findByOrderNumber(orderNumber);
        if (order == null) {
            throw new NotFoundException("Order " + orderNumber + " not found");
        }
        return order;
    }
}

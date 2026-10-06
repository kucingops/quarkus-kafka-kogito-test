package com.example.transactions.api;

import java.util.List;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import com.example.transactions.model.AmountCategory;
import com.example.transactions.persistence.CategorySummary;
import com.example.transactions.persistence.TransactionEntity;
import com.example.transactions.persistence.TransactionRepository;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/transactions")
@Produces(MediaType.APPLICATION_JSON)
public class TransactionResource {

    @Inject
    TransactionRepository repository;

    @Inject
    @Channel("transactions-raw-out")
    Emitter<String> rawEmitter;

    @POST
    @Path("/publish")
    @Consumes(MediaType.WILDCARD)
    public Response publish(String payload) {
        rawEmitter.send(payload).toCompletableFuture().join();
        return Response.accepted().build();
    }

    @GET
    public List<TransactionEntity> list(@QueryParam("category") AmountCategory category,
            @QueryParam("highRisk") Boolean highRisk) {
        return repository.findLatest(category, highRisk);
    }

    @GET
    @Path("/summary")
    public List<CategorySummary> summary() {
        return repository.summarizeByCategory();
    }

    @GET
    @Path("/{id}")
    public TransactionEntity get(@PathParam("id") String id) {
        TransactionEntity entity = repository.findById(id);
        if (entity == null) {
            throw new NotFoundException("Transaction " + id + " not found");
        }
        return entity;
    }
}

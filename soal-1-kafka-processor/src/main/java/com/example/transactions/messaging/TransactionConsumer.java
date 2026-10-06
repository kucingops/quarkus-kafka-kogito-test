package com.example.transactions.messaging;

import java.time.Instant;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.example.transactions.model.EnrichedTransaction;
import com.example.transactions.model.RejectedTransaction;
import com.example.transactions.service.TransactionProcessor;
import com.example.transactions.service.TransactionProcessor.ProcessingResult;

import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TransactionConsumer {

    private static final Logger LOG = Logger.getLogger(TransactionConsumer.class);

    @Inject
    TransactionProcessor processor;

    @Inject
    @Channel("transactions-enriched-out")
    Emitter<EnrichedTransaction> enrichedEmitter;

    @Inject
    @Channel("transactions-dlq-out")
    Emitter<RejectedTransaction> dlqEmitter;

    @Incoming("transactions-in")
    @Blocking
    public void consume(String payload) {
        ProcessingResult result = processor.process(payload);

        switch (result.status()) {
            case PROCESSED -> {
                enrichedEmitter.send(result.transaction()).toCompletableFuture().join();
                LOG.infof("Processed %s -> %s (%s IDR, highRisk=%s)",
                        result.transaction().transactionId(),
                        result.transaction().category(),
                        result.transaction().amountIdr(),
                        result.transaction().highRisk());
            }
            case DUPLICATE -> LOG.debugf("Duplicate ignored: %s", result.transaction().transactionId());
            case REJECTED -> {
                dlqEmitter.send(new RejectedTransaction(payload, result.reason(), Instant.now()))
                        .toCompletableFuture().join();
                LOG.warnf("Rejected message: %s", result.reason());
            }
        }
    }
}

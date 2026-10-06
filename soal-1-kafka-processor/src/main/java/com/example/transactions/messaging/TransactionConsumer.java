package com.example.transactions.messaging;

import java.time.Instant;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.example.transactions.model.EnrichedTransaction;
import com.example.transactions.model.RejectedTransaction;
import com.example.transactions.service.TransactionProcessor;
import com.example.transactions.service.TransactionProcessor.ProcessingResult;

import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TransactionConsumer {

    private static final Logger LOG = Logger.getLogger(TransactionConsumer.class);

    @Inject
    TransactionProcessor processor;

    @Inject
    @Channel("transactions-enriched-out")
    MutinyEmitter<EnrichedTransaction> enrichedEmitter;

    @Inject
    @Channel("transactions-dlq-out")
    MutinyEmitter<RejectedTransaction> dlqEmitter;

    @Incoming("transactions-in")
    @Blocking
    public void consume(String payload) {
        ProcessingResult result;
        try {
            result = processor.process(payload);
        } catch (RuntimeException e) {
            LOG.errorf(e, "Unexpected error while processing message");
            reject(payload, "processing error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
            return;
        }

        switch (result.status()) {
            case PROCESSED -> publish(result.transaction());
            case DUPLICATE -> LOG.debugf("Duplicate ignored: %s", result.transaction().transactionId());
            case REJECTED -> reject(payload, result.reason());
        }
    }

    private void publish(EnrichedTransaction tx) {
        enrichedEmitter.sendMessageAndAwait(KafkaRecord.of(tx.transactionId(), tx));
        LOG.infof("Processed %s -> %s (%s IDR, highRisk=%s)",
                tx.transactionId(), tx.category(), tx.amountIdr(), tx.highRisk());
    }

    private void reject(String payload, String reason) {
        dlqEmitter.sendAndAwait(new RejectedTransaction(payload, reason, Instant.now()));
        LOG.warnf("Rejected message: %s", reason);
    }
}

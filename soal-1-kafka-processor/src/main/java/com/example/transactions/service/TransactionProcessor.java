package com.example.transactions.service;

import org.jboss.logging.Logger;

import com.example.transactions.model.EnrichedTransaction;
import com.example.transactions.model.RawTransaction;
import com.example.transactions.persistence.TransactionEntity;
import com.example.transactions.persistence.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TransactionProcessor {

    private static final Logger LOG = Logger.getLogger(TransactionProcessor.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TransactionTransformer transformer;

    @Inject
    TransactionRepository repository;

    @Transactional
    public ProcessingResult process(String payload) {
        RawTransaction raw;
        try {
            raw = objectMapper.readValue(payload, RawTransaction.class);
        } catch (JsonProcessingException e) {
            return ProcessingResult.rejected("malformed JSON: " + e.getOriginalMessage());
        }

        EnrichedTransaction enriched;
        try {
            enriched = transformer.transform(raw);
        } catch (InvalidTransactionException e) {
            return ProcessingResult.rejected(e.getMessage());
        }

        if (repository.findById(enriched.transactionId()) != null) {
            LOG.infof("Skip duplicate transaction %s", enriched.transactionId());
            return ProcessingResult.duplicate(enriched);
        }

        repository.persist(TransactionEntity.from(enriched));
        return ProcessingResult.processed(enriched);
    }

    public record ProcessingResult(Status status, EnrichedTransaction transaction, String reason) {

        public enum Status {
            PROCESSED, DUPLICATE, REJECTED
        }

        static ProcessingResult processed(EnrichedTransaction tx) {
            return new ProcessingResult(Status.PROCESSED, tx, null);
        }

        static ProcessingResult duplicate(EnrichedTransaction tx) {
            return new ProcessingResult(Status.DUPLICATE, tx, "duplicate transactionId");
        }

        static ProcessingResult rejected(String reason) {
            return new ProcessingResult(Status.REJECTED, null, reason);
        }
    }
}

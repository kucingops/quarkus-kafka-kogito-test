package com.example.transactions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.transactions.model.AmountCategory;
import com.example.transactions.model.EnrichedTransaction;
import com.example.transactions.model.RawTransaction;
import com.example.transactions.persistence.TransactionEntity;
import com.example.transactions.persistence.TransactionRepository;
import com.example.transactions.service.TransactionProcessor.ProcessingResult;
import com.example.transactions.service.TransactionProcessor.ProcessingResult.Status;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {

    private static final String PAYLOAD = """
            {"transactionId":"TRX-1","customerId":"CUST-1","merchant":"toko",
             "amount":75000,"currency":"IDR","timestamp":"2026-10-01T08:15:00Z"}
            """;

    private static final EnrichedTransaction TX = new EnrichedTransaction(
            "TRX-1", "CUST-1", null, "TOKO", new BigDecimal("75000"), "IDR", new BigDecimal("75000"),
            null, AmountCategory.SMALL, false, Instant.parse("2026-10-01T08:15:00Z"), Instant.now());

    @Spy
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    TransactionTransformer transformer;

    @Mock
    TransactionRepository repository;

    @InjectMocks
    TransactionProcessor processor;

    @Test
    void validTransactionIsPersisted() {
        when(transformer.transform(any(RawTransaction.class))).thenReturn(TX);
        when(repository.findById("TRX-1")).thenReturn(null);

        ProcessingResult result = processor.process(PAYLOAD);

        assertEquals(Status.PROCESSED, result.status());
        assertSame(TX, result.transaction());
        ArgumentCaptor<TransactionEntity> saved = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(repository).persist(saved.capture());
        assertEquals("TRX-1", saved.getValue().transactionId);
        assertEquals("TOKO", saved.getValue().merchant);
    }

    @Test
    void duplicateTransactionIsNotPersistedAgain() {
        when(transformer.transform(any(RawTransaction.class))).thenReturn(TX);
        when(repository.findById("TRX-1")).thenReturn(new TransactionEntity());

        ProcessingResult result = processor.process(PAYLOAD);

        assertEquals(Status.DUPLICATE, result.status());
        verify(repository, never()).persist(any(TransactionEntity.class));
    }

    @Test
    void invalidTransactionIsRejectedWithReason() {
        when(transformer.transform(any(RawTransaction.class)))
                .thenThrow(new InvalidTransactionException("amount must be greater than 0"));

        ProcessingResult result = processor.process(PAYLOAD);

        assertEquals(Status.REJECTED, result.status());
        assertEquals("amount must be greater than 0", result.reason());
        verifyNoInteractions(repository);
    }

    @Test
    void malformedJsonIsRejected() {
        ProcessingResult result = processor.process("{\"transactionId\":\"TRX-1\", \"not valid json\"");

        assertEquals(Status.REJECTED, result.status());
        assertTrue(result.reason().startsWith("malformed JSON"));
        verifyNoInteractions(transformer, repository);
    }
}

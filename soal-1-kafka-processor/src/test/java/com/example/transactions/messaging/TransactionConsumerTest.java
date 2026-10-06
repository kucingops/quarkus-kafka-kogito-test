package com.example.transactions.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.transactions.model.AmountCategory;
import com.example.transactions.model.EnrichedTransaction;
import com.example.transactions.model.RejectedTransaction;
import com.example.transactions.service.TransactionProcessor;
import com.example.transactions.service.TransactionProcessor.ProcessingResult;
import com.example.transactions.service.TransactionProcessor.ProcessingResult.Status;

@ExtendWith(MockitoExtension.class)
class TransactionConsumerTest {

    private static final EnrichedTransaction TX = new EnrichedTransaction(
            "TRX-1", "CUST-1", null, "TOKO", new BigDecimal("75000"), "IDR", new BigDecimal("75000"),
            "COD", AmountCategory.SMALL, false, Instant.now(), Instant.now());

    @Mock
    TransactionProcessor processor;

    @Mock
    Emitter<EnrichedTransaction> enrichedEmitter;

    @Mock
    Emitter<RejectedTransaction> dlqEmitter;

    @InjectMocks
    TransactionConsumer consumer;

    @Test
    void processedTransactionIsSentToEnrichedTopic() {
        when(processor.process("payload")).thenReturn(new ProcessingResult(Status.PROCESSED, TX, null));
        when(enrichedEmitter.send(TX)).thenReturn(CompletableFuture.completedFuture(null));

        consumer.consume("payload");

        verify(enrichedEmitter).send(TX);
        verify(dlqEmitter, never()).send(any(RejectedTransaction.class));
    }

    @Test
    void rejectedTransactionIsSentToDlqWithReason() {
        when(processor.process("bad"))
                .thenReturn(new ProcessingResult(Status.REJECTED, null, "amount must be greater than 0"));
        when(dlqEmitter.send(any(RejectedTransaction.class))).thenReturn(CompletableFuture.completedFuture(null));

        consumer.consume("bad");

        ArgumentCaptor<RejectedTransaction> sent = ArgumentCaptor.forClass(RejectedTransaction.class);
        verify(dlqEmitter).send(sent.capture());
        assertEquals("bad", sent.getValue().rawPayload());
        assertEquals("amount must be greater than 0", sent.getValue().reason());
        verify(enrichedEmitter, never()).send(any(EnrichedTransaction.class));
    }

    @Test
    void duplicateTransactionIsNotSentAnywhere() {
        when(processor.process("dup"))
                .thenReturn(new ProcessingResult(Status.DUPLICATE, TX, "duplicate transactionId"));

        consumer.consume("dup");

        verify(enrichedEmitter, never()).send(any(EnrichedTransaction.class));
        verify(dlqEmitter, never()).send(any(RejectedTransaction.class));
    }
}

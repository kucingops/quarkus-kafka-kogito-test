package com.example.transactions.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Consumer;

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

import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

@ExtendWith(MockitoExtension.class)
class TransactionConsumerTest {

    private static final EnrichedTransaction TX = new EnrichedTransaction(
            "TRX-1", "CUST-1", null, "TOKO", new BigDecimal("75000"), "IDR", new BigDecimal("75000"),
            "COD", AmountCategory.SMALL, false, Instant.now(), Instant.now());

    @Mock
    TransactionProcessor processor;

    @Mock
    MutinyEmitter<EnrichedTransaction> enrichedEmitter;

    @Mock
    MutinyEmitter<RejectedTransaction> dlqEmitter;

    @InjectMocks
    TransactionConsumer consumer;

    @Test
    @SuppressWarnings("unchecked")
    void processedTransactionIsSentToEnrichedTopicKeyedByTransactionId() {
        when(processor.process(eq("payload"), any())).thenAnswer(inv -> {
            Consumer<EnrichedTransaction> beforeCommit = inv.getArgument(1);
            beforeCommit.accept(TX);
            return new ProcessingResult(Status.PROCESSED, TX, null);
        });

        consumer.consume("payload");

        ArgumentCaptor<KafkaRecord<String, EnrichedTransaction>> sent = ArgumentCaptor.forClass(KafkaRecord.class);
        verify(enrichedEmitter).sendMessageAndAwait(sent.capture());
        assertEquals("TRX-1", sent.getValue().getKey());
        assertSame(TX, sent.getValue().getPayload());
        verifyNoInteractions(dlqEmitter);
    }

    @Test
    void rejectedTransactionIsSentToDlqWithReason() {
        when(processor.process(eq("bad"), any()))
                .thenReturn(new ProcessingResult(Status.REJECTED, null, "amount must be greater than 0"));

        consumer.consume("bad");

        ArgumentCaptor<RejectedTransaction> sent = ArgumentCaptor.forClass(RejectedTransaction.class);
        verify(dlqEmitter).sendAndAwait(sent.capture());
        assertEquals("bad", sent.getValue().rawPayload());
        assertEquals("amount must be greater than 0", sent.getValue().reason());
        verifyNoInteractions(enrichedEmitter);
    }

    @Test
    void unexpectedProcessingErrorIsSentToDlqInsteadOfStoppingTheConsumer() {
        when(processor.process(eq("boom"), any())).thenThrow(new IllegalStateException("database unavailable"));

        consumer.consume("boom");

        ArgumentCaptor<RejectedTransaction> sent = ArgumentCaptor.forClass(RejectedTransaction.class);
        verify(dlqEmitter).sendAndAwait(sent.capture());
        assertEquals("boom", sent.getValue().rawPayload());
        assertEquals("processing error: database unavailable", sent.getValue().reason());
        verifyNoInteractions(enrichedEmitter);
    }

    @Test
    void duplicateTransactionIsNotSentAnywhere() {
        when(processor.process(eq("dup"), any()))
                .thenReturn(new ProcessingResult(Status.DUPLICATE, TX, "duplicate transactionId"));

        consumer.consume("dup");

        verifyNoInteractions(enrichedEmitter, dlqEmitter);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishFailureIsPropagatedSoTheMessageIsRetriedInsteadOfGoingToDlq() {
        when(processor.process(eq("payload"), any())).thenAnswer(inv -> {
            Consumer<EnrichedTransaction> beforeCommit = inv.getArgument(1);
            beforeCommit.accept(TX);
            return new ProcessingResult(Status.PROCESSED, TX, null);
        });
        doThrow(new IllegalStateException("broker unavailable"))
                .when(enrichedEmitter).sendMessageAndAwait(any(KafkaRecord.class));

        assertThrows(TransactionConsumer.PublishFailedException.class, () -> consumer.consume("payload"));

        verifyNoInteractions(dlqEmitter);
    }
}

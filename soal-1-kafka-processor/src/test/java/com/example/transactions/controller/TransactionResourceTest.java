package com.example.transactions.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.transactions.model.AmountCategory;
import com.example.transactions.persistence.TransactionEntity;
import com.example.transactions.repository.TransactionRepository;

import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class TransactionResourceTest {

    @Mock
    TransactionRepository repository;

    @Mock
    Emitter<String> rawEmitter;

    @InjectMocks
    TransactionResource resource;

    @Test
    void publishSendsRawPayloadToInputTopic() {
        when(rawEmitter.send("{\"transactionId\":\"TRX-X\"}")).thenReturn(CompletableFuture.completedFuture(null));

        assertEquals(202, resource.publish("{\"transactionId\":\"TRX-X\"}").getStatus());

        verify(rawEmitter).send("{\"transactionId\":\"TRX-X\"}");
    }

    @Test
    void listPassesFiltersToRepository() {
        List<TransactionEntity> stored = List.of(new TransactionEntity());
        when(repository.findLatest(AmountCategory.LARGE, true)).thenReturn(stored);

        assertSame(stored, resource.list(AmountCategory.LARGE, true));
    }

    @Test
    void getReturnsStoredTransaction() {
        TransactionEntity entity = new TransactionEntity();
        when(repository.findById("TRX-1")).thenReturn(entity);

        assertSame(entity, resource.get("TRX-1"));
    }

    @Test
    void getUnknownTransactionThrowsNotFound() {
        when(repository.findById("UNKNOWN")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> resource.get("UNKNOWN"));
    }
}

package com.example.transactions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.example.transactions.persistence.TransactionEntity;
import com.example.transactions.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(value = KafkaContainerResource.class, restrictToAnnotatedClass = true)
class KafkaContainerIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrapServers;

    @Inject
    TransactionRepository repository;

    @Inject
    ObjectMapper mapper;

    @Test
    void validTransactionIsInsertedAndProducedToKafka() throws Exception {
        String id = "TRX-" + UUID.randomUUID();
        send("""
                {"transactionId":"%s","customerId":"CUST-03","customerEmail":"andi.w@outlook.com",
                 "merchant":"elektronik   sejahtera","amount":1250,"currency":"SGD",
                 "paymentMethod":"bank-transfer","timestamp":"2026-10-01T10:45:00Z"}
                """.formatted(id));

        ConsumerRecord<String, String> record = waitForRecord("transactions-enriched", id);
        assertEquals(id, record.key());
        JsonNode out = mapper.readTree(record.value());
        assertEquals("ELEKTRONIK SEJAHTERA", out.get("merchant").asText());
        assertEquals(15000000, out.get("amountIdr").asLong());
        assertTrue(out.get("highRisk").asBoolean());

        TransactionEntity stored = QuarkusTransaction.requiringNew().call(() -> repository.findById(id));
        assertNotNull(stored);
        assertEquals("BANK_TRANSFER", stored.paymentMethod);
    }

    @Test
    void valueThatDoesNotFitTheTableIsProducedToDlqAndConsumerKeepsRunning() throws Exception {
        String tooLong = "TRX-" + "9".repeat(80);
        send("""
                {"transactionId":"%s","customerId":"CUST-07","merchant":"Toko Panjang",
                 "amount":5000,"currency":"IDR","timestamp":"2026-10-01T14:00:00Z"}
                """.formatted(tooLong));
        String id = "TRX-" + UUID.randomUUID();
        send("""
                {"transactionId":"%s","customerId":"CUST-08","merchant":"Toko Berikutnya",
                 "amount":5000,"currency":"IDR","timestamp":"2026-10-01T14:05:00Z"}
                """.formatted(id));

        JsonNode rejected = mapper.readTree(waitForRecord("transactions-dlq", tooLong).value());
        assertEquals("transactionId must not exceed 64 characters", rejected.get("reason").asText());
        assertNotNull(waitForRecord("transactions-enriched", id));
    }

    @Test
    void invalidTransactionIsProducedToDlqAndNotInserted() throws Exception {
        String id = "TRX-" + UUID.randomUUID();
        send("""
                {"transactionId":"%s","customerId":"CUST-05","merchant":"Toko Buku",
                 "amount":-5000,"currency":"IDR","timestamp":"2026-10-01T14:00:00Z"}
                """.formatted(id));

        JsonNode rejected = mapper.readTree(waitForRecord("transactions-dlq", id).value());
        assertEquals("amount must be greater than 0", rejected.get("reason").asText());

        assertNull(QuarkusTransaction.requiringNew().call(() -> repository.findById(id)));
    }

    private void send(String payload) throws Exception {
        Map<String, Object> config = Map.of(
                "bootstrap.servers", bootstrapServers,
                "key.serializer", StringSerializer.class.getName(),
                "value.serializer", StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(config)) {
            producer.send(new ProducerRecord<>("transactions-raw", payload)).get();
        }
    }

    private ConsumerRecord<String, String> waitForRecord(String topic, String marker) {
        Map<String, Object> config = Map.of(
                "bootstrap.servers", bootstrapServers,
                "group.id", "it-" + UUID.randomUUID(),
                "auto.offset.reset", "earliest",
                "key.deserializer", StringDeserializer.class.getName(),
                "value.deserializer", StringDeserializer.class.getName());
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + TIMEOUT.toMillis();
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.value().contains(marker)) {
                        return record;
                    }
                }
            }
        }
        throw new AssertionError("Tidak ada pesan untuk " + marker + " di topic " + topic);
    }
}

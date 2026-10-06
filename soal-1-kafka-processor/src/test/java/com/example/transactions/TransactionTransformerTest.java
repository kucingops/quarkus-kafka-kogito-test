package com.example.transactions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.example.transactions.model.AmountCategory;
import com.example.transactions.model.EnrichedTransaction;
import com.example.transactions.model.RawTransaction;
import com.example.transactions.service.InvalidTransactionException;
import com.example.transactions.service.TransactionTransformer;

class TransactionTransformerTest {

    private final TransactionTransformer transformer = new TransactionTransformer();

    private static RawTransaction raw(String amount, String currency) {
        return new RawTransaction("TRX-1", "CUST-1", "budi.santoso@gmail.com", "  toko   maju jaya ",
                new BigDecimal(amount), currency, "e-wallet", Instant.parse("2026-10-01T08:15:00Z"));
    }

    @Test
    void normalizesTextFieldsAndMasksEmail() {
        EnrichedTransaction tx = transformer.transform(raw("75000", "idr"));

        assertEquals("TOKO MAJU JAYA", tx.merchant());
        assertEquals("IDR", tx.originalCurrency());
        assertEquals("E_WALLET", tx.paymentMethod());
        assertEquals("bu**********@gmail.com", tx.maskedEmail());
    }

    @Test
    void convertsForeignCurrencyToRupiah() {
        EnrichedTransaction tx = transformer.transform(raw("45.5", "USD"));

        assertEquals(new BigDecimal("728000"), tx.amountIdr());
        assertEquals(AmountCategory.MEDIUM, tx.category());
        assertFalse(tx.highRisk());
    }

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource({
            "99999,    IDR, SMALL",
            "100000,   IDR, MEDIUM",
            "999999,   IDR, MEDIUM",
            "1000000,  IDR, LARGE",
            "9999999,  IDR, LARGE",
            "10000000, IDR, VERY_LARGE",
            "1250,     SGD, VERY_LARGE"
    })
    void categorizesByRupiahAmount(String amount, String currency, AmountCategory expected) {
        assertEquals(expected, transformer.transform(raw(amount, currency)).category());
    }

    @Test
    void flagsHighRiskFromTenMillionRupiah() {
        assertTrue(transformer.transform(raw("10000000", "IDR")).highRisk());
        assertTrue(transformer.transform(raw("1200", "EUR")).highRisk());
        assertFalse(transformer.transform(raw("9999999", "IDR")).highRisk());
    }

    @Test
    void rejectsNonPositiveAmount() {
        InvalidTransactionException e = assertThrows(InvalidTransactionException.class,
                () -> transformer.transform(raw("-5000", "IDR")));
        assertEquals("amount must be greater than 0", e.getMessage());
    }

    @Test
    void rejectsUnsupportedCurrency() {
        InvalidTransactionException e = assertThrows(InvalidTransactionException.class,
                () -> transformer.transform(raw("30000", "JPY")));
        assertEquals("unsupported currency: JPY", e.getMessage());
    }

    @Test
    void rejectsValuesThatDoNotFitTheTable() {
        RawTransaction longId = new RawTransaction("T".repeat(65), "CUST-1", null, "Shop",
                BigDecimal.TEN, "IDR", null, Instant.now());
        InvalidTransactionException e = assertThrows(InvalidTransactionException.class,
                () -> transformer.transform(longId));
        assertEquals("transactionId must not exceed 64 characters", e.getMessage());

        assertThrows(InvalidTransactionException.class, () -> transformer.transform(raw("1000000000001", "IDR")));
        assertThrows(InvalidTransactionException.class, () -> transformer.transform(raw("0.00001", "USD")));
    }

    @Test
    void masksShortEmailLocalPart() {
        assertEquals("*@x.com", maskedEmailOf("a@x.com"));
        assertEquals("a*@x.com", maskedEmailOf("ab@x.com"));
        assertEquals("ab*@x.com", maskedEmailOf("abc@x.com"));
    }

    private String maskedEmailOf(String email) {
        return transformer.transform(new RawTransaction("TRX-1", "CUST-1", email, "Shop",
                BigDecimal.TEN, "IDR", null, Instant.now())).maskedEmail();
    }

    @Test
    void rejectsMissingRequiredFields() {
        RawTransaction noCustomer = new RawTransaction("TRX-1", " ", null, "Shop",
                BigDecimal.TEN, "IDR", null, Instant.now());
        assertThrows(InvalidTransactionException.class, () -> transformer.transform(noCustomer));
    }
}

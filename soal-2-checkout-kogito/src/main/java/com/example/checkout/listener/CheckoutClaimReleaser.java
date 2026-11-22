package com.example.checkout.listener;

import org.jboss.logging.Logger;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.kogito.internal.process.event.DefaultKogitoProcessEventListener;
import org.kie.kogito.internal.process.runtime.KogitoWorkflowProcessInstance;

import com.example.checkout.model.Checkout;
import com.example.checkout.model.CheckoutStatus;
import com.example.checkout.service.IdempotencyService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Process event listener Kogito: saat proses {@code checkout} selesai tanpa menghasilkan order,
 * klaim {@code requestId} dilepas lagi. Dengan begitu {@code requestId} hanya "terpakai" oleh checkout
 * yang berhasil, dan retry setelah stok habis atau pembayaran gagal tidak ditolak sebagai duplikat.
 *
 * <p>Logika ini sengaja ditaruh di listener, bukan di tiap End Event gagal, supaya end event baru
 * yang ditambahkan nanti otomatis ikut tertangani.
 */
@ApplicationScoped
public class CheckoutClaimReleaser extends DefaultKogitoProcessEventListener {

    private static final Logger LOG = Logger.getLogger(CheckoutClaimReleaser.class);
    static final String PROCESS_ID = "checkout";

    @Inject
    IdempotencyService idempotencyService;

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        if (!PROCESS_ID.equals(event.getProcessInstance().getProcessId())
                || !(event.getProcessInstance() instanceof KogitoWorkflowProcessInstance pi)
                || !(pi.getVariable("checkout") instanceof Checkout checkout)) {
            return;
        }
        if (checkout.getStatus() == CheckoutStatus.COMPLETED || !checkout.isRequestClaimed()) {
            return;
        }
        try {
            idempotencyService.releaseClaim(checkout);
        } catch (RuntimeException e) {
            // Gagal melepas klaim tidak boleh mengubah hasil checkout; paling buruk client perlu requestId baru.
            LOG.errorf(e, "Failed to release requestId %s", checkout.getRequestId());
        }
    }
}

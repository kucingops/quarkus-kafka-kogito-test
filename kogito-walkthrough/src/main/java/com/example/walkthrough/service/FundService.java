package com.example.walkthrough.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.walkthrough.model.Applicant;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Langkah 8: simulasi pencairan dana. Dipakai untuk mendemokan Boundary Error Event
 * dan kompensasi BPMN. Setiap aksi dicatat di {@link #journal()} supaya test bisa
 * memeriksa urutan langkah yang benar-benar dijalankan engine.
 */
@ApplicationScoped
public class FundService {

    /** Batas transfer per transaksi. Di atas angka ini, transfer gagal. */
    public static final long TRANSFER_LIMIT = 100_000_000L;

    private static final Logger LOG = Logger.getLogger(FundService.class);

    private final List<String> journal = Collections.synchronizedList(new ArrayList<>());

    public void reserveFunds(Applicant applicant) {
        record("RESERVE " + applicant.getName() + " " + applicant.getRequestedAmount());
    }

    public void releaseFunds(Applicant applicant) {
        record("RELEASE " + applicant.getName() + " " + applicant.getRequestedAmount());
    }

    public void transfer(Applicant applicant) {
        if (applicant.getRequestedAmount() > TRANSFER_LIMIT) {
            record("TRANSFER_FAILED " + applicant.getName());
            throw new TransferFailedException("transfer " + applicant.getRequestedAmount() + " melebihi limit " + TRANSFER_LIMIT);
        }
        record("TRANSFER " + applicant.getName() + " " + applicant.getRequestedAmount());
    }

    public List<String> journal() {
        synchronized (journal) {
            return List.copyOf(journal);
        }
    }

    public void clearJournal() {
        journal.clear();
    }

    private void record(String entry) {
        LOG.infof("[fund] %s", entry);
        journal.add(entry);
    }
}

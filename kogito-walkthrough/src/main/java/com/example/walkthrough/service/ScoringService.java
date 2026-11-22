package com.example.walkthrough.service;

import com.example.walkthrough.model.Applicant;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Langkah 2: CDI bean biasa yang dipanggil dari Service Task BPMN.
 * Kogito menemukan bean ini lewat atribut {@code drools:serviceinterface}.
 */
@ApplicationScoped
public class ScoringService {

    private static final Logger LOG = Logger.getLogger(ScoringService.class);

    /**
     * Skor sederhana: mulai dari credit score, dikurangi penalti kalau cicilan
     * (pinjaman / 12 bulan) lebih dari 30% penghasilan bulanan.
     */
    public int score(Applicant applicant) {
        int score = applicant.getCreditScore();
        long installment = applicant.getRequestedAmount() / 12;
        if (applicant.getMonthlyIncome() > 0 && installment * 100 / applicant.getMonthlyIncome() > 30) {
            score -= 100;
        }
        LOG.infof("[scoring] %s -> %d", applicant.getName(), score);
        return score;
    }
}

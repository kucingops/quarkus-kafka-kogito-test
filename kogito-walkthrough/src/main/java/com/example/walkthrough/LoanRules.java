package com.example.walkthrough;

import com.example.walkthrough.model.Alert;
import com.example.walkthrough.model.Applicant;

import org.drools.ruleunits.api.DataSource;
import org.drools.ruleunits.api.DataStore;
import org.drools.ruleunits.api.RuleUnitData;

/**
 * Langkah 6: Rule Unit. Class ini adalah "pintu masuk" ke rule di {@code LoanRules.drl}.
 * Setiap field {@link DataStore} menjadi sumber fakta yang bisa dicocokkan rule lewat OOPath
 * ({@code /applicants}, {@code /alerts}). Kogito men-generate REST endpoint untuk setiap
 * {@code query} di file DRL, dengan body JSON berbentuk class ini.
 */
public class LoanRules implements RuleUnitData {

    private final DataStore<Applicant> applicants;
    private final DataStore<Alert> alerts;

    public LoanRules() {
        this(DataSource.createStore(), DataSource.createStore());
    }

    public LoanRules(DataStore<Applicant> applicants, DataStore<Alert> alerts) {
        this.applicants = applicants;
        this.alerts = alerts;
    }

    public DataStore<Applicant> getApplicants() {
        return applicants;
    }

    public DataStore<Alert> getAlerts() {
        return alerts;
    }
}

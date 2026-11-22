package com.example.walkthrough.model;

/** Peringatan yang dihasilkan rule DRL di Langkah 6. */
public class Alert {

    private String applicant;
    private String severity;
    private String message;

    public Alert() {
    }

    public Alert(String applicant, String severity, String message) {
        this.applicant = applicant;
        this.severity = severity;
        this.message = message;
    }

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

package com.example.walkthrough.model;

/**
 * Data pemohon pinjaman. Dipakai bersama oleh semua langkah walkthrough:
 * sebagai process variable di BPMN, sebagai fakta di DRL, dan sebagai input DMN.
 */
public class Applicant {

    private String name;
    private int age;
    private long monthlyIncome;
    private long requestedAmount;
    private int creditScore;

    public Applicant() {
    }

    public Applicant(String name, int age, long monthlyIncome, long requestedAmount, int creditScore) {
        this.name = name;
        this.age = age;
        this.monthlyIncome = monthlyIncome;
        this.requestedAmount = requestedAmount;
        this.creditScore = creditScore;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public long getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(long monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public long getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(long requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    @Override
    public String toString() {
        return "Applicant[" + name + ", age=" + age + ", income=" + monthlyIncome
                + ", requested=" + requestedAmount + ", score=" + creditScore + "]";
    }
}

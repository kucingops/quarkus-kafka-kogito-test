package com.example.walkthrough.service;

/** Dilempar {@link FundService#transfer}; ditangkap Boundary Error Event di BPMN Langkah 8. */
public class TransferFailedException extends RuntimeException {

    public TransferFailedException(String message) {
        super(message);
    }
}

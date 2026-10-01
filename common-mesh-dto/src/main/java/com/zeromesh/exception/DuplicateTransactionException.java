package com.zeromesh.exception;

/**
 * Thrown when an already-claimed or settled packet is ingested.
 * Maps to HTTP 409 CONFLICT.
 */
public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String packetId) {
        super("Duplicate transaction detected. Packet has already been ingested or settled: " + packetId);
    }
}

package com.zeromesh.exception;

/**
 * Thrown when SHA-256 hash or AES-GCM Authentication Tag integrity check fails.
 * Maps to HTTP 400 BAD REQUEST.
 */
public class PacketTamperedException extends RuntimeException {
    public PacketTamperedException(String message) {
        super(message);
    }
}

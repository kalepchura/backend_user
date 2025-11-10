// ============================================
// BadRequestException.java
// ============================================
package com.tecsup.productivity.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
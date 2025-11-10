// ============================================
// UnauthorizedException.java
// ============================================
package com.tecsup.productivity.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
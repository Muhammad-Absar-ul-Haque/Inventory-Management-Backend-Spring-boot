package com.example.inventory_order_system.exception;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

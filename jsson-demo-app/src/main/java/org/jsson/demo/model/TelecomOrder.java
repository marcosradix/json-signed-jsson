package org.jsson.demo.model;

/**
 * Simple POJO to demonstrate the cryptographic mesh overloading HTTP without polluting the business class.
 */
public record TelecomOrder(String orderId, String service, Double price) {
}

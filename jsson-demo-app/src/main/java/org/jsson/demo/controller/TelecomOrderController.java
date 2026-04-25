package org.jsson.demo.controller;

import org.jsson.demo.model.TelecomOrder;
import org.jsson.spring.annotation.JssonSign;
import org.jsson.spring.annotation.JssonVerify;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class TelecomOrderController {

    // Simulates legacy API or remote Operator provisioner that now magically
    // injects our signature
    @GetMapping("/{id}")
    @JssonSign(includes = { "orderId", "price" })
    public TelecomOrder getOrder(@PathVariable("id") String id) {
        return new TelecomOrder(id, "5G_PREMIUM_UNLIMITED", 39.99);
    }

    @GetMapping("/all/{id}")
    @JssonSign
    public TelecomOrder getOrderAllFields(@PathVariable("id") String id) {
        return new TelecomOrder(id, "5G_PREMIUM_UNLIMITED", 39.99);
    }

    // Receives Payload from Client/EdgeRouter and automatically blocks violated or
    // modified payloads in TCP/IP
    @JssonSign(includes = { "orderId", "price" })
    @PostMapping("/process")
    public ResponseEntity<TelecomOrder> processOrder(
            @RequestBody @JssonVerify TelecomOrder order) {
        return ResponseEntity
                .ok(order);
    }
}

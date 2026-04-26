package org.jsson.demo.controller;

import org.jsson.demo.api.ApiApi;
import org.jsson.demo.model.TelecomOrder;
import org.jsson.spring.annotation.JssonVerify;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TelecomOrderController implements ApiApi {

    // Simulates legacy API or remote Operator provisioner that now magically
    // injects our signature
    @Override
    public ResponseEntity<TelecomOrder> getOrder(String id) {
        return ResponseEntity.ok(new TelecomOrder().orderId(id).service("5G_PREMIUM_UNLIMITED").price(39.99));
    }

    @Override
    public ResponseEntity<TelecomOrder> getOrderService(String id) {
        return ResponseEntity.ok(new TelecomOrder().orderId(id).service("5G_PREMIUM_UNLIMITED").price(39.99));
    }

    @Override
    public ResponseEntity<TelecomOrder> getOrderAllFields(String id) {
        return ResponseEntity.ok(new TelecomOrder().orderId(id).service("5G_PREMIUM_UNLIMITED").price(39.99));
    }

    // Receives Payload from Client/EdgeRouter and automatically blocks violated or
    // modified payloads in TCP/IP
    @Override
    public ResponseEntity<TelecomOrder> processOrder(@JssonVerify TelecomOrder order) {
        return ResponseEntity.ok(order);
    }
}

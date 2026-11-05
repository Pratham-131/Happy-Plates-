package com.foodify.foodiesapi.controller;
import com.foodify.foodiesapi.io.OrderRequest;
import com.foodify.foodiesapi.io.OrderResponse;
import com.foodify.foodiesapi.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/orders")
@AllArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/create")
    public OrderResponse createOrderWithPayment(@RequestBody OrderRequest request) {
        return orderService.createOrderWithPayment(request);
    }

    @PostMapping("/verify")
    public Map<String, Boolean> verifyPayment(@RequestBody Map<String, String> paymentData) {
        if (paymentData == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment data is required");
        }
        String razorpayOrderId = paymentData.get("razorpay_order_id");
        String razorpayPaymentId = paymentData.get("razorpay_payment_id");
        String razorpaySignature = paymentData.get("razorpay_signature");
        if (isBlank(razorpayOrderId) || isBlank(razorpayPaymentId) || isBlank(razorpaySignature)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required payment fields are missing");
        }
        return Map.of("verified",
                orderService.verifyPayment(razorpayOrderId, razorpayPaymentId, razorpaySignature));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @GetMapping
    public List<OrderResponse> getUserOrders() {
        return orderService.getUserOrders();
    }

    //admin panel
    @GetMapping("/all")
    public List<OrderResponse> getOrdersOfAllUsers() {
        return orderService.getOrdersOfAllUsers();
    }
    //admin panel
    @PatchMapping("/status/{orderId}")
    public void updateOrderStatus(@PathVariable String orderId, @RequestParam String status) {
        orderService.updateOrderStatus(orderId, status);
    }
}
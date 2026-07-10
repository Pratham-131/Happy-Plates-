package com.foodify.foodiesapi.controller;
import com.foodify.foodiesapi.io.OrderRequest;
import com.foodify.foodiesapi.io.OrderResponse;
import com.foodify.foodiesapi.service.OrderService;
import com.razorpay.RazorpayException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
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
        return Map.of("verified", true);
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
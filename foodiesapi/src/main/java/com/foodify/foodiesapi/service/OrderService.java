package com.foodify.foodiesapi.service;

import com.foodify.foodiesapi.io.OrderRequest;
import com.foodify.foodiesapi.io.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrderWithPayment(OrderRequest request);

    List<OrderResponse> getOrdersOfAllUsers();

    List<OrderResponse> getUserOrders();

    void updateOrderStatus(String orderId, String status);

    boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature);

}
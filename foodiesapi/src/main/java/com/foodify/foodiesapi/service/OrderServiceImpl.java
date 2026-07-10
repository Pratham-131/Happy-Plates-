package com.foodify.foodiesapi.service;


import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Order;

import com.foodify.foodiesapi.entity.OrderEntity;
import com.foodify.foodiesapi.io.OrderRequest;
import com.foodify.foodiesapi.io.OrderResponse;
import com.foodify.foodiesapi.repository.CartRespository;
import com.foodify.foodiesapi.repository.OrderRepository;


import lombok.AllArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private CartRespository cartRespository;

   
    @Value("${razorpay.key.id}")
private String razorpayKeyId;

@Value("${razorpay.key.secret}")
private String razorpayKeySecret;

@Value("${app.stub.payment:false}")
private boolean stubPayment;

@Override
public OrderResponse createOrderWithPayment(OrderRequest request) {
    OrderEntity newOrder = convertToEntity(request);

    if (stubPayment) {
        newOrder.setRazorpayOrderId("stub_order_" + System.currentTimeMillis());
        newOrder.setPaymentStatus("PENDING");
    } else {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject options = new JSONObject();
            options.put("amount", (int) (request.getAmount() * 100));
            options.put("currency", "INR");
            options.put("receipt", "txn_" + System.currentTimeMillis());
            Order razorpayOrder = client.orders.create(options);
            newOrder.setRazorpayOrderId(razorpayOrder.get("id"));
            newOrder.setPaymentStatus("PENDING");
        } catch (RazorpayException e) {
            throw new RuntimeException("Error creating Razorpay order: " + e.getMessage());
        }
    }

    newOrder = orderRepository.save(newOrder);
    return convertToResponse(newOrder);
}


    @Override
    public List<OrderResponse> getOrdersOfAllUsers() {
        List<OrderEntity> list = orderRepository.findAll();
        return list.stream().map(entity -> convertToResponse(entity)).collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getUserOrders() {
        String userId = userService.findByUserId();
        List<OrderEntity> list = orderRepository.findByUserId(userId);
        return list.stream().map(entity -> convertToResponse(entity)).collect(Collectors.toList());
    }

    @Override
    public void updateOrderStatus(String orderId, String status) {
        OrderEntity entity = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        entity.setOrderStatus(status);
        orderRepository.save(entity);
    }

    private OrderResponse convertToResponse(OrderEntity newOrder) {
        return OrderResponse.builder()
                .id(newOrder.getId())
                .amount(newOrder.getAmount())
                .userAddress(newOrder.getUserAddress())
                .userId(newOrder.getUserId())
                .razorpayOrderId(newOrder.getRazorpayOrderId())
                .paymentStatus(newOrder.getPaymentStatus())
                .orderStatus(newOrder.getOrderStatus())
                .email(newOrder.getEmail())
                .phoneNumber(newOrder.getPhoneNumber())
                .orderedItems(newOrder.getOrderedItems())
                .build();
    }

    private OrderEntity convertToEntity(OrderRequest request) {
        return OrderEntity.builder()
                .userId(userService.findByUserId())
                .userAddress(request.getUserAddress())
                .amount(request.getAmount())
                .orderedItems(request.getOrderedItems())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .orderStatus(request.getOrderStatus())
                .build();
    }
}

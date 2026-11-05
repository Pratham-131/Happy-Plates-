package com.foodify.foodiesapi.service;


import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Order;

import com.foodify.foodiesapi.entity.FoodEntity;
import com.foodify.foodiesapi.entity.OrderEntity;
import com.foodify.foodiesapi.io.OrderItem;
import com.foodify.foodiesapi.io.OrderRequest;
import com.foodify.foodiesapi.io.OrderResponse;
import com.foodify.foodiesapi.repository.CartRespository;
import com.foodify.foodiesapi.repository.FoodRepository;
import com.foodify.foodiesapi.repository.OrderRepository;


import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService{

    private static final Set<String> ALLOWED_ORDER_STATUSES =
            Set.of("Preparing", "Out for delivery", "Delivered", "Cancelled");

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private FoodRepository foodRepository;
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
            options.put("amount", BigDecimal.valueOf(newOrder.getAmount())
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact());
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
        if (!ALLOWED_ORDER_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status");
        }
        OrderEntity entity = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
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
        if (request.getOrderedItems() == null || request.getOrderedItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordered items are required");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderedItems = new ArrayList<>();
        for (OrderItem item : request.getOrderedItems()) {
            if (item == null || item.getQuantity() <= 0 || item.getFoodId() == null || item.getFoodId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each ordered item must have a food id and positive quantity");
            }

            FoodEntity food = foodRepository.findById(item.getFoodId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown food id"));
            BigDecimal itemPrice = BigDecimal.valueOf(food.getPrice())
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemPrice);
            orderedItems.add(OrderItem.builder()
                    .foodId(item.getFoodId())
                    .quantity(item.getQuantity())
                    .price(itemPrice.doubleValue())
                    .name(food.getName())
                    .category(food.getCategory())
                    .imageUrl(food.getImageUrl())
                    .description(food.getDescription())
                    .build());
        }

        BigDecimal shipping = subtotal.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.TEN : BigDecimal.ZERO;
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.1));
        BigDecimal total = subtotal.add(shipping).add(tax).setScale(2, RoundingMode.HALF_UP);

        return OrderEntity.builder()
                .userId(userService.findByUserId())
                .userAddress(request.getUserAddress())
                .amount(total.doubleValue())
                .orderedItems(orderedItems)
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .orderStatus("Preparing")
                .build();
    }
}

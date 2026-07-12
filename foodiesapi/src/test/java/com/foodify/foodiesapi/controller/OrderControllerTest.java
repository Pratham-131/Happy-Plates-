package com.foodify.foodiesapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodify.foodiesapi.io.OrderItem;
import com.foodify.foodiesapi.io.OrderRequest;
import com.foodify.foodiesapi.io.OrderResponse;
import com.foodify.foodiesapi.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private OrderRequest orderRequest;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();

        OrderItem item = OrderItem.builder()
                .foodId("food1")
                .quantity(2)
                .price(500.0)
                .category("Biryani")
                .imageUrl("img.jpg")
                .description("Tasty")
                .name("Chicken Biryani")
                .build();

        orderRequest = OrderRequest.builder()
                .orderedItems(List.of(item))
                .userAddress("123 Main St, Bangalore, Karnataka, 560001")
                .amount(500.0)
                .email("test@example.com")
                .phoneNumber("9876543210")
                .orderStatus("Preparing")
                .build();

        orderResponse = OrderResponse.builder()
                .id("order1")
                .userId("user1")
                .userAddress("123 Main St, Bangalore, Karnataka, 560001")
                .phoneNumber("9876543210")
                .email("test@example.com")
                .amount(500.0)
                .paymentStatus("PENDING")
                .razorpayOrderId("stub_order_123")
                .orderStatus("Preparing")
                .orderedItems(List.of(item))
                .build();
    }

    @Test
    void createOrder_validRequest_returnsOrderResponse() throws Exception {
        when(orderService.createOrderWithPayment(any(OrderRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/orders/create")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("order1"))
                .andExpect(jsonPath("$.razorpayOrderId").value("stub_order_123"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        verify(orderService, times(1)).createOrderWithPayment(any(OrderRequest.class));
    }

    @Test
    void verifyPayment_returnsVerifiedTrue() throws Exception {
        mockMvc.perform(post("/api/orders/verify")
                        .contentType("application/json")
                        .content("{\"razorpay_order_id\":\"stub_order_123\",\"razorpay_payment_id\":\"pay_123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void getUserOrders_returnsListOfOrders() throws Exception {
        when(orderService.getUserOrders()).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("order1"))
                .andExpect(jsonPath("$[0].userId").value("user1"));

        verify(orderService, times(1)).getUserOrders();
    }

    @Test
    void getOrdersOfAllUsers_adminEndpoint_returnsAllOrders() throws Exception {
        when(orderService.getOrdersOfAllUsers()).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/orders/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("order1"));

        verify(orderService, times(1)).getOrdersOfAllUsers();
    }

    @Test
    void updateOrderStatus_validOrderId_callsServiceWithCorrectArgs() throws Exception {
        doNothing().when(orderService).updateOrderStatus(eq("order1"), eq("Delivered"));

        mockMvc.perform(patch("/api/orders/status/order1")
                        .param("status", "Delivered"))
                .andExpect(status().isOk());

        verify(orderService, times(1)).updateOrderStatus("order1", "Delivered");
    }

    @Test
    void updateOrderStatus_serviceThrows_propagatesError() {
        doThrow(new RuntimeException("Order not found"))
                .when(orderService).updateOrderStatus(eq("bad_id"), any());

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(patch("/api/orders/status/bad_id")
                        .param("status", "Delivered"))
        );

        verify(orderService, times(1)).updateOrderStatus("bad_id", "Delivered");
    }
}
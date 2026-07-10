package com.foodify.foodiesapi.service;

import com.foodify.foodiesapi.entity.OrderEntity;
import com.foodify.foodiesapi.io.OrderItem;
import com.foodify.foodiesapi.io.OrderRequest;
import com.foodify.foodiesapi.io.OrderResponse;
import com.foodify.foodiesapi.repository.CartRespository;
import com.foodify.foodiesapi.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserService userService;

    @Mock
    private CartRespository cartRespository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderRequest orderRequest;
    private OrderEntity orderEntity;

    @BeforeEach
    void setUp() {
        // force stub payment mode so no real Razorpay network call happens in tests
        ReflectionTestUtils.setField(orderService, "stubPayment", true);
        ReflectionTestUtils.setField(orderService, "razorpayKeyId", "test_key");
        ReflectionTestUtils.setField(orderService, "razorpayKeySecret", "test_secret");

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

        orderEntity = OrderEntity.builder()
                .id("order1")
                .userId("user1")
                .userAddress("123 Main St, Bangalore, Karnataka, 560001")
                .phoneNumber("9876543210")
                .email("test@example.com")
                .orderedItems(List.of(item))
                .amount(500.0)
                .paymentStatus("PENDING")
                .razorpayOrderId("stub_order_123")
                .orderStatus("Preparing")
                .build();
    }

    @Test
    void createOrderWithPayment_stubMode_savesOrderWithStubId() {
        when(userService.findByUserId()).thenReturn("user1");
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(orderEntity);

        OrderResponse response = orderService.createOrderWithPayment(orderRequest);

        assertThat(response).isNotNull();
        assertThat(response.getRazorpayOrderId()).startsWith("stub_order_");
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        verify(orderRepository, times(1)).save(any(OrderEntity.class));
    }

    @Test
    void createOrderWithPayment_stampsUserIdFromLoggedInUser() {
        when(userService.findByUserId()).thenReturn("user1");
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrderWithPayment(orderRequest);

        assertThat(response.getUserId()).isEqualTo("user1");
    }

    @Test
    void getOrdersOfAllUsers_returnsAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(orderEntity));

        List<OrderResponse> result = orderService.getOrdersOfAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("order1");
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void getUserOrders_returnsOnlyLoggedInUsersOrders() {
        when(userService.findByUserId()).thenReturn("user1");
        when(orderRepository.findByUserId("user1")).thenReturn(List.of(orderEntity));

        List<OrderResponse> result = orderService.getUserOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user1");
        verify(orderRepository, times(1)).findByUserId("user1");
    }

    @Test
    void getUserOrders_noOrders_returnsEmptyList() {
        when(userService.findByUserId()).thenReturn("user2");
        when(orderRepository.findByUserId("user2")).thenReturn(List.of());

        List<OrderResponse> result = orderService.getUserOrders();

        assertThat(result).isEmpty();
    }

    @Test
    void updateOrderStatus_validOrder_updatesStatus() {
        when(orderRepository.findById("order1")).thenReturn(Optional.of(orderEntity));
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(orderEntity);

        orderService.updateOrderStatus("order1", "Delivered");

        verify(orderRepository, times(1)).findById("order1");
        verify(orderRepository, times(1)).save(argThat(entity -> entity.getOrderStatus().equals("Delivered")));
    }

    @Test
    void updateOrderStatus_invalidOrderId_throwsException() {
        when(orderRepository.findById("bad_id")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orderService.updateOrderStatus("bad_id", "Delivered"));

        verify(orderRepository, never()).save(any(OrderEntity.class));
    }
}
package com.foodify.foodiesapi.service;

import com.foodify.foodiesapi.entity.OrderEntity;
import com.foodify.foodiesapi.entity.FoodEntity;
import com.foodify.foodiesapi.io.OrderItem;
import com.foodify.foodiesapi.io.OrderRequest;
import com.foodify.foodiesapi.io.OrderResponse;
import com.foodify.foodiesapi.repository.CartRespository;
import com.foodify.foodiesapi.repository.FoodRepository;
import com.foodify.foodiesapi.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
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
    private FoodRepository foodRepository;

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
        mockFood();
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
        mockFood();
        when(userService.findByUserId()).thenReturn("user1");
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrderWithPayment(orderRequest);

        assertThat(response.getUserId()).isEqualTo("user1");
    }

    @Test
    void createOrderWithPayment_ignoresClientAmountAndItemDetails() {
        mockFood();
        orderRequest.setAmount(1.0);
        OrderItem requestedItem = orderRequest.getOrderedItems().get(0);
        requestedItem.setPrice(1.0);
        requestedItem.setName("Untrusted name");
        requestedItem.setCategory("Untrusted category");
        requestedItem.setImageUrl("untrusted.jpg");
        requestedItem.setDescription("Untrusted description");
        when(userService.findByUserId()).thenReturn("user1");
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrderWithPayment(orderRequest);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        OrderEntity savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getAmount()).isEqualTo(230.0);
        assertThat(savedOrder.getOrderedItems().get(0).getPrice()).isEqualTo(200.0);
        assertThat(savedOrder.getOrderedItems().get(0).getName()).isEqualTo("Chicken Biryani");
        assertThat(savedOrder.getOrderedItems().get(0).getCategory()).isEqualTo("Biryani");
        assertThat(savedOrder.getOrderedItems().get(0).getImageUrl()).isEqualTo("img.jpg");
        assertThat(savedOrder.getOrderedItems().get(0).getDescription()).isEqualTo("Tasty");
        assertThat(response.getAmount()).isEqualTo(230.0);
    }

    @Test
    void createOrderWithPayment_ignoresClientStatusAndAllowsDuplicateFoodLines() {
        mockFood();
        OrderItem secondItem = OrderItem.builder()
                .foodId("food1")
                .quantity(1)
                .price(999.0)
                .name("Untrusted name")
                .build();
        orderRequest.setOrderedItems(List.of(orderRequest.getOrderedItems().get(0), secondItem));
        orderRequest.setAmount(1.0);
        orderRequest.setOrderStatus("DELIVERED");
        when(userService.findByUserId()).thenReturn("user1");
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrderWithPayment(orderRequest);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        OrderEntity savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getAmount()).isEqualTo(340.0);
        assertThat(savedOrder.getOrderStatus()).isEqualTo("Preparing");
        assertThat(savedOrder.getOrderedItems()).hasSize(2);
        assertThat(savedOrder.getOrderedItems()).extracting(OrderItem::getPrice)
                .containsExactly(200.0, 100.0);
    }

    @Test
    void createOrderWithPayment_unknownFoodId_throwsBadRequest() {
        orderRequest.getOrderedItems().get(0).setFoodId("missing");
        when(foodRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrderWithPayment(orderRequest));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void createOrderWithPayment_zeroQuantity_throwsBadRequest() {
        orderRequest.getOrderedItems().get(0).setQuantity(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrderWithPayment(orderRequest));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void createOrderWithPayment_emptyOrderedItems_throwsBadRequest() {
        orderRequest.setOrderedItems(List.of());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrderWithPayment(orderRequest));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void createOrderWithPayment_nullOrderedItems_throwsBadRequest() {
        orderRequest.setOrderedItems(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrderWithPayment(orderRequest));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void createOrderWithPayment_nullFoodId_throwsBadRequest() {
        orderRequest.getOrderedItems().get(0).setFoodId(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrderWithPayment(orderRequest));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderRepository, never()).save(any(OrderEntity.class));
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
    void updateOrderStatus_invalidStatus_throwsBadRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.updateOrderStatus("order1", "DELIVERED"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseStatusException nullStatusException = assertThrows(ResponseStatusException.class,
                () -> orderService.updateOrderStatus("order1", null));
        assertThat(nullStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderRepository, never()).findById(anyString());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void updateOrderStatus_invalidOrderId_throwsException() {
        when(orderRepository.findById("bad_id")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.updateOrderStatus("bad_id", "Delivered"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void verifyPayment_wrongOwner_throwsForbidden() {
        when(orderRepository.findByRazorpayOrderId("stub_order_123")).thenReturn(Optional.of(orderEntity));
        when(userService.findByUserId()).thenReturn("another-user");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.verifyPayment("stub_order_123", "pay_123", "signature"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void verifyPayment_unknownOrder_throwsNotFound() {
        when(orderRepository.findByRazorpayOrderId("missing")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.verifyPayment("missing", "pay_123", "signature"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void verifyPayment_stubMode_savesPaidStubStatus() {
        when(orderRepository.findByRazorpayOrderId("stub_order_123")).thenReturn(Optional.of(orderEntity));
        when(userService.findByUserId()).thenReturn("user1");

        boolean verified = orderService.verifyPayment("stub_order_123", "pay_123", "signature");

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(verified).isTrue();
        assertThat(orderCaptor.getValue().getPaymentStatus()).isEqualTo("PAID_STUB");
    }

    @Test
    void verifyPayment_alreadyPaid_returnsTrueWithoutSaving() {
        orderEntity.setPaymentStatus("PAID");
        when(orderRepository.findByRazorpayOrderId("stub_order_123")).thenReturn(Optional.of(orderEntity));
        when(userService.findByUserId()).thenReturn("user1");

        boolean verified = orderService.verifyPayment("stub_order_123", "pay_123", "signature");

        assertThat(verified).isTrue();
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void verifyPayment_missingKey_throwsBadRequest() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.verifyPayment("stub_order_123", " ", "signature"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderRepository, never()).findByRazorpayOrderId(anyString());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void verifyPayment_realMode_validSignature_savesPaidStatus() throws Exception {
        ReflectionTestUtils.setField(orderService, "stubPayment", false);
        when(orderRepository.findByRazorpayOrderId("stub_order_123")).thenReturn(Optional.of(orderEntity));
        when(userService.findByUserId()).thenReturn("user1");
        String signature = hmacSha256Hex("stub_order_123|pay_123", "test_secret");

        boolean verified = orderService.verifyPayment("stub_order_123", "pay_123", signature);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(verified).isTrue();
        assertThat(orderCaptor.getValue().getPaymentStatus()).isEqualTo("PAID");
    }

    @Test
    void verifyPayment_realMode_badSignature_returnsFalseWithoutSaving() {
        ReflectionTestUtils.setField(orderService, "stubPayment", false);
        when(orderRepository.findByRazorpayOrderId("stub_order_123")).thenReturn(Optional.of(orderEntity));
        when(userService.findByUserId()).thenReturn("user1");

        boolean verified = orderService.verifyPayment("stub_order_123", "pay_123", "invalid-signature");

        assertThat(verified).isFalse();
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    private String hmacSha256Hex(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private void mockFood() {
        when(foodRepository.findById("food1")).thenReturn(Optional.of(FoodEntity.builder()
                .id("food1")
                .name("Chicken Biryani")
                .description("Tasty")
                .category("Biryani")
                .imageUrl("img.jpg")
                .price(100.0)
                .build()));
    }
}
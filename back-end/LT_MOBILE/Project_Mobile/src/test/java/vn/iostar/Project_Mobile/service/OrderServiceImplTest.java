package vn.iostar.Project_Mobile.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.iostar.Project_Mobile.DTO.CreateOrderRequest;
import vn.iostar.Project_Mobile.DTO.CreateOrderResponseDTO;
import vn.iostar.Project_Mobile.entity.*;
import vn.iostar.Project_Mobile.exception.ResourceNotFoundException;
import vn.iostar.Project_Mobile.repository.*;
import vn.iostar.Project_Mobile.service.impl.OrderServiceImpl;
import vn.iostar.Project_Mobile.service.impl.VnpayService;
import vn.iostar.Project_Mobile.util.OrderStatus;
import vn.iostar.Project_Mobile.util.PaymentMethod;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Sử dụng MockitoExtension để tự động khởi tạo mocks
class OrderServiceImplTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private IOrderRepository orderRepository;
    @Mock
    private OrderLineRepository orderLineRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private VnpayService vnpayService;
    @Mock
    private ICommentRepository commentRepository;
    @Mock
    private HttpServletRequest httpServletRequest; // Mock HttpServletRequest

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    private User currentUser;
    private Address defaultAddress;
    private Cart userCart;
    private Product product1, product2;
    private CartItem cartItem1, cartItem2;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setUserId(1L);
        currentUser.setEmail("user@example.com");

        defaultAddress = new Address();
        defaultAddress.setAddressId(10L);
        defaultAddress.setUser(currentUser);
        defaultAddress.setDefault(true);
        defaultAddress.setStreetAddress("123 Main St");
        defaultAddress.setWard("Ward 1");
        defaultAddress.setDistrict("District 1");
        defaultAddress.setCity("City 1");

        userCart = new Cart();
        userCart.setCartId(20L);
        userCart.setUser(currentUser);

        product1 = new Product();
        product1.setProductId(100L);
        product1.setName("Product A");
        product1.setPrice(10000.0);
        product1.setQuantity(10); // Tồn kho

        product2 = new Product();
        product2.setProductId(101L);
        product2.setName("Product B");
        product2.setPrice(20000.0);
        product2.setQuantity(5); // Tồn kho

        cartItem1 = new CartItem();
        cartItem1.setCartItemId(30L);
        cartItem1.setCart(userCart);
        cartItem1.setProduct(product1);
        cartItem1.setQuantity(2);

        cartItem2 = new CartItem();
        cartItem2.setCartItemId(31L);
        cartItem2.setCart(userCart);
        cartItem2.setProduct(product2);
        cartItem2.setQuantity(1);

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setCartItemIds(Arrays.asList(cartItem1.getCartItemId(), cartItem2.getCartItemId()));
    }

//    @Test
//    @DisplayName("Create Order - COD - Success")
//    void createOrder_COD_Success() {
//        createOrderRequest.setPaymentMethod(PaymentMethod.COD);
//
//        // Mocking repository calls
//        when(addressRepository.findByUser_UserIdAndIsDefaultTrue(currentUser.getUserId())).thenReturn(Optional.of(defaultAddress));
//        when(cartRepository.findByUser_UserId(currentUser.getUserId())).thenReturn(Optional.of(userCart));
//        when(cartItemRepository.findByCartItemIdInAndCart_CartId(anyList(), eq(userCart.getCartId())))
//                .thenReturn(Arrays.asList(cartItem1, cartItem2));
//
//        // Mock product findById for stock update (nếu có)
//        when(productRepository.findById(product1.getProductId())).thenReturn(Optional.of(product1));
//        when(productRepository.findById(product2.getProductId())).thenReturn(Optional.of(product2));
//
//        // Mock orderRepository.save to return the saved order with an ID
//        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
//            Order orderToSave = invocation.getArgument(0);
//            orderToSave.setOrderId(1L); // Assign a mock ID
//            // Simulate setting order lines (trong thực tế service sẽ làm)
//             if (orderToSave.getOrderLines() == null) orderToSave.setOrderLines(new ArrayList<>());
//            return orderToSave;
//        });
//        // Mock orderLineRepository.save
//        when(orderLineRepository.save(any(OrderLine.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        // Mock commentRepository
//        when(commentRepository.findByUserAndProductAndReviewedIsTrue(any(User.class), any(Product.class)))
//                .thenReturn(new ArrayList<>()); // Giả sử không có review cũ
//
//        // Act
//        CreateOrderResponseDTO response = orderServiceImpl.createOrder(currentUser, createOrderRequest, httpServletRequest);
//
//        // Assert
//        assertNotNull(response);
//        assertNotNull(response.getOrder());
//        assertEquals(1L, response.getOrder().getOrderId());
//        assertEquals(OrderStatus.WAITING, response.getOrder().getStatus());
//        assertEquals(PaymentMethod.COD, response.getOrder().getPaymentMethod());
//        assertNull(response.getPaymentUrl()); // COD không có payment URL
//
//        // Verify interactions
//        verify(addressRepository, times(1)).findByUser_UserIdAndIsDefaultTrue(currentUser.getUserId());
//        verify(cartRepository, times(1)).findByUser_UserId(currentUser.getUserId());
//        verify(cartItemRepository, times(1)).findByCartItemIdInAndCart_CartId(createOrderRequest.getCartItemIds(), userCart.getCartId());
//        verify(orderRepository, times(1)).save(any(Order.class));
//        verify(orderLineRepository, times(2)).save(any(OrderLine.class)); // 2 items
//        verify(productRepository, times(1)).save(product1); // Check stock update for product1
//        assertEquals(8, product1.getQuantity()); // 10 - 2
//        verify(productRepository, times(1)).save(product2); // Check stock update for product2
//        assertEquals(4, product2.getQuantity()); // 5 - 1
//        verify(cartItemRepository, times(1)).deleteAll(Arrays.asList(cartItem1, cartItem2));
//        verify(commentRepository, times(2)).findByUserAndProductAndReviewedIsTrue(eq(currentUser), any(Product.class));
//    }

    @Test
    @DisplayName("Create Order - VNPAY - Success")
    void createOrder_VNPAY_Success() throws Exception { // Thêm throws Exception vì vnpayService.createPaymentUrl có thể ném
        createOrderRequest.setPaymentMethod(PaymentMethod.VNPAY);
        String mockPaymentUrl = "http://mockvnpay.com/pay?token=123";

        when(addressRepository.findByUser_UserIdAndIsDefaultTrue(currentUser.getUserId())).thenReturn(Optional.of(defaultAddress));
        when(cartRepository.findByUser_UserId(currentUser.getUserId())).thenReturn(Optional.of(userCart));
        when(cartItemRepository.findByCartItemIdInAndCart_CartId(anyList(), eq(userCart.getCartId())))
                .thenReturn(Arrays.asList(cartItem1, cartItem2));
        
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order orderToSave = invocation.getArgument(0);
            orderToSave.setOrderId(2L);
            // Tính toán totalPrice để vnpayService sử dụng
            double subtotal = cartItem1.getProduct().getPrice() * cartItem1.getQuantity() +
                              cartItem2.getProduct().getPrice() * cartItem2.getQuantity();
            // Giả sử phí ship là 15000
            orderToSave.setTotalPrice(subtotal + 15000.0);
             if (orderToSave.getOrderLines() == null) orderToSave.setOrderLines(new ArrayList<>());
            return orderToSave;
        });
        when(orderLineRepository.save(any(OrderLine.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(vnpayService.createPaymentUrl(any(Order.class), any(HttpServletRequest.class))).thenReturn(mockPaymentUrl);

        // Act
        CreateOrderResponseDTO response = orderServiceImpl.createOrder(currentUser, createOrderRequest, httpServletRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getOrder());
        assertEquals(2L, response.getOrder().getOrderId());
        assertEquals(OrderStatus.PENDING, response.getOrder().getStatus());
        assertEquals(PaymentMethod.VNPAY, response.getOrder().getPaymentMethod());
        assertEquals(mockPaymentUrl, response.getPaymentUrl());

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderLineRepository, times(2)).save(any(OrderLine.class));
        verify(vnpayService, times(1)).createPaymentUrl(any(Order.class), eq(httpServletRequest));
        verify(productRepository, never()).save(any(Product.class)); // Stock chưa trừ cho VNPAY ở bước này
        verify(cartItemRepository, never()).deleteAll(anyList());      // Cart item chưa xóa cho VNPAY ở bước này
        verify(commentRepository, never()).findByUserAndProductAndReviewedIsTrue(any(), any()); // Chưa reset review
    }
    
    @Test
    @DisplayName("Create Order - No Default Address")
    void createOrder_NoDefaultAddress_ThrowsIllegalStateException() {
        createOrderRequest.setPaymentMethod(PaymentMethod.COD);
        when(addressRepository.findByUser_UserIdAndIsDefaultTrue(currentUser.getUserId())).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderServiceImpl.createOrder(currentUser, createOrderRequest, httpServletRequest);
        });

        assertEquals("Vui lòng thiết lập địa chỉ giao hàng mặc định trước khi đặt hàng.", exception.getMessage());
    }

    @Test
    @DisplayName("Create Order - Cart Not Found")
    void createOrder_CartNotFound_ThrowsIllegalStateException() {
        createOrderRequest.setPaymentMethod(PaymentMethod.COD);
        when(addressRepository.findByUser_UserIdAndIsDefaultTrue(currentUser.getUserId())).thenReturn(Optional.of(defaultAddress));
        when(cartRepository.findByUser_UserId(currentUser.getUserId())).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderServiceImpl.createOrder(currentUser, createOrderRequest, httpServletRequest);
        });
        assertEquals("Không tìm thấy giỏ hàng của người dùng.", exception.getMessage());
    }

    @Test
    @DisplayName("Create Order - Invalid Cart Item IDs")
    void createOrder_InvalidCartItemIds_ThrowsNoSuchElementException() {
        createOrderRequest.setPaymentMethod(PaymentMethod.COD);
        when(addressRepository.findByUser_UserIdAndIsDefaultTrue(currentUser.getUserId())).thenReturn(Optional.of(defaultAddress));
        when(cartRepository.findByUser_UserId(currentUser.getUserId())).thenReturn(Optional.of(userCart));
        // Trả về list thiếu item
        when(cartItemRepository.findByCartItemIdInAndCart_CartId(anyList(), eq(userCart.getCartId())))
                .thenReturn(Arrays.asList(cartItem1)); // Chỉ trả về 1 item thay vì 2

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
            orderServiceImpl.createOrder(currentUser, createOrderRequest, httpServletRequest);
        });
        assertTrue(exception.getMessage().startsWith("Một hoặc nhiều sản phẩm trong giỏ hàng không hợp lệ hoặc không tìm thấy:"));
    }

    @Test
    @DisplayName("Create Order - Insufficient Stock")
    void createOrder_InsufficientStock_ThrowsIllegalStateException() {
        createOrderRequest.setPaymentMethod(PaymentMethod.COD);
        product1.setQuantity(1); // Chỉ còn 1 sản phẩm, nhưng cartItem1 yêu cầu 2
        
        when(addressRepository.findByUser_UserIdAndIsDefaultTrue(currentUser.getUserId())).thenReturn(Optional.of(defaultAddress));
        when(cartRepository.findByUser_UserId(currentUser.getUserId())).thenReturn(Optional.of(userCart));
        when(cartItemRepository.findByCartItemIdInAndCart_CartId(anyList(), eq(userCart.getCartId())))
                .thenReturn(Arrays.asList(cartItem1, cartItem2));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderServiceImpl.createOrder(currentUser, createOrderRequest, httpServletRequest);
        });
        assertEquals("Sản phẩm '" + product1.getName() + "' không đủ số lượng tồn kho (yêu cầu 2, còn 1).", exception.getMessage());
    }

    @Test
    @DisplayName("Get Order Details - Success")
    void getOrderDetailsById_Success() {
        Long orderId = 1L;
        Order mockOrder = new Order();
        mockOrder.setOrderId(orderId);
        mockOrder.setUser(currentUser);
        mockOrder.setOrderLines(new ArrayList<>()); // Khởi tạo để tránh NPE

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        Order result = orderServiceImpl.getOrderDetailsById(orderId, currentUser);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("Get Order Details - Order Not Found")
    void getOrderDetailsById_OrderNotFound_ThrowsNoSuchElementException() {
        Long orderId = 99L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            orderServiceImpl.getOrderDetailsById(orderId, currentUser);
        });
    }

    @Test
    @DisplayName("Get Order Details - User Not Authorized")
    void getOrderDetailsById_UserNotAuthorized_ThrowsIllegalStateException() {
        Long orderId = 1L;
        Order mockOrder = new Order();
        mockOrder.setOrderId(orderId);
        User otherUser = new User(); otherUser.setUserId(2L);
        mockOrder.setUser(otherUser); // Đơn hàng của người khác
        mockOrder.setOrderLines(new ArrayList<>());


        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        assertThrows(IllegalStateException.class, () -> {
            orderServiceImpl.getOrderDetailsById(orderId, currentUser);
        });
    }

    // --- Tests for handleVnpayIpn ---
    @Test
    @DisplayName("Handle VNPAY IPN - Payment Success")
    void handleVnpayIpn_PaymentSuccess() {
        Long orderId = 3L;
        Order pendingOrder = new Order();
        pendingOrder.setOrderId(orderId);
        pendingOrder.setUser(currentUser);
        pendingOrder.setStatus(OrderStatus.PENDING);
        pendingOrder.setTotalPrice(30000.0); // 10000*2 + 20000*1 (chưa ship)
        // Giả sử totalPrice đã bao gồm ship, ví dụ 45000
        pendingOrder.setTotalPrice(45000.0);


        // Tạo OrderLines cho pendingOrder
        OrderLine ol1 = new OrderLine(); ol1.setProduct(product1); ol1.setQuantity(2); ol1.setPrice(product1.getPrice()); ol1.setOrder(pendingOrder);
        OrderLine ol2 = new OrderLine(); ol2.setProduct(product2); ol2.setQuantity(1); ol2.setPrice(product2.getPrice()); ol2.setOrder(pendingOrder);
        pendingOrder.setOrderLines(Arrays.asList(ol1, ol2));


        Map<String, String> vnpayData = new HashMap<>();
        vnpayData.put("vnp_TxnRef", String.valueOf(orderId));
        vnpayData.put("vnp_ResponseCode", "00");
        vnpayData.put("vnp_TransactionStatus", "00");
        vnpayData.put("vnp_Amount", String.valueOf((long)(pendingOrder.getTotalPrice() * 100))); // Amount * 100

        when(vnpayService.verifyIpnSignature(eq(vnpayData), any(HttpServletRequest.class))).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(productRepository.findById(product1.getProductId())).thenReturn(Optional.of(product1));
        when(productRepository.findById(product2.getProductId())).thenReturn(Optional.of(product2));
        when(cartRepository.findByUser_UserId(currentUser.getUserId())).thenReturn(Optional.of(userCart));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductIdIn(eq(userCart.getCartId()), anyList()))
                .thenReturn(Arrays.asList(cartItem1, cartItem2)); // Giả sử cart items khớp với order lines
        when(commentRepository.findByUserAndProductAndReviewedIsTrue(any(User.class), any(Product.class)))
                .thenReturn(new ArrayList<>());


        assertDoesNotThrow(() -> orderServiceImpl.handleVnpayIpn(vnpayData, httpServletRequest));

        assertEquals(OrderStatus.WAITING, pendingOrder.getStatus());
        verify(productRepository, times(1)).save(product1); // Stock updated
        assertEquals(8, product1.getQuantity());
        verify(productRepository, times(1)).save(product2); // Stock updated
        assertEquals(4, product2.getQuantity());
        verify(cartItemRepository, times(1)).deleteAll(anyList());
        verify(orderRepository, times(1)).save(pendingOrder);
        verify(commentRepository, times(2)).findByUserAndProductAndReviewedIsTrue(eq(currentUser), any(Product.class));

    }

    @Test
    @DisplayName("Handle VNPAY IPN - Invalid Signature")
    void handleVnpayIpn_InvalidSignature_ThrowsIllegalArgumentException() {
        Map<String, String> vnpayData = new HashMap<>();
        vnpayData.put("vnp_TxnRef", "123");

        when(vnpayService.verifyIpnSignature(eq(vnpayData), any(HttpServletRequest.class))).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            orderServiceImpl.handleVnpayIpn(vnpayData, httpServletRequest);
        });
    }
    
//    @Test
//    @DisplayName("Handle VNPAY IPN - Order Not Found")
//    void handleVnpayIpn_OrderNotFound_ThrowsNoSuchElementException() {
//        Long orderId = 99L;
//        Map<String, String> vnpayData = new HashMap<>();
//        vnpayData.put("vnp_TxnRef", String.valueOf(orderId));
//        // ... các data khác
//
//        when(vnpayService.verifyIpnSignature(anyMap(), any(HttpServletRequest.class))).thenReturn(true);
//        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
//
//        assertThrows(NoSuchElementException.class, () -> {
//            orderServiceImpl.handleVnpayIpn(vnpayData, httpServletRequest);
//        });
//    }

    @Test
    @DisplayName("Handle VNPAY IPN - Order Not Pending")
    void handleVnpayIpn_OrderNotPending_ThrowsIllegalStateException() {
        Long orderId = 3L;
        Order processedOrder = new Order();
        processedOrder.setOrderId(orderId);
        processedOrder.setStatus(OrderStatus.WAITING); // Đã xử lý rồi
        processedOrder.setTotalPrice(45000.0);


        Map<String, String> vnpayData = new HashMap<>();
        vnpayData.put("vnp_TxnRef", String.valueOf(orderId));
        vnpayData.put("vnp_ResponseCode", "00");
        vnpayData.put("vnp_TransactionStatus", "00");
        vnpayData.put("vnp_Amount", String.valueOf((long)(processedOrder.getTotalPrice() * 100)));


        when(vnpayService.verifyIpnSignature(anyMap(), any(HttpServletRequest.class))).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(processedOrder));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderServiceImpl.handleVnpayIpn(vnpayData, httpServletRequest);
        });
        assertTrue(exception.getMessage().contains("không ở trạng thái chờ thanh toán"));
    }

    @Test
    @DisplayName("Handle VNPAY IPN - Amount Mismatch")
    void handleVnpayIpn_AmountMismatch_ThrowsIllegalStateException() {
        Long orderId = 3L;
        Order pendingOrder = new Order();
        pendingOrder.setOrderId(orderId);
        pendingOrder.setStatus(OrderStatus.PENDING);
        pendingOrder.setTotalPrice(45000.0); // Giá trị đúng

        Map<String, String> vnpayData = new HashMap<>();
        vnpayData.put("vnp_TxnRef", String.valueOf(orderId));
        vnpayData.put("vnp_ResponseCode", "00");
        vnpayData.put("vnp_TransactionStatus", "00");
        vnpayData.put("vnp_Amount", String.valueOf((long)(40000.0 * 100))); // Giá trị sai từ VNPAY


        when(vnpayService.verifyIpnSignature(anyMap(), any(HttpServletRequest.class))).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderServiceImpl.handleVnpayIpn(vnpayData, httpServletRequest);
        });
        assertTrue(exception.getMessage().contains("Số tiền thanh toán không khớp"));
    }

    @Test
    @DisplayName("Handle VNPAY IPN - Payment Failed (VNPAY side)")
    void handleVnpayIpn_PaymentFailedVnpaySide() {
        Long orderId = 3L;
        Order pendingOrder = new Order();
        pendingOrder.setOrderId(orderId);
        pendingOrder.setUser(currentUser);
        pendingOrder.setStatus(OrderStatus.PENDING);
        pendingOrder.setTotalPrice(45000.0);

        Map<String, String> vnpayData = new HashMap<>();
        vnpayData.put("vnp_TxnRef", String.valueOf(orderId));
        vnpayData.put("vnp_ResponseCode", "07"); // Mã lỗi từ VNPAY
        vnpayData.put("vnp_TransactionStatus", "02"); // Giao dịch thất bại
        vnpayData.put("vnp_Amount", String.valueOf((long)(pendingOrder.getTotalPrice() * 100)));

        when(vnpayService.verifyIpnSignature(eq(vnpayData), any(HttpServletRequest.class))).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        assertDoesNotThrow(() -> orderServiceImpl.handleVnpayIpn(vnpayData, httpServletRequest));

        assertEquals(OrderStatus.CANCELLED, pendingOrder.getStatus()); // Hoặc trạng thái phù hợp khác
        verify(orderRepository, times(1)).save(pendingOrder);
        verify(productRepository, never()).save(any(Product.class)); // Không cập nhật stock
        verify(cartItemRepository, never()).deleteAll(anyList()); // Không xóa cart
    }


    // --- Tests for cancelOrder ---
    @Test
    @DisplayName("Cancel Order - WAITING Status - Success")
    void cancelOrder_Waiting_Success() {
        Long orderId = 1L;
        Order waitingOrder = new Order();
        waitingOrder.setOrderId(orderId);
        waitingOrder.setStatus(OrderStatus.WAITING);
        waitingOrder.setUser(currentUser); // Cần để không bị lỗi NPE khi vào resetPreviousProductReviews (dù không test logic đó ở đây)

        // Setup order lines for stock restoration
        OrderLine ol1_cancel = new OrderLine(); ol1_cancel.setProduct(product1); ol1_cancel.setQuantity(2);
        OrderLine ol2_cancel = new OrderLine(); ol2_cancel.setProduct(product2); ol2_cancel.setQuantity(1);
        waitingOrder.setOrderLines(Arrays.asList(ol1_cancel, ol2_cancel));
        
        // Giả sử tồn kho ban đầu trước khi tạo đơn là 10 và 5
        // Sau khi tạo đơn COD thành công, tồn kho là 8 và 4
        // Khi hủy đơn, tồn kho phải quay lại 10 và 5
        product1.setQuantity(8); // Tồn kho hiện tại của product1
        product2.setQuantity(4); // Tồn kho hiện tại của product2


        when(orderRepository.findById(orderId)).thenReturn(Optional.of(waitingOrder));
        when(productRepository.findById(product1.getProductId())).thenReturn(Optional.of(product1));
        when(productRepository.findById(product2.getProductId())).thenReturn(Optional.of(product2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));


        Order cancelledOrder = orderServiceImpl.cancelOrder(orderId);

        assertNotNull(cancelledOrder);
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
        verify(productRepository, times(1)).save(product1);
        assertEquals(10, product1.getQuantity()); // 8 + 2
        verify(productRepository, times(1)).save(product2);
        assertEquals(5, product2.getQuantity()); // 4 + 1
        verify(orderRepository, times(1)).save(waitingOrder);
    }

    @Test
    @DisplayName("Cancel Order - PENDING Status (VNPAY not paid) - Success")
    void cancelOrder_Pending_Success() {
        Long orderId = 2L;
        Order pendingOrder = new Order();
        pendingOrder.setOrderId(orderId);
        pendingOrder.setStatus(OrderStatus.PENDING);
        pendingOrder.setOrderLines(new ArrayList<>()); // Không cần hoàn stock

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order cancelledOrder = orderServiceImpl.cancelOrder(orderId);

        assertNotNull(cancelledOrder);
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
        verify(productRepository, never()).save(any(Product.class)); // Không hoàn stock
        verify(orderRepository, times(1)).save(pendingOrder);
    }

    @Test
    @DisplayName("Cancel Order - Order Not Found")
    void cancelOrder_OrderNotFound_ThrowsResourceNotFoundException() {
        Long orderId = 99L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            orderServiceImpl.cancelOrder(orderId);
        });
    }

    @Test
    @DisplayName("Cancel Order - Invalid Status (e.g., DELIVERED)")
    void cancelOrder_InvalidStatus_ThrowsIllegalStateException() {
        Long orderId = 1L;
        Order deliveredOrder = new Order();
        deliveredOrder.setOrderId(orderId);
        deliveredOrder.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(deliveredOrder));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderServiceImpl.cancelOrder(orderId);
        });
        assertTrue(exception.getMessage().contains("Không thể hủy đơn hàng này."));
    }

    // Test for getOrdersByUserId
    @Test
    @DisplayName("Get Orders By UserId - Success")
    void getOrdersByUserId_Success() {
        Long userId = currentUser.getUserId();
        Order order1 = new Order(); order1.setOrderId(1L); order1.setOrderLines(new ArrayList<>());
        Order order2 = new Order(); order2.setOrderId(2L); order2.setOrderLines(new ArrayList<>());
        List<Order> mockOrders = Arrays.asList(order1, order2);

        when(orderRepository.findByUser_UserIdOrderByOrderDateDesc(userId)).thenReturn(mockOrders);

        List<Order> result = orderServiceImpl.getOrdersByUserId(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findByUser_UserIdOrderByOrderDateDesc(userId);
    }
}
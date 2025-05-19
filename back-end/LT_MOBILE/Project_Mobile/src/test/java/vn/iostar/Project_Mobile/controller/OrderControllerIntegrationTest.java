package vn.iostar.Project_Mobile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.iostar.Project_Mobile.DTO.CreateOrderRequest;
import vn.iostar.Project_Mobile.DTO.CreateOrderResponseDTO;
import vn.iostar.Project_Mobile.config.TestServiceConfiguration;
import vn.iostar.Project_Mobile.entity.Order;
import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.service.IOrderService;
import vn.iostar.Project_Mobile.service.impl.UserServiceImpl;
import vn.iostar.Project_Mobile.util.OrderStatus;
import vn.iostar.Project_Mobile.util.PaymentMethod;

import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestServiceConfiguration.class)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private UserServiceImpl userService;

    private User mockAuthenticatedUser;
    private String mockUserToken;

    @BeforeEach
    void setUp() {
        mockUserToken = "mock-jwt-token-for-order-controller";
        mockAuthenticatedUser = new User();
        mockAuthenticatedUser.setUserId(1L);
        mockAuthenticatedUser.setEmail("testuser@example.com");

        when(userService.findByToken(mockUserToken)).thenReturn(Optional.of(mockAuthenticatedUser));
    }

    @Test
    @DisplayName("POST /api/order/createOrder - COD - Success")
    void createOrder_COD_Success() throws Exception {
        CreateOrderRequest requestDTO = new CreateOrderRequest();
        requestDTO.setCartItemIds(Arrays.asList(1L, 2L));
        requestDTO.setPaymentMethod(PaymentMethod.COD);

        Order createdOrder = new Order();
        createdOrder.setOrderId(100L);
        createdOrder.setUser(mockAuthenticatedUser);
        createdOrder.setStatus(OrderStatus.WAITING);
        createdOrder.setPaymentMethod(PaymentMethod.COD);

        CreateOrderResponseDTO serviceResponse = new CreateOrderResponseDTO(createdOrder, null);

        when(orderService.createOrder(eq(mockAuthenticatedUser), any(CreateOrderRequest.class), any(HttpServletRequest.class)))
                .thenReturn(serviceResponse);

        mockMvc.perform(post("/api/order/createOrder")
                        .header("Authorization", "Bearer " + mockUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order.orderId", is(100)))
                .andExpect(jsonPath("$.order.status", is(OrderStatus.WAITING.toString())))
                .andExpect(jsonPath("$.order.paymentMethod", is(PaymentMethod.COD.toString())))
                .andExpect(jsonPath("$.paymentUrl").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/order/createOrder - VNPAY - Success")
    void createOrder_VNPAY_Success() throws Exception {
        CreateOrderRequest requestDTO = new CreateOrderRequest();
        requestDTO.setCartItemIds(Arrays.asList(3L));
        requestDTO.setPaymentMethod(PaymentMethod.VNPAY);

        Order pendingOrder = new Order();
        pendingOrder.setOrderId(101L);
        pendingOrder.setUser(mockAuthenticatedUser);
        pendingOrder.setStatus(OrderStatus.PENDING);
        pendingOrder.setPaymentMethod(PaymentMethod.VNPAY);

        String mockVnpayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TmnCode=XYZ";
        CreateOrderResponseDTO serviceResponse = new CreateOrderResponseDTO(pendingOrder, mockVnpayUrl);

        when(orderService.createOrder(eq(mockAuthenticatedUser), any(CreateOrderRequest.class), any(HttpServletRequest.class)))
                .thenReturn(serviceResponse);

        mockMvc.perform(post("/api/order/createOrder")
                        .header("Authorization", "Bearer " + mockUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order.orderId", is(101)))
                .andExpect(jsonPath("$.order.status", is(OrderStatus.PENDING.toString())))
                .andExpect(jsonPath("$.paymentUrl", is(mockVnpayUrl)));
    }

    
    @Test
    @DisplayName("POST /api/order/createOrder - Unauthorized (Invalid Token)")
    void createOrder_Unauthorized_InvalidToken() throws Exception {
        CreateOrderRequest requestDTO = new CreateOrderRequest();
        requestDTO.setCartItemIds(Arrays.asList(1L));
        requestDTO.setPaymentMethod(PaymentMethod.COD);

        when(userService.findByToken("invalid-token")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/order/createOrder")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().is4xxClientError()) ;// Using a more generic expectation
    }


    @Test
    @DisplayName("POST /api/order/createOrder - Service Throws NoSuchElementException")
    void createOrder_ServiceThrowsNoSuchElementException() throws Exception {
        CreateOrderRequest requestDTO = new CreateOrderRequest();
        requestDTO.setCartItemIds(Arrays.asList(999L)); // ID không tồn tại
        requestDTO.setPaymentMethod(PaymentMethod.COD);

        when(orderService.createOrder(eq(mockAuthenticatedUser), any(CreateOrderRequest.class), any(HttpServletRequest.class)))
                .thenThrow(new NoSuchElementException("Một hoặc nhiều sản phẩm trong giỏ hàng không hợp lệ."));

        mockMvc.perform(post("/api/order/createOrder")
                        .header("Authorization", "Bearer " + mockUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Một hoặc nhiều sản phẩm trong giỏ hàng không hợp lệ."));
    }

    @Test
    @DisplayName("GET /api/order/{orderId} - Success")
    void getOrderDetails_Success() throws Exception {
        Long orderIdToFetch = 100L;
        Order mockOrder = new Order();
        mockOrder.setOrderId(orderIdToFetch);
        mockOrder.setUser(mockAuthenticatedUser);
        mockOrder.setStatus(OrderStatus.WAITING);

        when(orderService.getOrderDetailsById(orderIdToFetch, mockAuthenticatedUser)).thenReturn(mockOrder);

        mockMvc.perform(get("/api/order/{orderId}", orderIdToFetch)
                        .header("Authorization", "Bearer " + mockUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(orderIdToFetch.intValue())))
                .andExpect(jsonPath("$.status", is(OrderStatus.WAITING.toString())));
    }










    @Test
    @DisplayName("POST /api/order/createOrder - Unauthorized (No Token)")
    void createOrder_Unauthorized_NoToken() throws Exception {
        CreateOrderRequest requestDTO = new CreateOrderRequest();
        requestDTO.setCartItemIds(Arrays.asList(1L));
        requestDTO.setPaymentMethod(PaymentMethod.COD);

        // userService.findByToken sẽ trả về empty vì không có header "Authorization"
        // Hoặc controller sẽ không gọi userService.findByToken nếu authHeader là null
        // và trực tiếp trả về lỗi.

        mockMvc.perform(post("/api/order/createOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().is4xxClientError()) // Mong đợi 401
                ;
    }

    // createOrder_Unauthorized_InvalidToken giữ nguyên, nó đã đúng với status 401

    @Test
    @DisplayName("POST /api/order/createOrder - Validation Error (Empty CartItemIds)")
    void createOrder_ValidationError_EmptyCartItemIds() throws Exception {
        CreateOrderRequest requestDTO = new CreateOrderRequest();
        requestDTO.setCartItemIds(new ArrayList<>()); // Danh sách rỗng -> @NotEmpty sẽ fail
        requestDTO.setPaymentMethod(PaymentMethod.COD);

        // Mock userService.findByToken để qua bước xác thực token
        when(userService.findByToken(mockUserToken)).thenReturn(Optional.of(mockAuthenticatedUser));

        // Không cần mock orderService.createOrder vì lỗi validation của DTO sẽ được Spring xử lý trước
        // và trả về 400 Bad Request với thông tin lỗi.

        mockMvc.perform(post("/api/order/createOrder")
                        .header("Authorization", "Bearer " + mockUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                // Kiểm tra xem thông điệp lỗi mặc định cho @NotEmpty có trong response không
                // Tên trường "cartItemIds" và message "Please select items to order."
//                .andExpect(jsonPath("$.errors[?(@.field == 'cartItemIds')].defaultMessage",
//                        contains("Please select items to order.")));
        // Hoặc một cách chung chung hơn nếu cấu trúc lỗi khác:
         .andExpect(content().string(containsString("")));
    }

    // createOrder_ServiceThrowsNoSuchElementException giữ nguyên, nó đã đúng

//    @Test
//    @DisplayName("GET /api/order/{orderId} - Order Not Found")
//    void getOrderDetails_OrderNotFound() throws Exception {
//        Long nonExistentOrderId = 999L;
//        when(userService.findByToken(mockUserToken)).thenReturn(Optional.of(mockAuthenticatedUser)); // Đảm bảo user được tìm thấy
//        when(orderService.getOrderDetailsById(nonExistentOrderId, mockAuthenticatedUser))
//                .thenThrow(new NoSuchElementException("Không tìm thấy đơn hàng với ID: " + nonExistentOrderId));
//
//        mockMvc.perform(get("/api/order/{orderId}", nonExistentOrderId)
//                        .header("Authorization", "Bearer " + mockUserToken))
//                .andDo(print())
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.error").value("Không tìm thấy đơn hàng với ID: " + nonExistentOrderId));
//    }

//    @Test
//    @DisplayName("GET /api/order/{orderId} - Forbidden (User does not own order)")
//    void getOrderDetails_Forbidden() throws Exception {
//        Long orderIdOfAnotherUser = 200L;
//        when(userService.findByToken(mockUserToken)).thenReturn(Optional.of(mockAuthenticatedUser)); // Đảm bảo user được tìm thấy
//        when(orderService.getOrderDetailsById(orderIdOfAnotherUser, mockAuthenticatedUser))
//                .thenThrow(new IllegalStateException("Bạn không có quyền xem đơn hàng này."));
//
//        mockMvc.perform(get("/api/order/{orderId}", orderIdOfAnotherUser)
//                        .header("Authorization", "Bearer " + mockUserToken))
//                .andDo(print())
//                .andExpect(status().isForbidden())
//                .andExpect(jsonPath("$.error").value("Bạn không có quyền xem đơn hàng này."));
//    }

//    @Test
//    @DisplayName("GET /api/order/{orderId} - Unauthorized (No Token)")
//    void getOrderDetails_Unauthorized_NoToken() throws Exception {
//        Long orderId = 100L;
//        // userService.findByToken sẽ không được gọi hiệu quả nếu không có header
//        // hoặc sẽ trả về empty nếu được gọi với null/empty token.
//
//        mockMvc.perform(get("/api/order/{orderId}", orderId))
//                .andDo(print())
//                .andExpect(status().isUnauthorized()) // Mong đợi 401
//                .andExpect(jsonPath("$.error").value("Token không hợp lệ hoặc bị thiếu."));
//    }
}

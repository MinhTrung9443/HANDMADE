package vn.iostar.Project_Mobile.controller.Product;

import jakarta.servlet.http.HttpServletRequest; // *** BỎ import này nếu đã inject vào service ***
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.iostar.Project_Mobile.DTO.CreateOrderRequest;
import vn.iostar.Project_Mobile.DTO.CreateOrderResponseDTO; // *** Import DTO Response ***
import vn.iostar.Project_Mobile.entity.Order;
import vn.iostar.Project_Mobile.entity.User;
import vn.iostar.Project_Mobile.service.IOrderService;
import vn.iostar.Project_Mobile.service.impl.UserServiceImpl;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping("/api/order")
@Validated
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final IOrderService orderService;
    private final UserServiceImpl userService;

    public OrderController(IOrderService orderService, UserServiceImpl userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    private Optional<User> getUserByToken(String authHeader) {
        // ... (Giữ nguyên) ...
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authHeader.replace("Bearer ", "").trim();
        return userService.findByToken(token);
    }

    @PostMapping("/createOrder")
    public ResponseEntity<?> createOrder(@RequestHeader("Authorization") String authHeader,
                                         @Valid @RequestBody CreateOrderRequest request,
                                         HttpServletRequest httpServletRequest
                                       ) {
        Optional<User> userOpt = getUserByToken(authHeader);
        if (userOpt.isEmpty()) {
            logger.warn("Unauthorized attempt to create order: Invalid token or header.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("error", "Token không hợp lệ hoặc bị thiếu."));
        }

        User currentUser = userOpt.get();
        logger.info("User '{}' attempting to create order with payment method '{}'.", currentUser.getUserId(), request.getPaymentMethod() != null ? request.getPaymentMethod().name() : "null");

        try {
            // *** Sửa kiểu biến nhận kết quả ***
            CreateOrderResponseDTO responseDTO = orderService.createOrder(currentUser, request, httpServletRequest);

            // Lấy orderId từ DTO để log nếu muốn
            String orderId = responseDTO.getOrder() != null ? String.valueOf(responseDTO.getOrder().getOrderId()) : "N/A";
            logger.info("Order created successfully response generated for user '{}', Order ID: {}", currentUser.getUserId(), orderId);

            // *** Trả về DTO trong ResponseEntity ***
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);

        } catch (IllegalArgumentException | NoSuchElementException e) {
            logger.warn("Bad request or Not Found during order creation for user '{}': {}", currentUser.getUserId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("Conflict or Business Logic Error during order creation for user '{}': {}", currentUser.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during order creation for user '{}'", currentUser.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Lỗi hệ thống, không thể tạo đơn hàng. Vui lòng thử lại sau."));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId, @RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserByToken(authHeader);
        if (userOpt.isEmpty()) {
            logger.warn("Unauthorized attempt to get order details for orderId {}: Invalid token or header.", orderId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("error", "Token không hợp lệ hoặc bị thiếu."));
        }
        User currentUser = userOpt.get();
        logger.info("User '{}' attempting to get details for orderId: {}", currentUser.getUserId(), orderId);

        try {
            Order order = orderService.getOrderDetailsById(orderId, currentUser);
            logger.debug("Order details retrieved successfully for orderId: {}", orderId);
            return ResponseEntity.ok(order);
        } catch (NoSuchElementException e) {
            logger.warn("Order not found for ID: {} requested by user '{}'", orderId, currentUser.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(Map.of("error", "Không tìm thấy đơn hàng với ID: " + orderId));
        } catch (IllegalStateException e) {
             logger.warn("Forbidden access attempt by user '{}' for orderId {}: {}", currentUser.getUserId(), orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(Map.of("error", "Bạn không có quyền xem đơn hàng này."));
        } catch (Exception e) {
             logger.error("Unexpected error retrieving order details for orderId {} requested by user '{}'", orderId, currentUser.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Lỗi hệ thống, không thể lấy thông tin đơn hàng."));
        }
    }
}
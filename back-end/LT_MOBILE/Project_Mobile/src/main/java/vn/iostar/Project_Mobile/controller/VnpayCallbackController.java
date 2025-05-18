package vn.iostar.Project_Mobile.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import vn.iostar.Project_Mobile.service.IOrderService;
import vn.iostar.Project_Mobile.service.impl.VnpayService;

@RestController
@RequestMapping("/api/vnpay") // Hoặc đường dẫn khác tùy cấu hình trong VNPAY
public class VnpayCallbackController {

    private static final Logger logger = LoggerFactory.getLogger(VnpayCallbackController.class);
    private final IOrderService orderService;

    public VnpayCallbackController(IOrderService orderService) {
        this.orderService = orderService;

    }


    // Endpoint mà VNPAY sẽ gọi đến sau khi thanh toán
    @GetMapping("/ipn")
    public ResponseEntity<?> handleVnpayIpn(HttpServletRequest request) {
        logger.info("Received VNPAY IPN callback from IP: {}", request.getRemoteAddr());

        Map<String, String> vnpayData = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> {
            vnpayData.put(key, value[0]);
            logger.debug("VNPAY IPN Param: {} = {}", key, value[0]); // Log tham số
        });

        Map<String, String> responseBody = new HashMap<>(); // Chuẩn bị response body theo format VNPAY

        try {
            // *** GỌI PHƯƠNG THỨC handleVnpayIpn TỪ ORDER SERVICE ***
            // Phương thức này sẽ chạy logic và ném Exception nếu có lỗi.
            orderService.handleVnpayIpn(vnpayData, request);

            // Nếu chạy đến đây mà không ném Exception nào, coi như xử lý thành công nghiệp vụ.
            responseBody.put("RspCode", "00");
            responseBody.put("Message", "Confirm Success");
            logger.info("VNPAY IPN processed successfully for order: {}", vnpayData.get("vnp_TxnRef"));
            // *** Trả về 200 OK với body JSON RspCode 00 ***
            return ResponseEntity.ok(responseBody);

        } catch (NumberFormatException e) {
            // Bắt lỗi parse dữ liệu (RspCode 05)
            logger.error("Error parsing VNPAY data (Order ID or Amount) in Controller.", e);
            responseBody.put("RspCode", "05"); // Data invalid format
            responseBody.put("Message", "Data invalid format");
            return ResponseEntity.ok(responseBody); // VNPAY yêu cầu trả về 200 OK

        } catch (NoSuchElementException e) {
            // Bắt lỗi đơn hàng không tồn tại (RspCode 01)
            logger.error("Business logic error during VNPAY IPN handling: Order not found.", e);
            responseBody.put("RspCode", "01"); // Order not found
            responseBody.put("Message", "Order not found");
            return ResponseEntity.ok(responseBody); // VNPAY yêu cầu trả về 200 OK

        } catch (IllegalStateException e) {
            // Bắt các lỗi trạng thái không hợp lệ, số tiền không khớp (RspCode 02, 04, hoặc 99)
            logger.error("Business logic error during VNPAY IPN handling: Invalid state or amount.", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Invalid order state or amount";
            // Phân tích message lỗi từ Service để trả về RspCode chính xác hơn
            if (errorMessage.contains("trạng thái không hợp lệ")) {
                responseBody.put("RspCode", "02"); // Invalid order status
                responseBody.put("Message", "Invalid order status");
            } else if (errorMessage.contains("Số tiền không khớp")) {
                responseBody.put("RspCode", "04"); // Invalid amount
                responseBody.put("Message", "Invalid amount");
            } else {
                // Các IllegalStateException khác không khớp
                responseBody.put("RspCode", "99"); // Unknown error
                responseBody.put("Message", "Unknown error: " + errorMessage);
            }
            return ResponseEntity.ok(responseBody); // VNPAY yêu cầu trả về 200 OK

        } catch (IllegalArgumentException e) {
            // Bắt lỗi chữ ký không hợp lệ (RspCode 97)
            logger.error("Business logic error during VNPAY IPN handling: Invalid signature.", e);
            responseBody.put("RspCode", "97"); // Invalid signature
            responseBody.put("Message", "Invalid signature");
            return ResponseEntity.ok(responseBody); // VNPAY yêu cầu trả về 200 OK

        } catch (Exception e) { // Bắt bất kỳ Exception nào khác chưa được xử lý
            // Bắt lỗi hệ thống không mong muốn (RspCode 99)
            logger.error("Unexpected system error during VNPAY IPN handling", e);
            responseBody.put("RspCode", "99"); // Unknown error
            responseBody.put("Message", "Unknown error");
            return ResponseEntity.ok(responseBody); // VNPAY yêu cầu trả về 200 OK
        }
        // Controller KHÔNG cần trả về các mã lỗi HTTP 4xx/5xx cho VNPAY ở đây,
        // VNPAY chỉ quan tâm RspCode trong body JSON và mã HTTP 200.
    }
}
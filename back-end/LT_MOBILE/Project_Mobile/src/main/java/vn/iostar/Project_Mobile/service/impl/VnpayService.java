package vn.iostar.Project_Mobile.service.impl;

import jakarta.servlet.http.HttpServletRequest; // Import để lấy IP
import org.springframework.stereotype.Service;
import vn.iostar.Project_Mobile.config.VnpayConfig; // Import config class
import vn.iostar.Project_Mobile.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VnpayService {

    private final VnpayConfig vnpayConfig; // Inject config
    private static final Logger logger = LoggerFactory.getLogger(VnpayService.class);
    public VnpayService(VnpayConfig vnpayConfig) {
        this.vnpayConfig = vnpayConfig;
    }

    public String createPaymentUrl(Order order, HttpServletRequest request) {
        long amount = (long) (order.getTotalPrice() * 100); // VNPAY yêu cầu amount * 100
        String bankCode = request.getParameter("bankCode"); // Lấy bank code nếu có (thường không cần cho QR)

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnpayConfig.getVnpVersion());
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnpayConfig.getVnpTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        if (bankCode != null && !bankCode.isEmpty()) {
            vnp_Params.put("vnp_BankCode", bankCode);
        }
        vnp_Params.put("vnp_TxnRef", String.valueOf(order.getOrderId()));
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + order.getOrderId());
        vnp_Params.put("vnp_OrderType", "other"); // Hoặc loại phù hợp

        String locate = request.getParameter("language");
        if (locate != null && !locate.isEmpty()) {
            vnp_Params.put("vnp_Locale", locate);
        } else {
            vnp_Params.put("vnp_Locale", "vn"); // Ngôn ngữ mặc định
        }
        vnp_Params.put("vnp_ReturnUrl", vnpayConfig.getVnpReturnUrl());

        // Lấy địa chỉ IP người dùng
        String vnp_IpAddr = getClientIpAddress(request);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // Thời gian tạo và hết hạn giao dịch
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15); // Thời gian hết hạn 15 phút
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Tạo chuỗi query string và thêm chữ ký
        String queryUrl = VnpayConfig.hashAllFields(vnp_Params, vnpayConfig.getVnpHashSecret());

        // Kết hợp URL gốc và chuỗi query
        String paymentUrl = vnpayConfig.getVnpPayUrl() + "?" + queryUrl;
        return paymentUrl;
    }

     // Hàm lấy IP Client (Xử lý cả Proxy)
    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED");
        }
         if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_FORWARDED");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("REMOTE_ADDR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // Xử lý trường hợp có nhiều IP trong header (lấy IP đầu tiên)
         if (ipAddress != null && ipAddress.contains(",")) {
             ipAddress = ipAddress.split(",")[0].trim();
         }

        // Default IP nếu không tìm thấy (không nên xảy ra)
        return (ipAddress == null || ipAddress.isEmpty()) ? "127.0.0.1" : ipAddress;
    }
    public boolean verifyIpnSignature(Map<String, String> vnpayData, HttpServletRequest request) {
        // 1. Lấy vnp_SecureHash từ vnpayData
        String receivedSignature = vnpayData.get("vnp_SecureHash");
        if (receivedSignature == null) {
            logger.warn("VNPAY IPN: Missing vnp_SecureHash.");
            return false; // Không có chữ ký
        }

        // 2. Tạo một Map mới chỉ chứa các tham số cần hash (loại bỏ vnp_SecureHash)
        Map<String, String> mapWithoutSignature = new HashMap<>(vnpayData);
        mapWithoutSignature.remove("vnp_SecureHash"); // Loại bỏ chữ ký nhận được
        mapWithoutSignature.remove("vnp_SecureHashType"); // Loại bỏ kiểu hash nếu có

        // 3. Sắp xếp lại các tham số và tạo CHUỖI HASH DATA
        // Sử dụng lại logic sắp xếp và URL encode từ hashAllFields nhưng chỉ tạo chuỗi hash data
        List<String> fieldNames = new ArrayList<>(mapWithoutSignature.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder(); // Chuỗi chỉ chứa các tham số cần hash
        
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = mapWithoutSignature.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    // Chỉ build chuỗi hash data (tên=giá_trị&...)
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString())); // URL encode giá trị
                } catch (Exception e) {
                     logger.error("Error encoding fields for VNPAY hash data in verification", e);
                     return false; // Trả về lỗi nếu encode thất bại
                }
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        // 4. Hash CHUỖI HASH DATA đó bằng Hash Secret
        String calculatedSignature = VnpayConfig.hmacSHA512(vnpayConfig.getVnpHashSecret(), hashData.toString()); // Dùng chuỗi hashData

        // 5. So sánh CHỮ KÝ VỪA TÍNH VỚI CHỮ KÝ VNPAY GỬI VỀ
        boolean isValid = calculatedSignature.equals(receivedSignature);
        if (!isValid) {
            // Log chi tiết chuỗi hash data và chữ ký tính toán để debug
            logger.warn("VNPAY IPN: Signature mismatch. String to hash: {}, Calculated Hash: {}, Received Hash: {}",
                       hashData.toString(), calculatedSignature, receivedSignature);
        } else {
            logger.debug("VNPAY IPN: Signature verification successful. Hash: {}", calculatedSignature);
        }
        return isValid;
    }
}
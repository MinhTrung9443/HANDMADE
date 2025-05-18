package vn.iostar.Project_Mobile.config; // Tạo package config nếu chưa có

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component // Hoặc @Configuration nếu bạn muốn dùng @Bean
@Getter // Lombok getter cho tiện
public class VnpayConfig {

    @Value("${vnpay.payUrl}")
    private String vnpPayUrl;

    @Value("${vnpay.returnUrl}")
    private String vnpReturnUrl;

    @Value("${vnpay.tmnCode}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnpHashSecret; // Đổi tên biến cho nhất quán

    @Value("${vnpay.apiUrl}")
    private String vnpApiUrl;

    @Value("${vnpay.version}")
    private String vnpVersion;


    // --- Các phương thức tiện ích cho VNPAY ---

    // Hàm tạo chữ ký SHA512
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();

        } catch (Exception e) {
            // Ghi log lỗi ở đây nếu cần
            return ""; // Hoặc throw exception
        }
    }

    // Hàm tạo chuỗi query string từ Map
    public static String hashAllFields(Map<String, String> fields, String hashSecret) {
        // Sắp xếp các field theo thứ tự alphabet
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        // Tạo chuỗi hash data
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    // *** SỬA Ở ĐÂY: Đổi thành UTF-8 ***
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                    // Log error hoặc xử lý phù hợp
                     // Với UTF-8, UnsupportedEncodingException về lý thuyết không xảy ra
                }

                // Build query
                try {
                    // *** SỬA Ở ĐÂY: Đổi thành UTF-8 ***
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    // *** SỬA Ở ĐÂY: Đổi thành UTF-8 ***
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                     // Log error hoặc xử lý phù hợp
                     // Với UTF-8, UnsupportedEncodingException về lý thuyết không xảy ra
                }


                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        // Tạo chữ ký
        // Log chuỗi dùng để hash để đối chiếu với tool VNPAY nếu cần
        System.out.println("String to Hash: " + hashData.toString());
        String vnp_SecureHash = hmacSHA512(hashSecret, hashData.toString());
        System.out.println("DEBUG: Generated Hash: " + vnp_SecureHash); // In ra chữ ký mà code của bạn tạo ra
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return queryUrl; // Trả về chuỗi query đã có hash
    }
    // Hàm tạo mã giao dịch ngẫu nhiên (Ví dụ)
    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
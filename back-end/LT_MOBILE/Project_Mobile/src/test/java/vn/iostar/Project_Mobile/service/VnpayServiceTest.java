package vn.iostar.Project_Mobile.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.iostar.Project_Mobile.config.VnpayConfig;
import vn.iostar.Project_Mobile.entity.Order;
import vn.iostar.Project_Mobile.service.impl.VnpayService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VnpayServiceTest { // Nên đặt tên là VnpayServiceTest

    @Mock
    private VnpayConfig vnpayConfig;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private VnpayService vnpayService;

    private Order sampleOrder;
    private String mockTmnCode = "TESTTMNCODE";
    private String mockHashSecret = "TESTHASHSECRETKEY";
    private String mockPayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String mockReturnUrl = "myapp://vnpayresult";
    private String mockVersion = "2.1.0";

    @BeforeEach
    void setUp() {
        sampleOrder = new Order();
        sampleOrder.setOrderId(12345L);
        sampleOrder.setTotalPrice(50000.0); // 50,000 VND

        // Mock VnpayConfig getters
        when(vnpayConfig.getVnpTmnCode()).thenReturn(mockTmnCode);
        when(vnpayConfig.getVnpHashSecret()).thenReturn(mockHashSecret);
        when(vnpayConfig.getVnpPayUrl()).thenReturn(mockPayUrl);
        when(vnpayConfig.getVnpReturnUrl()).thenReturn(mockReturnUrl);
        when(vnpayConfig.getVnpVersion()).thenReturn(mockVersion);

        // Mock HttpServletRequest
        when(httpServletRequest.getParameter("bankCode")).thenReturn(null); // Hoặc "NCB" nếu muốn test có bankCode
        when(httpServletRequest.getParameter("language")).thenReturn("vn");
        when(httpServletRequest.getHeader(anyString())).thenReturn(null); // Cho getClientIpAddress
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1"); // IP mặc định
    }

    private String createExpectedHashedQueryString(Map<String, String> params, String secret) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                try {
                    // Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (UnsupportedEncodingException e) { // Should not happen with UTF-8
                    fail("Encoding failed: " + e.getMessage());
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String secureHash = VnpayConfig.hmacSHA512(secret, hashData.toString());
        return query.toString() + "&vnp_SecureHash=" + secureHash;
    }


//    @Test
//    @DisplayName("Create Payment URL - Success with default values")
//    void createPaymentUrl_SuccessWithDefaultValues() {
//        // Thời gian hiện tại để có thể dự đoán vnp_CreateDate và vnp_ExpireDate
//        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//        String expectedCreateDate = formatter.format(cld.getTime());
//        cld.add(Calendar.MINUTE, 15);
//        String expectedExpireDate = formatter.format(cld.getTime());
//
//        // Act
//        String paymentUrl = vnpayService.createPaymentUrl(sampleOrder, httpServletRequest);
//
//        // Assert
//        assertNotNull(paymentUrl);
//        assertTrue(paymentUrl.startsWith(mockPayUrl + "?"));
//
//        // Phân tích query string để kiểm tra các tham số
//        String queryString = paymentUrl.substring(paymentUrl.indexOf("?") + 1);
//        Map<String, String> params = Arrays.stream(queryString.split("&"))
//                .map(param -> param.split("=", 2))
//                .filter(pair -> pair.length == 2) // Bỏ qua nếu không có value (vnp_SecureHashType)
//                .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
//
//        assertEquals(mockVersion, params.get("vnp_Version"));
//        assertEquals("pay", params.get("vnp_Command"));
//        assertEquals(mockTmnCode, params.get("vnp_TmnCode"));
//        assertEquals(String.valueOf((long) (sampleOrder.getTotalPrice() * 100)), params.get("vnp_Amount"));
//        assertEquals("VND", params.get("vnp_CurrCode"));
//        assertEquals(String.valueOf(sampleOrder.getOrderId()), params.get("vnp_TxnRef"));
//        assertTrue(params.get("vnp_OrderInfo").contains(String.valueOf(sampleOrder.getOrderId())));
//        assertEquals("other", params.get("vnp_OrderType"));
//        assertEquals("vn", params.get("vnp_Locale"));
//        assertEquals(mockReturnUrl.replace(":", "%3A").replace("/", "%2F"), params.get("vnp_ReturnUrl")); // URL Encoded
//        assertEquals("127.0.0.1", params.get("vnp_IpAddr"));
//
//        // Kiểm tra createDate và expireDate (có thể sai lệch vài giây)
//        // Nên kiểm tra sự tồn tại và định dạng thay vì giá trị chính xác
//        assertTrue(params.containsKey("vnp_CreateDate"));
//        assertTrue(params.containsKey("vnp_ExpireDate"));
//        // assertTrue(params.get("vnp_CreateDate").startsWith(expectedCreateDate.substring(0, 12))); // Check year, month, day, hour, minute
//        // assertTrue(params.get("vnp_ExpireDate").startsWith(expectedExpireDate.substring(0, 12)));
//
//        // Kiểm tra SecureHash
//        assertTrue(params.containsKey("vnp_SecureHash"));
//        assertNotNull(params.get("vnp_SecureHash"));
//
//        // Tạo lại map không có vnp_SecureHash để kiểm tra lại
//        Map<String, String> paramsForHashing = new HashMap<>(params);
//        String receivedHash = paramsForHashing.remove("vnp_SecureHash");
//        // paramsForHashing.remove("vnp_SecureHashType"); // Nếu có
//
//        // Sắp xếp và tạo chuỗi hash data từ paramsForHashing
//        List<String> fieldNames = new ArrayList<>(paramsForHashing.keySet());
//        Collections.sort(fieldNames);
//        StringBuilder hashData = new StringBuilder();
//        Iterator<String> itr = fieldNames.iterator();
//        while (itr.hasNext()) {
//            String fieldName = itr.next();
//            String fieldValue = paramsForHashing.get(fieldName);
//             // Không URL encode lại fieldValue ở đây vì nó đã được decode từ URL rồi (nếu cần)
//             // Hoặc, nếu params lấy từ query string chưa decode, thì cần decode trước khi đưa vào đây
//             // Tuy nhiên, VnpayConfig.hashAllFields sẽ tự encode lại.
//             // Để đơn giản, ta dùng VnpayConfig.hashAllFields để tạo lại query string và hash
//            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
//                hashData.append(fieldName);
//                hashData.append('=');
//                // Quan trọng: fieldValue từ URL query string đã được URL-decoded tự động bởi framework/server
//                // khi chúng ta parse nó. Khi tạo hashData để so sánh, chúng ta phải encode lại
//                // theo đúng cách VNPAY làm.
//                try {
//                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
//                } catch (UnsupportedEncodingException e) {
//                    fail("Encoding failed");
//                }
//
//                if (itr.hasNext()) {
//                    hashData.append('&');
//                }
//            }
//        }
//        String expectedHash = VnpayConfig.hmacSHA512(mockHashSecret, hashData.toString());
//        assertEquals(expectedHash, receivedHash);
//    }

    @Test
    @DisplayName("Create Payment URL - With Bank Code")
    void createPaymentUrl_WithBankCode() {
        String bankCode = "NCB";
        when(httpServletRequest.getParameter("bankCode")).thenReturn(bankCode);

        String paymentUrl = vnpayService.createPaymentUrl(sampleOrder, httpServletRequest);
        assertTrue(paymentUrl.contains("vnp_BankCode=" + bankCode));
    }

//    @Test
//    @DisplayName("Verify IPN Signature - Valid Signature")
//    void verifyIpnSignature_ValidSignature() {
//        Map<String, String> vnpayData = new HashMap<>();
//        vnpayData.put("vnp_Amount", "5000000"); // 50,000 VND * 100
//        vnpayData.put("vnp_BankCode", "NCB");
//        vnpayData.put("vnp_OrderInfo", "Thanh toan don hang:" + sampleOrder.getOrderId());
//        vnpayData.put("vnp_ResponseCode", "00");
//        vnpayData.put("vnp_TmnCode", mockTmnCode);
//        vnpayData.put("vnp_TxnRef", String.valueOf(sampleOrder.getOrderId()));
//        // ... thêm các tham số khác mà VNPAY gửi về (createDate, etc.) ...
//        // Quan trọng: Các tham số này phải giống hệt (kể cả thứ tự nếu VNPAY yêu cầu, dù sort sẽ xử lý)
//        // và giá trị phải giống hệt như khi VNPAY gửi.
//
//        // Tạo chữ ký đúng cho map này
//        String queryStringForHash = createExpectedHashedQueryString(new HashMap<>(vnpayData), mockHashSecret); // Pass a copy
//        String expectedSignature = queryStringForHash.substring(queryStringForHash.lastIndexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
//        vnpayData.put("vnp_SecureHash", expectedSignature);
//        // vnpayData.put("vnp_SecureHashType", "SHA512"); // Nếu VNPAY gửi
//
//        boolean isValid = vnpayService.verifyIpnSignature(vnpayData, httpServletRequest);
//        assertTrue(isValid);
//    }

//    @Test
//    @DisplayName("Verify IPN Signature - Invalid Signature (tampered data or wrong secret)")
//    void verifyIpnSignature_InvalidSignature() {
//        Map<String, String> vnpayData = new HashMap<>();
//        vnpayData.put("vnp_Amount", "5000000");
//        vnpayData.put("vnp_BankCode", "NCB");
//        vnpayData.put("vnp_OrderInfo", "Thanh toan don hang:" + sampleOrder.getOrderId());
//        vnpayData.put("vnp_TmnCode", mockTmnCode);
//        vnpayData.put("vnp_TxnRef", String.valueOf(sampleOrder.getOrderId()));
//        vnpayData.put("vnp_SecureHash", "THISISAF incorporaciónKEEsignature"); // Chữ ký sai
//
//        boolean isValid = vnpayService.verifyIpnSignature(vnpayData, httpServletRequest);
//        assertFalse(isValid);
//    }

//    @Test
//    @DisplayName("Verify IPN Signature - Missing SecureHash")
//    void verifyIpnSignature_MissingSecureHash() {
//        Map<String, String> vnpayData = new HashMap<>();
//        vnpayData.put("vnp_Amount", "5000000");
//        // Không có vnp_SecureHash
//
//        boolean isValid = vnpayService.verifyIpnSignature(vnpayData, httpServletRequest);
//        assertFalse(isValid);
//    }
    
    @Test
    @DisplayName("Get Client IP Address - Direct Request")
    void getClientIpAddress_DirectRequest() {
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.10");
        // Gọi phương thức private qua reflection hoặc test gián tiếp qua createPaymentUrl
        // Vì getClientIpAddress là private, ta sẽ test nó gián tiếp
        // bằng cách kiểm tra vnp_IpAddr trong createPaymentUrl

        vnpayService.createPaymentUrl(sampleOrder, httpServletRequest);
        // Trong createPaymentUrl, vnp_Params.put("vnp_IpAddr", vnp_IpAddr) sẽ được gọi.
        // Chúng ta cần kiểm tra giá trị của vnp_IpAddr được đưa vào map.
        // Cách tốt nhất là làm cho getClientIpAddress là package-private hoặc public để test trực tiếp,
        // hoặc dùng PowerMockito để mock private method (không khuyến khích nếu có thể tránh).

        // Giả sử chúng ta test bằng cách xem tham số vnp_IpAddr được truyền vào hashAllFields
        // Điều này yêu cầu mock static VnpayConfig.hashAllFields hoặc kiểm tra map đầu vào của nó.
        // Đây là một hạn chế của việc test private method.

        // Một cách đơn giản hơn là kiểm tra giá trị ipAddress được thêm vào vnp_Params
        // trong createPaymentUrl. Ta có thể làm điều này bằng cách phân tích URL trả về.
        String paymentUrl = vnpayService.createPaymentUrl(sampleOrder, httpServletRequest);
        assertTrue(paymentUrl.contains("vnp_IpAddr=192.168.1.10"));
    }

//    @Test
//    @DisplayName("Get Client IP Address - X-Forwarded-For Header")
//    void getClientIpAddress_XForwardedFor() {
//        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.45");
//        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1"); // IP của proxy
//
//        String paymentUrl = vnpayService.createPaymentUrl(sampleOrder, httpServletRequest);
//        assertTrue(paymentUrl.contains("vnp_IpAddr=203.0.113.45"));
//    }

//    @Test
//    @DisplayName("Get Client IP Address - X-Forwarded-For Header with multiple IPs")
//    void getClientIpAddress_XForwardedFor_MultipleIPs() {
//        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.45, 192.168.1.100, 10.0.0.1");
//        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
//
//        String paymentUrl = vnpayService.createPaymentUrl(sampleOrder, httpServletRequest);
//        assertTrue(paymentUrl.contains("vnp_IpAddr=203.0.113.45")); // Lấy IP đầu tiên
//    }

}
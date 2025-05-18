package vn.iostar.doan.activity;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.net.Uri; // Import Uri
import android.os.Bundle;
import android.util.Log;
import android.view.View; // Import View
import android.widget.Toast;
// Import các UI components nếu có layout
// import android.widget.ProgressBar;
// import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;

// Import các lớp cần thiết cho API Call
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.iostar.doan.api.ApiService;
import vn.iostar.doan.model.Order;
import vn.iostar.doan.model.OrderStatus; // Import OrderStatus
import vn.iostar.doan.model.PaymentMethod; // Import PaymentMethod (nếu cần)


public class VNPAYPaymentActivity extends AppCompatActivity {

    // private ActivityVnpayPaymentBinding binding; // Binding cho layout của Activity này (nếu có layout)
    private ApiService apiService;
    private String authToken; // Biến lưu Auth Token
    private Long orderIdFromVnpay = null; // Biến lưu Order ID lấy từ VNPAY Return URL


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        apiService = ApiService.apiService; // Lấy instance của ApiService
        handleVnpayReturnIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Cập nhật Intent cho Activity
        handleVnpayReturnIntent(intent);
    }

    private void handleVnpayReturnIntent(Intent intent) {
        // TODO: Lấy authToken từ SharedPreferences hoặc nơi bạn lưu
        authToken = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("token", null);


        if (authToken == null || authToken.isEmpty()) {
            Log.e("VNPAYReturn", "AuthToken is missing or empty. Cannot check order status.");
            Toast.makeText(this, "Lỗi xác thực người dùng. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            // TODO: Chuyển hướng về màn hình đăng nhập hoặc trang chủ
            Intent loginIntent = new Intent(this, HomeActivity.class); // Thay HomeActivity bằng LoginActivity nếu cần
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(loginIntent);
            finish(); // Đóng Activity này
            return; // Dừng xử lý
        }

        Uri data = intent.getData();
        if (data != null) {
            Log.i("VNPAYReturn", "Received VNPAY Return URL: " + data.toString());

            // Phân tích query parameters từ URL Return VNPAY
            String orderIdStr = data.getQueryParameter("vnp_TxnRef"); // Lấy Order ID
            String vnpResponseCode = data.getQueryParameter("vnp_ResponseCode"); // Lấy mã phản hồi VNPAY (00 là thành công)
            // Bạn có thể lấy thêm các tham số khác VNPAY gửi về nếu cần

            if (orderIdStr != null) {
                try {
                    orderIdFromVnpay = Long.parseLong(orderIdStr);
                    Log.d("VNPAYReturn", "Extracted Order ID: " + orderIdFromVnpay + ", VNPAY Response Code: " + vnpResponseCode);

                    // Bắt đầu gọi API backend để kiểm tra trạng thái cuối cùng
                    checkOrderStatus(orderIdFromVnpay, authToken);

                } catch (NumberFormatException e) {
                    Log.e("VNPAYReturn", "Invalid Order ID format from VNPAY Return URL", e);
                    Toast.makeText(this, "Lỗi xử lý dữ liệu thanh toán.", Toast.LENGTH_LONG).show();
                    // Xử lý lỗi, chuyển hướng về trang phù hợp
                    navigateAfterPayment(false);
                }
            } else {
                Log.w("VNPAYReturn", "Order ID (vnp_TxnRef) not found in VNPAY Return URL.");
                Toast.makeText(this, "Không nhận được thông tin đơn hàng từ VNPAY.", Toast.LENGTH_LONG).show();
                // Xử lý trường hợp không có Order ID, chuyển hướng
                navigateAfterPayment(false);
            }
        } else {
            Log.w("VNPAYReturn", "No data received in VNPAY Return Intent.");
            Toast.makeText(this, "Không nhận được phản hồi từ VNPAY.", Toast.LENGTH_LONG).show();
            // Xử lý trường hợp không có dữ liệu, chuyển hướng
            navigateAfterPayment(false);
        }
    }


    private void checkOrderStatus(Long orderId, String token) {
        // TODO: Hiển thị loading indicator


        String headerAuth = "Bearer " + token;
        apiService.getOrderDetails(headerAuth, orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {

                if (response.isSuccessful() && response.body() != null) {
                    Order order = response.body();
                    Log.i("VNPAYReturn", "Checked status for order " + orderId + ": " + (order != null && order.getStatus() != null ? order.getStatus().name() : "N/A"));

                    if (order != null && order.getStatus() != null) {
                        if (order.getStatus() == OrderStatus.REVIEWED || // Trạng thái Backend set khi IPN thành công
                                order.getStatus() == OrderStatus.WAITING || // Hoặc Waiting nếu Backend set Waiting
                                order.getStatus() == OrderStatus.SHIPPING||
                                order.getStatus() == OrderStatus.DELIVERED) {

                            Toast.makeText(VNPAYPaymentActivity.this, "Thanh toán VNPAY thành công! Mã đơn: " + order.getOrderId(), Toast.LENGTH_LONG).show();
                            navigateAfterPayment(true);

                        } else if (order.getStatus() == OrderStatus.CANCELLED ) {
                            Toast.makeText(VNPAYPaymentActivity.this, "Thanh toán VNPAY thất bại hoặc đã bị hủy cho đơn " + order.getOrderId() + ".", Toast.LENGTH_LONG).show();
                            navigateAfterPayment(false);

                        } else if (order.getStatus() == OrderStatus.PENDING) {
                            Toast.makeText(VNPAYPaymentActivity.this, "Đơn hàng đang chờ xác nhận thanh toán. Vui lòng kiểm tra lại sau.", Toast.LENGTH_LONG).show();
                            navigateAfterPayment(false); // Coi như chưa thành công để chuyển hướng
                        } else {
                            Log.w("VNPAYReturn", "Order " + orderId + " in unexpected state: " + order.getStatus().name());
                            Toast.makeText(VNPAYPaymentActivity.this, "Trạng thái đơn hàng không xác định: " + order.getStatus().name() + ".", Toast.LENGTH_LONG).show();
                            navigateAfterPayment(false);
                        }
                    } else {
                        Log.e("VNPAYReturn", "Order object or status is null in status check response for Order ID: " + orderId);
                        Toast.makeText(VNPAYPaymentActivity.this, "Lỗi kiểm tra trạng thái đơn hàng.", Toast.LENGTH_LONG).show();
                        navigateAfterPayment(false);
                    }

                } else {
                    Log.e("VNPAYReturn", "Error fetching order status for Order ID " + orderId + ": " + response.code());
                    Toast.makeText(VNPAYPaymentActivity.this, "Lỗi khi lấy thông tin đơn hàng.", Toast.LENGTH_LONG).show();
                    navigateAfterPayment(false);
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Log.e("VNPAYReturn", "Network error fetching order status for Order ID " + orderId, t);
                Toast.makeText(VNPAYPaymentActivity.this, "Lỗi mạng khi kiểm tra trạng thái đơn hàng.", Toast.LENGTH_LONG).show();
                navigateAfterPayment(false);
            }
        });
    }

    private void navigateAfterPayment(boolean success) {
        Intent intent; // Khai báo Intent ở đây

        if (success) {
            // Thanh toán thành công -> Chuyển đến OrderDetailActivity
            Log.i(TAG, "Payment confirmed successfully. Navigating to OrderDetailActivity.");
            if (orderIdFromVnpay != null && orderIdFromVnpay > 0) {
                intent = new Intent(this, OrderDetailActivity.class);
                // *** QUAN TRỌNG: Sử dụng key chính xác mà OrderDetailActivity mong đợi ***
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, orderIdFromVnpay);
            } else {
                // Trường hợp hi hữu: thành công nhưng không có orderId? Chuyển về Home để tránh lỗi.
                Log.e(TAG, "Payment success reported but orderId is missing or invalid. Navigating home.");
                Toast.makeText(this, "Thanh toán thành công nhưng có lỗi lấy mã đơn. Vui lòng kiểm tra lịch sử.", Toast.LENGTH_LONG).show();
                intent = new Intent(this, HomeActivity.class);
            }
        } else {
            // Thanh toán thất bại hoặc chưa xác nhận -> Chuyển về HomeActivity
            Log.w(TAG, "Payment failed or not confirmed by backend. Navigating to HomeActivity.");
            // Toast thông báo lỗi/trạng thái nên đã được hiển thị trước khi gọi hàm này
            intent = new Intent(this, HomeActivity.class);
        }

        // Đặt flags và khởi chạy Activity đã chọn
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        // Đóng Activity hiện tại (VNPAYPaymentActivity)
        finish();
    }
}
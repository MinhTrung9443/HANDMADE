package vn.iostar.doan.activity;

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

    // Bạn có thể cần khai báo các View nếu có layout riêng cho Activity này
    // private ProgressBar progressBar;
    // private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: Load layout cho Activity này (nếu có)
        // Ví dụ: setContentView(R.layout.activity_vnpay_payment);
        // TODO: Khởi tạo binding nếu dùng
        // binding = ActivityVnpayPaymentBinding.inflate(getLayoutInflater());
        // setContentView(binding.getRoot());
        // TODO: Tìm và gán các View (ví dụ: ProgressBar, TextView) nếu có layout
        // progressBar = findViewById(R.id.progressBar);
        // tvStatus = findViewById(R.id.tvStatus);


        apiService = ApiService.apiService; // Lấy instance của ApiService

        // TODO: Lấy authToken từ nơi bạn lưu (ví dụ: SharedPreferences sau khi đăng nhập)
        // Đây là bước quan trọng để có thể gọi API backend.
        // authToken = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("AUTH_TOKEN", null);
        // Nếu authToken null, bạn không thể gọi API và cần xử lý lỗi (ví dụ: chuyển hướng về màn hình đăng nhập)


        // Xử lý Intent đầu tiên khi Activity được tạo
        handleVnpayReturnIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Xử lý Intent khi Activity đã tồn tại (launchMode singleTask/singleTop)
        setIntent(intent); // Cập nhật Intent cho Activity
        handleVnpayReturnIntent(intent);
    }

    private void handleVnpayReturnIntent(Intent intent) {
        // Đảm bảo đã lấy được authToken trước khi xử lý Intent
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
        // if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        // if (tvStatus != null) tvStatus.setText("Đang kiểm tra trạng thái đơn hàng...");


        String headerAuth = "Bearer " + token;
        apiService.getOrderDetails(headerAuth, orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                // TODO: Ẩn loading indicator
                // if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    Order order = response.body();
                    Log.i("VNPAYReturn", "Checked status for order " + orderId + ": " + (order != null && order.getStatus() != null ? order.getStatus().name() : "N/A"));

                    if (order != null && order.getStatus() != null) {
                        // *** Xử lý dựa trên trạng thái từ Backend (đã được cập nhật bởi IPN) ***
                        // So sánh trạng thái với các giá trị trong Enum OrderStatus của Mobile
                        if (order.getStatus() == OrderStatus.REVIEWED || // Trạng thái Backend set khi IPN thành công
                                order.getStatus() == OrderStatus.WAITING || // Hoặc Waiting nếu Backend set Waiting
                                order.getStatus() == OrderStatus.SHIPPING||
                                order.getStatus() == OrderStatus.DELIVERED) {

                            Toast.makeText(VNPAYPaymentActivity.this, "Thanh toán VNPAY thành công! Mã đơn: " + order.getOrderId(), Toast.LENGTH_LONG).show();
                            // Chuyển hướng đến màn hình thành công hoặc trang chủ
                            navigateAfterPayment(true);

                        } else if (order.getStatus() == OrderStatus.CANCELLED ) {
                            Toast.makeText(VNPAYPaymentActivity.this, "Thanh toán VNPAY thất bại hoặc đã bị hủy cho đơn " + order.getOrderId() + ".", Toast.LENGTH_LONG).show();
                            // Chuyển hướng đến màn hình thông báo thất bại hoặc lịch sử đơn hàng
                            navigateAfterPayment(false);

                        } else if (order.getStatus() == OrderStatus.PENDING) {
                            // IPN có thể bị trễ. Đơn hàng vẫn PendingPayment.
                            Toast.makeText(VNPAYPaymentActivity.this, "Đơn hàng đang chờ xác nhận thanh toán. Vui lòng kiểm tra lại sau.", Toast.LENGTH_LONG).show();
                            // Chuyển hướng đến lịch sử đơn hàng hoặc trang chủ
                            navigateAfterPayment(false); // Coi như chưa thành công để chuyển hướng
                        } else {
                            // Trạng thái khác không mong muốn
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
                    // API getOrderDetails bị lỗi (4xx, 5xx)
                    Log.e("VNPAYReturn", "Error fetching order status for Order ID " + orderId + ": " + response.code());
                    // Xử lý lỗi API, hiển thị thông báo và chuyển hướng
                    Toast.makeText(VNPAYPaymentActivity.this, "Lỗi khi lấy thông tin đơn hàng.", Toast.LENGTH_LONG).show();
                    navigateAfterPayment(false);
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                // TODO: Ẩn loading
                // Lỗi mạng khi gọi API getOrderDetails
                Log.e("VNPAYReturn", "Network error fetching order status for Order ID " + orderId, t);
                Toast.makeText(VNPAYPaymentActivity.this, "Lỗi mạng khi kiểm tra trạng thái đơn hàng.", Toast.LENGTH_LONG).show();
                navigateAfterPayment(false);
            }
        });
    }

    // Hàm giúp chuyển hướng sau khi xử lý thanh toán
    private void navigateAfterPayment(boolean success) {
        // TODO: Implement logic chuyển hướng
        // Tùy thuộc vào kết quả thành công hay thất bại, chuyển đến màn hình phù hợp
        // Ví dụ: Luôn chuyển về HomeActivity và có thể truyền extra để hiển thị thông báo ở đó
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Xóa các Activity trên stack, tạo task mới cho Home

        // Có thể truyền kết quả (thành công/thất bại) qua Intent extra
        // intent.putExtra("paymentSuccess", success);
        // intent.putExtra("orderId", orderIdFromVnpay); // Truyền cả Order ID

        startActivity(intent);
        finish(); // Đóng VNPAYPaymentActivity sau khi đã chuyển hướng
    }
}